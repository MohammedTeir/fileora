package dev.deviceagent.core.tools.file

import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.fs.FsException
import dev.deviceagent.core.tools.ErrorKind
import dev.deviceagent.core.tools.APPROVED_ITEMS_ARG
import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ParamType.BOOLEAN
import dev.deviceagent.core.tools.ParamType.STRING
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.ReviewItem
import dev.deviceagent.core.tools.RiskLevel
import dev.deviceagent.core.tools.Tool
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolResult
import dev.deviceagent.core.tools.ToolSpec
import dev.deviceagent.core.tools.UndoAction
import dev.deviceagent.core.util.bool
import dev.deviceagent.core.util.jsonObjectOf
import dev.deviceagent.core.util.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val MAX_BATCH_MATCHES = 5_000

/** The ids (source paths) a person approved, or null when the whole batch stands. Set only by the engine. */
private fun JsonObject.approvedSources(): Set<String>? =
    (this[APPROVED_ITEMS_ARG] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }?.toSet()

/** A source/destination pair planned by file.move_matching. */
data class MoveMatch(val source: String, val destination: String, val bytes: Long)

private fun compileMovePattern(pattern: String): (String) -> Boolean {
    if (pattern.isBlank() || pattern == "*") return { true }
    if (pattern.contains('*') || pattern.contains('?')) {
        val regex = Regex(pattern.map { c ->
            when (c) {
                '*' -> ".*"
                '?' -> "."
                else -> Regex.escape(c.toString())
            }
        }.joinToString(""), RegexOption.IGNORE_CASE)
        return { name -> regex.matches(name) }
    }
    return { name -> name.contains(pattern, ignoreCase = true) }
}

internal fun collectMoveMatches(
    fs: FileSystem,
    sourceRoot: String,
    destinationRoot: String,
    pattern: String,
    recursive: Boolean,
): List<MoveMatch> {
    val matches = ArrayList<MoveMatch>()
    val matcher = compileMovePattern(pattern)
    var visited = 0

    fun walk(directory: String, relative: String) {
        for (entry in fs.list(directory)) {
            if (++visited > MAX_BATCH_MATCHES) {
                throw FsException(ErrorKind.BUDGET_EXHAUSTED, "Too many matching entries for one batch move", sourceRoot)
            }
            if (entry.name == dev.deviceagent.core.tools.TRASH_DIR || entry.isSymlink) continue
            val childRelative = if (relative.isEmpty()) entry.name else "$relative/${entry.name}"
            if (!entry.isDirectory && matcher(entry.name)) {
                matches += MoveMatch(entry.path, fs.child(destinationRoot, childRelative), entry.size)
            } else if (recursive && entry.isDirectory) {
                walk(entry.path, childRelative)
            }
        }
    }

    walk(sourceRoot, "")
    return matches
}

/** Moves matching files in one authorized, verified operation to avoid hundreds of model turns. */
class FileMoveMatchingTool : Tool {
    override val spec = ToolSpec(
        "file.move_matching",
        "Move every matching file from one folder to another in one operation. Use for requests such as moving all PDFs. " +
            "The pattern matches file names (for example *.pdf), never overwrites, preserves relative folders when recursive, and can be undone as one task.",
        listOf(
            ParamSpec("source_folder", STRING, "Absolute source folder"),
            ParamSpec("destination_folder", STRING, "Absolute destination folder"),
            ParamSpec("pattern", STRING, "File-name substring or glob such as *.pdf"),
            ParamSpec("recursive", BOOLEAN, "Whether to include subfolders", required = false),
        ),
        RiskLevel.MODERATE,
    )

    private fun inputs(args: JsonObject): Triple<String, String, String> = Triple(
        args.str("source_folder")!!,
        args.str("destination_folder")!!,
        args.str("pattern")!!,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext): Plan = guardedPlan("Move matching files") {
        val (source, destination, pattern) = inputs(args)
        val recursive = args.bool("recursive") ?: false
        val summary = "Move $pattern files from $source to $destination"
        val sourceInfo = ctx.fs.stat(source)
            ?: return@guardedPlan FileOps.failedPlan(summary, ErrorKind.NOT_FOUND, "Source folder does not exist", source)
        if (!sourceInfo.isDirectory) return@guardedPlan FileOps.failedPlan(summary, ErrorKind.INVALID_ARGUMENTS, "Source is not a folder", source)
        if (ctx.fs.isWithin(destination, source)) {
            return@guardedPlan FileOps.failedPlan(summary, ErrorKind.INVALID_ARGUMENTS, "Destination cannot be inside the source folder", destination)
        }
        val existingDestination = ctx.fs.stat(destination)
        if (existingDestination != null && !existingDestination.isDirectory) {
            return@guardedPlan FileOps.failedPlan(summary, ErrorKind.ALREADY_EXISTS, "Destination is not a folder", destination)
        }
        val approved = args.approvedSources()
        val matches = collectMoveMatches(ctx.fs, source, destination, pattern, recursive).filter { approved == null || it.source in approved }
        val collision = matches.firstOrNull { ctx.fs.stat(it.destination) != null }
        if (collision != null) return@guardedPlan FileOps.failedPlan(summary, ErrorKind.ALREADY_EXISTS, "Destination already exists: ${collision.destination}", collision.destination)
        Plan(
            summary = "$summary (${matches.size} file(s), ${matches.sumOf { it.bytes }} bytes)",
            reads = matches.map { it.source },
            writes = matches.map { it.destination },
            fileCount = matches.size,
            bytes = matches.sumOf { it.bytes },
            items = matches.map { ReviewItem(id = it.source, label = it.source, detail = it.destination, bytes = it.bytes) },
        )
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult = withContext(Dispatchers.IO) {
        val (source, destination, pattern) = inputs(args)
        val recursive = args.bool("recursive") ?: false
        val approved = args.approvedSources()
        val matches = try {
            collectMoveMatches(ctx.fs, source, destination, pattern, recursive).filter { approved == null || it.source in approved }
        } catch (e: FsException) {
            return@withContext ToolResult.failure(e.kind, e.message ?: "Could not find matching files", e.path)
        }
        val undo = ArrayList<UndoAction>()
        try {
            FileOps.missingAncestors(ctx.fs, destination).forEach { undo += UndoAction.DeleteCreated(it) }
            ctx.fs.mkdirs(destination)
            for (match in matches) {
                ctx.fs.parent(match.destination)?.let { parent ->
                    FileOps.missingAncestors(ctx.fs, parent).forEach { undo += UndoAction.DeleteCreated(it) }
                    ctx.fs.mkdirs(parent)
                }
                if (ctx.fs.stat(match.destination) != null) {
                    throw FsException(ErrorKind.ALREADY_EXISTS, "Destination already exists: ${match.destination}", match.destination)
                }
                ctx.fs.move(match.source, match.destination)
                undo += UndoAction.MoveBack(match.destination, match.source)
            }
            ToolResult.success(
                jsonObjectOf("source_folder" to source, "destination_folder" to destination, "pattern" to pattern, "moved" to matches.size, "bytes" to matches.sumOf { it.bytes }),
                undo,
            )
        } catch (e: FsException) {
            ToolResult.failure(e.kind, e.message ?: "Batch move failed", e.path, undo)
        }
    }
}
