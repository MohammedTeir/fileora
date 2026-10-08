package dev.deviceagent.core.tools.file

import dev.deviceagent.core.fs.FileInfo
import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.fs.FsException
import dev.deviceagent.core.tools.ErrorKind
import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ParamType.BOOLEAN
import dev.deviceagent.core.tools.ParamType.INTEGER
import dev.deviceagent.core.tools.ParamType.STRING
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.RiskLevel
import dev.deviceagent.core.tools.TRASH_DIR
import dev.deviceagent.core.tools.Tool
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolError
import dev.deviceagent.core.tools.ToolResult
import dev.deviceagent.core.tools.ToolSpec
import dev.deviceagent.core.tools.UndoAction
import dev.deviceagent.core.util.jsonObjectOf
import dev.deviceagent.core.util.long
import dev.deviceagent.core.util.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

private const val MAX_WALK_ENTRIES = 50_000

/** Shared helpers. All blocking; callers are already on Dispatchers.IO. */
internal object FileOps {
    /** (file count, total bytes) under [path]; symlinks and the trash folder are skipped. */
    fun measure(fs: FileSystem, path: String): Pair<Int, Long> {
        var files = 0; var bytes = 0L; var visited = 0
        fun walk(p: String) {
            val info = fs.stat(p) ?: return
            if (info.isSymlink) return
            if (!info.isDirectory) { files++; bytes += info.size; return }
            for (e in fs.list(p)) {
                if (++visited > MAX_WALK_ENTRIES) throw FsException(ErrorKind.IO, "Too many entries to process in one operation", p)
                if (e.name == TRASH_DIR) continue
                walk(e.path)
            }
        }
        walk(path)
        return files to bytes
    }

    /** Missing directories on the way to [dir], topmost first. */
    fun missingAncestors(fs: FileSystem, dir: String): List<String> {
        val missing = ArrayList<String>()
        var cur: String? = dir
        while (cur != null && fs.stat(cur) == null) { missing.add(cur); cur = fs.parent(cur) }
        missing.reverse()
        return missing
    }

    fun copyTree(fs: FileSystem, src: String, dst: String, created: MutableList<UndoAction>) {
        val info = fs.stat(src) ?: throw FsException(ErrorKind.NOT_FOUND, "No such file or directory", src)
        if (info.isDirectory) {
            created += UndoAction.DeleteCreated(dst)
            fs.mkdirs(dst)
            for (e in fs.list(src)) {
                if (e.isSymlink || e.name == TRASH_DIR) continue
                copyTree(fs, e.path, fs.child(dst, e.name), created)
            }
        } else {
            created += UndoAction.DeleteCreated(dst)
            fs.copyFile(src, dst)
        }
    }

    fun failedPlan(summary: String, kind: ErrorKind, message: String, path: String?) =
        Plan(summary = summary, error = ToolError(kind, message, path))

    fun fileJson(i: FileInfo) = jsonObjectOf(
        "name" to i.name,
        "type" to if (i.isDirectory) "directory" else "file",
        "size" to i.size,
        "modified_ms" to i.modifiedMs,
    )
}

/** Runs [block] on the IO dispatcher and converts filesystem exceptions into tool failures. */
internal suspend fun guarded(block: () -> ToolResult): ToolResult = withContext(Dispatchers.IO) {
    try { block() } catch (e: FsException) { ToolResult.failure(e.kind, e.message ?: "I/O error", e.path) }
}

internal suspend fun guardedPlan(summary: String, block: () -> Plan): Plan = withContext(Dispatchers.IO) {
    try { block() } catch (e: FsException) { FileOps.failedPlan(summary, e.kind, e.message ?: "I/O error", e.path) }
}

// ───────────────────────────── read-only tools ─────────────────────────────

class FileListTool : Tool {
    override val spec = ToolSpec(
        "file.list", "List the entries of one directory (not recursive): name, type, size.",
        listOf(
            ParamSpec("path", STRING, "Absolute path of the directory"),
            ParamSpec("limit", INTEGER, "Maximum entries to return (default 200)", required = false),
        ),
        RiskLevel.READ,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) =
        Plan("List ${args.str("path")}", reads = listOfNotNull(args.str("path")))

    override suspend fun execute(args: JsonObject, ctx: ToolContext) = guarded {
        val path = args.str("path")!!
        val limit = (args.long("limit") ?: 200).toInt().coerceIn(1, 1000)
        val all = ctx.fs.list(path).filter { it.name != TRASH_DIR }
        ToolResult.success(
            jsonObjectOf(
                "path" to path, "total" to all.size, "truncated" to (all.size > limit),
                "entries" to all.take(limit).map { FileOps.fileJson(it) },
            ),
        )
    }
}

class FileSearchTool : Tool {
    override val spec = ToolSpec(
        "file.search",
        "Recursively search below a folder for names matching a pattern. The pattern is a case-insensitive " +
            "substring, or a glob if it contains * or ? (e.g. *.mp4).",
        listOf(
            ParamSpec("root", STRING, "Absolute path of the folder to search in"),
            ParamSpec("pattern", STRING, "Substring or glob matched against the file name"),
            ParamSpec("kind", STRING, "Optional: 'file' or 'directory'", required = false),
            ParamSpec("max_results", INTEGER, "Maximum results (default 100)", required = false),
        ),
        RiskLevel.READ,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) =
        Plan("Search ${args.str("root")} for ${args.str("pattern")}", reads = listOfNotNull(args.str("root")))

    override suspend fun execute(args: JsonObject, ctx: ToolContext) = guarded {
        val root = args.str("root")!!
        val pattern = args.str("pattern")!!
        val kind = args.str("kind")
        if (kind != null && kind != "file" && kind != "directory") {
            return@guarded ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "kind must be 'file' or 'directory'")
        }
        val max = (args.long("max_results") ?: 100).toInt().coerceIn(1, 1000)
        val matcher = compile(pattern)
        val hits = ArrayList<JsonObject>()
        var visited = 0
        var truncated = false
        fun walk(dir: String, depth: Int) {
            if (truncated || depth > 15) return
            for (e in ctx.fs.list(dir)) {
                if (e.name == TRASH_DIR || e.isSymlink) continue
                if (++visited > MAX_WALK_ENTRIES) { truncated = true; return }
                val typeOk = kind == null || (kind == "directory") == e.isDirectory
                if (typeOk && matcher(e.name)) {
                    if (hits.size >= max) { truncated = true; return }
                    hits += jsonObjectOf(
                        "path" to e.path, "type" to if (e.isDirectory) "directory" else "file", "size" to e.size,
                    )
                }
                if (e.isDirectory) walk(e.path, depth + 1)
                if (truncated) return
            }
        }
        if (ctx.fs.stat(root) == null) return@guarded ToolResult.failure(ErrorKind.NOT_FOUND, "No such directory", root)
        walk(root, 0)
        ToolResult.success(jsonObjectOf("root" to root, "matches" to hits, "truncated" to truncated))
    }

    private fun compile(pattern: String): (String) -> Boolean {
        if (pattern.contains('*') || pattern.contains('?')) {
            val rx = Regex(pattern.map {
                when (it) { '*' -> ".*"; '?' -> "."; else -> Regex.escape(it.toString()) }
            }.joinToString(""), RegexOption.IGNORE_CASE)
            return { name -> rx.matches(name) }
        }
        return { name -> name.contains(pattern, ignoreCase = true) }
    }
}

class FileStatTool : Tool {
    override val spec = ToolSpec(
        "file.stat", "Get type, size and modification time of one path, or report that it does not exist.",
        listOf(ParamSpec("path", STRING, "Absolute path")), RiskLevel.READ,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) =
        Plan("Stat ${args.str("path")}", reads = listOfNotNull(args.str("path")))

    override suspend fun execute(args: JsonObject, ctx: ToolContext) = guarded {
        val path = args.str("path")!!
        val i = ctx.fs.stat(path)
        if (i == null) ToolResult.success(jsonObjectOf("path" to path, "exists" to false))
        else {
            val counts = if (i.isDirectory) FileOps.measure(ctx.fs, path) else null
            ToolResult.success(
                jsonObjectOf(
                    "path" to path, "exists" to true, "type" to if (i.isDirectory) "directory" else "file",
                    "size" to (counts?.second ?: i.size), "files" to counts?.first, "modified_ms" to i.modifiedMs,
                ),
            )
        }
    }
}

class FileHashTool : Tool {
    override val spec = ToolSpec(
        "file.hash", "SHA-256 of a file's contents; use it to check whether two files are identical.",
        listOf(ParamSpec("path", STRING, "Absolute path of a file")), RiskLevel.READ,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) =
        Plan("Hash ${args.str("path")}", reads = listOfNotNull(args.str("path")))

    override suspend fun execute(args: JsonObject, ctx: ToolContext) = guarded {
        val path = args.str("path")!!
        val i = ctx.fs.stat(path) ?: return@guarded ToolResult.failure(ErrorKind.NOT_FOUND, "No such file", path)
        if (i.isDirectory) return@guarded ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Path is a directory", path)
        ToolResult.success(jsonObjectOf("path" to path, "sha256" to ctx.fs.sha256(path), "size" to i.size))
    }
}

// ───────────────────────────── mutating tools ─────────────────────────────

class FileMkdirTool : Tool {
    override val spec = ToolSpec(
        "file.mkdir", "Create a directory (and missing parents). Succeeds if it already exists.",
        listOf(ParamSpec("path", STRING, "Absolute path of the directory")), RiskLevel.LOW,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext): Plan {
        val path = args.str("path")!!
        return guardedPlan("Create folder $path") {
            val existing = ctx.fs.stat(path)
            if (existing != null && !existing.isDirectory) {
                FileOps.failedPlan("Create folder $path", ErrorKind.ALREADY_EXISTS, "A file with this name already exists", path)
            } else Plan("Create folder $path", writes = listOf(path))
        }
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext) = guarded {
        val path = args.str("path")!!
        val created = FileOps.missingAncestors(ctx.fs, path).map { UndoAction.DeleteCreated(it) }
        ctx.fs.mkdirs(path)
        ToolResult.success(jsonObjectOf("path" to path, "created" to created.size), created)
    }
}

class FileCopyTool : Tool {
    override val spec = ToolSpec(
        "file.copy",
        "Copy a file or a whole folder to a new path. Missing parent folders are created. Never overwrites: " +
            "fails with ALREADY_EXISTS if the destination exists.",
        listOf(
            ParamSpec("source", STRING, "Absolute path of the file or folder to copy"),
            ParamSpec("destination", STRING, "Absolute path of the new copy (not the parent folder)"),
        ),
        RiskLevel.LOW,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext): Plan {
        val src = args.str("source")!!; val dst = args.str("destination")!!
        val summary = "Copy $src → $dst"
        return guardedPlan(summary) {
            val fs = ctx.fs
            val si = fs.stat(src) ?: return@guardedPlan FileOps.failedPlan(summary, ErrorKind.NOT_FOUND, "Source does not exist", src)
            if (fs.stat(dst) != null) return@guardedPlan FileOps.failedPlan(summary, ErrorKind.ALREADY_EXISTS, "Destination already exists", dst)
            if (si.isDirectory && fs.isWithin(dst, src)) {
                return@guardedPlan FileOps.failedPlan(summary, ErrorKind.INVALID_ARGUMENTS, "Cannot copy a folder into itself", dst)
            }
            val (files, bytes) = FileOps.measure(fs, src)
            val free = fs.freeSpaceBytes(dst)
            if (free != null && bytes > free) {
                return@guardedPlan FileOps.failedPlan(summary, ErrorKind.NO_SPACE, "Needs $bytes bytes but only $free are free", dst)
            }
            Plan("$summary ($files file(s), $bytes bytes)", reads = listOf(src), writes = listOf(dst), fileCount = files, bytes = bytes)
        }
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult = withContext(Dispatchers.IO) {
        val src = args.str("source")!!; val dst = args.str("destination")!!
        val created = ArrayList<UndoAction>()
        try {
            ctx.fs.parent(dst)?.let { p ->
                FileOps.missingAncestors(ctx.fs, p).forEach { created += UndoAction.DeleteCreated(it) }
                ctx.fs.mkdirs(p)
            }
            FileOps.copyTree(ctx.fs, src, dst, created)
            val (files, bytes) = FileOps.measure(ctx.fs, dst)
            ToolResult.success(jsonObjectOf("source" to src, "destination" to dst, "files" to files, "bytes" to bytes), created)
        } catch (e: FsException) {
            ToolResult.failure(e.kind, e.message ?: "I/O error", e.path, undo = created)
        }
    }
}

/** Shared by file.move and file.rename. */
private suspend fun planRelocation(verb: String, src: String, dst: String, ctx: ToolContext): Plan {
    val summary = "$verb $src → $dst"
    return guardedPlan(summary) {
        val fs = ctx.fs
        val si = fs.stat(src) ?: return@guardedPlan FileOps.failedPlan(summary, ErrorKind.NOT_FOUND, "Source does not exist", src)
        if (fs.stat(dst) != null) return@guardedPlan FileOps.failedPlan(summary, ErrorKind.ALREADY_EXISTS, "Destination already exists", dst)
        if (fs.rootOf(src) == src) return@guardedPlan FileOps.failedPlan(summary, ErrorKind.INVALID_ARGUMENTS, "Cannot move an accessible root folder", src)
        if (si.isDirectory && fs.isWithin(dst, src)) {
            return@guardedPlan FileOps.failedPlan(summary, ErrorKind.INVALID_ARGUMENTS, "Cannot move a folder into itself", dst)
        }
        val (files, bytes) = FileOps.measure(fs, src)
        Plan(summary, reads = listOf(src), writes = listOf(dst), fileCount = files, bytes = bytes)
    }
}

private suspend fun executeRelocation(src: String, dst: String, ctx: ToolContext): ToolResult = withContext(Dispatchers.IO) {
    val undo = ArrayList<UndoAction>()
    try {
        ctx.fs.parent(dst)?.let { p ->
            FileOps.missingAncestors(ctx.fs, p).forEach { undo += UndoAction.DeleteCreated(it) }
            ctx.fs.mkdirs(p)
        }
        ctx.fs.move(src, dst)
        undo += UndoAction.MoveBack(from = dst, to = src)
        ToolResult.success(jsonObjectOf("from" to src, "to" to dst), undo)
    } catch (e: FsException) {
        ToolResult.failure(e.kind, e.message ?: "I/O error", e.path, undo = undo.filterIsInstance<UndoAction.DeleteCreated>())
    }
}

class FileMoveTool : Tool {
    override val spec = ToolSpec(
        "file.move",
        "Move a file or folder to a new path. Missing parent folders are created. Never overwrites.",
        listOf(
            ParamSpec("source", STRING, "Absolute path of the file or folder to move"),
            ParamSpec("destination", STRING, "Absolute new path (not the parent folder)"),
        ),
        RiskLevel.MODERATE,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) =
        planRelocation("Move", args.str("source")!!, args.str("destination")!!, ctx)

    override suspend fun execute(args: JsonObject, ctx: ToolContext) =
        executeRelocation(args.str("source")!!, args.str("destination")!!, ctx)
}

class FileRenameTool : Tool {
    override val spec = ToolSpec(
        "file.rename", "Rename a file or folder in place. new_name is a bare name, not a path. Never overwrites.",
        listOf(
            ParamSpec("path", STRING, "Absolute path of the file or folder"),
            ParamSpec("new_name", STRING, "New name without any '/' characters"),
        ),
        RiskLevel.MODERATE,
    )

    private fun target(args: JsonObject, ctx: ToolContext): Pair<String, String?> {
        val path = args.str("path")!!
        val name = args.str("new_name")!!
        val bad = name.isBlank() || name == "." || name == ".." || name.contains('/') || name.contains('\\')
        return path to if (bad) null else ctx.fs.parent(path)?.let { ctx.fs.child(it, name) }
    }

    override suspend fun plan(args: JsonObject, ctx: ToolContext): Plan {
        val (path, dst) = target(args, ctx)
        if (dst == null) {
            return FileOps.failedPlan("Rename $path", ErrorKind.INVALID_ARGUMENTS, "new_name must be a plain file name", path)
        }
        return planRelocation("Rename", path, dst, ctx)
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val (path, dst) = target(args, ctx)
        dst ?: return ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "new_name must be a plain file name", path)
        return executeRelocation(path, dst, ctx)
    }
}

class FileDeleteTool : Tool {
    override val spec = ToolSpec(
        "file.delete",
        "Delete a file or folder by moving it to the agent's restorable trash. Always needs user confirmation. " +
            "Only use when the user explicitly asked for deletion.",
        listOf(ParamSpec("path", STRING, "Absolute path of the file or folder")),
        RiskLevel.DESTRUCTIVE,
    )

    private fun inTrash(fs: FileSystem, path: String) = fs.normalize(path).split('/', '\\').contains(TRASH_DIR)

    private fun trashTarget(ctx: ToolContext, path: String): String? {
        val fs = ctx.fs
        val root = fs.rootOf(path) ?: return null
        val dir = fs.child(fs.child(root, TRASH_DIR), ctx.taskId)
        return fs.child(dir, "${ctx.nextId()}_${fs.name(path)}")
    }

    override suspend fun plan(args: JsonObject, ctx: ToolContext): Plan {
        val path = args.str("path")!!
        val summary = "Delete $path (move to restorable trash)"
        return guardedPlan(summary) {
            val fs = ctx.fs
            val i = fs.stat(path) ?: return@guardedPlan FileOps.failedPlan(summary, ErrorKind.NOT_FOUND, "Path does not exist", path)
            if (fs.rootOf(path) == fs.normalize(path)) {
                return@guardedPlan FileOps.failedPlan(summary, ErrorKind.INVALID_ARGUMENTS, "Cannot delete an accessible root folder", path)
            }
            if (inTrash(fs, path)) {
                return@guardedPlan FileOps.failedPlan(summary, ErrorKind.INVALID_ARGUMENTS, "Path is already inside the trash", path)
            }
            val (files, bytes) = if (i.isDirectory) FileOps.measure(fs, path) else 1 to i.size
            Plan(summary, deletes = listOf(path), fileCount = files, bytes = bytes, reversible = true)
        }
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult = withContext(Dispatchers.IO) {
        val path = args.str("path")!!
        try {
            val trash = trashTarget(ctx, path)
                ?: return@withContext ToolResult.failure(ErrorKind.OUTSIDE_SANDBOX, "No trash available for this location", path)
            val (files, bytes) = ctx.fs.stat(path)?.let { if (it.isDirectory) FileOps.measure(ctx.fs, path) else 1 to it.size } ?: (0 to 0L)
            ctx.fs.parent(trash)?.let { ctx.fs.mkdirs(it) }
            ctx.fs.move(path, trash)
            ToolResult.success(
                jsonObjectOf("path" to path, "trashed_to" to trash, "files" to files, "bytes" to bytes),
                listOf(UndoAction.MoveBack(from = trash, to = path)),
            )
        } catch (e: FsException) {
            ToolResult.failure(e.kind, e.message ?: "I/O error", e.path)
        }
    }
}
