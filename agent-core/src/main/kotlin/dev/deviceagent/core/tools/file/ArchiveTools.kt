package dev.deviceagent.core.tools.file

import dev.deviceagent.core.fs.FileSystem
import dev.deviceagent.core.tools.ErrorKind
import dev.deviceagent.core.tools.ParamSpec
import dev.deviceagent.core.tools.ParamType.STRING
import dev.deviceagent.core.tools.Plan
import dev.deviceagent.core.tools.RiskLevel
import dev.deviceagent.core.tools.Tool
import dev.deviceagent.core.tools.ToolContext
import dev.deviceagent.core.tools.ToolResult
import dev.deviceagent.core.tools.ToolSpec
import dev.deviceagent.core.tools.UndoAction
import dev.deviceagent.core.util.str
import dev.deviceagent.core.util.jsonObjectOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val ARCHIVE_MAX_BYTES = 25 * 1024 * 1024
const val ARCHIVE_MAX_ENTRIES = 2_000
const val ARCHIVE_MAX_UNCOMPRESSED_BYTES = 100 * 1024 * 1024

private data class ArchiveEntry(val name: String, val bytes: ByteArray? = null, val directory: Boolean = false)

class FileCompressTool : Tool {
    override val spec = ToolSpec(
        "file.compress",
        "Create a ZIP archive from a file or folder. Skips the agent trash and refuses oversized archives.",
        listOf(
            ParamSpec("source", STRING, "Absolute path of the file or folder to compress"),
            ParamSpec("destination", STRING, "Absolute path of the new .zip archive"),
        ),
        RiskLevel.MODERATE,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) = guardedPlan("Compress ${args.str("source")} → ${args.str("destination")}") {
        val source = args.str("source")!!
        val destination = args.str("destination")!!
        val info = ctx.fs.stat(source) ?: return@guardedPlan FileOps.failedPlan("Compress $source → $destination", ErrorKind.NOT_FOUND, "Source does not exist", source)
        if (info.isDirectory && ctx.fs.isWithin(destination, source)) {
            return@guardedPlan FileOps.failedPlan("Compress $source → $destination", ErrorKind.INVALID_ARGUMENTS, "Archive destination cannot be inside its source", destination)
        }
        if (ctx.fs.stat(destination) != null) return@guardedPlan FileOps.failedPlan("Compress $source → $destination", ErrorKind.ALREADY_EXISTS, "Archive destination already exists", destination)
        val (files, bytes) = if (info.isDirectory) FileOps.measure(ctx.fs, source) else 1 to info.size
        if (bytes > ARCHIVE_MAX_UNCOMPRESSED_BYTES) return@guardedPlan FileOps.failedPlan("Compress $source → $destination", ErrorKind.INVALID_ARGUMENTS, "Input exceeds the $ARCHIVE_MAX_UNCOMPRESSED_BYTES byte limit", source)
        Plan("Compress $files file(s), $bytes bytes", reads = listOf(source), writes = listOf(destination), fileCount = files + 1, bytes = bytes)
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult = withContext(Dispatchers.IO) {
        val source = args.str("source")!!
        val destination = args.str("destination")!!
        try {
            val info = ctx.fs.stat(source) ?: return@withContext ToolResult.failure(ErrorKind.NOT_FOUND, "Source does not exist", source)
            if (ctx.fs.stat(destination) != null) return@withContext ToolResult.failure(ErrorKind.ALREADY_EXISTS, "Archive destination already exists", destination)
            val entries = collectEntries(ctx.fs, source)
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zip ->
                entries.forEach { entry ->
                    zip.putNextEntry(ZipEntry(entry.name))
                    entry.bytes?.let(zip::write)
                    zip.closeEntry()
                }
            }
            val bytes = output.toByteArray()
            if (bytes.size > ARCHIVE_MAX_BYTES) return@withContext ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "ZIP archive exceeds the $ARCHIVE_MAX_BYTES byte limit", destination)
            ctx.fs.parent(destination)?.let { ctx.fs.mkdirs(it) }
            ctx.fs.writeBytes(destination, bytes)
            ToolResult.success(
                jsonObjectOf("source" to source, "destination" to destination, "entries" to entries.size, "bytes" to bytes.size, "sha256" to ctx.fs.sha256(destination)),
                listOf(UndoAction.DeleteCreated(destination)),
            )
        } catch (e: dev.deviceagent.core.fs.FsException) {
            ToolResult.failure(e.kind, e.message ?: "Archive error", e.path)
        } catch (e: IllegalArgumentException) {
            ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, e.message ?: "Invalid archive", source)
        }
    }

    private fun collectEntries(fs: FileSystem, source: String): List<ArchiveEntry> {
        val result = ArrayList<ArchiveEntry>()
        val rootName = fs.name(source).ifBlank { "archive" }
        fun walk(path: String, relative: String) {
            if (result.size >= ARCHIVE_MAX_ENTRIES) throw IllegalArgumentException("Archive exceeds the $ARCHIVE_MAX_ENTRIES entry limit")
            val info = fs.stat(path) ?: throw IllegalArgumentException("Source disappeared: $path")
            if (info.isSymlink || info.name == ".agent_trash") return
            if (info.isDirectory) {
                result += ArchiveEntry("$relative/", directory = true)
                fs.list(path).forEach { child -> walk(child.path, "$relative/${child.name}") }
            } else {
                val bytes = fs.readBytes(path, ARCHIVE_MAX_UNCOMPRESSED_BYTES)
                if (bytes.size.toLong() != info.size || bytes.size > ARCHIVE_MAX_UNCOMPRESSED_BYTES) throw IllegalArgumentException("Input exceeds archive size limits")
                result += ArchiveEntry(relative, bytes)
            }
        }
        walk(source, rootName)
        return result
    }
}

class FileExtractTool : Tool {
    override val spec = ToolSpec(
        "file.extract",
        "Extract a ZIP archive into a new folder with zip-slip and zip-bomb protection. Never overwrites existing files.",
        listOf(
            ParamSpec("archive", STRING, "Absolute path of the ZIP archive"),
            ParamSpec("destination", STRING, "Absolute path of the new destination folder"),
        ),
        RiskLevel.MODERATE,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) = guardedPlan("Extract ${args.str("archive")} → ${args.str("destination")}") {
        val archive = args.str("archive")!!
        val destination = args.str("destination")!!
        val info = ctx.fs.stat(archive) ?: return@guardedPlan FileOps.failedPlan("Extract $archive → $destination", ErrorKind.NOT_FOUND, "Archive does not exist", archive)
        if (info.isDirectory) return@guardedPlan FileOps.failedPlan("Extract $archive → $destination", ErrorKind.INVALID_ARGUMENTS, "Archive path is a directory", archive)
        if (ctx.fs.stat(destination) != null) return@guardedPlan FileOps.failedPlan("Extract $archive → $destination", ErrorKind.ALREADY_EXISTS, "Destination already exists", destination)
        if (info.size > ARCHIVE_MAX_BYTES) return@guardedPlan FileOps.failedPlan("Extract $archive → $destination", ErrorKind.INVALID_ARGUMENTS, "Archive exceeds the $ARCHIVE_MAX_BYTES byte limit", archive)
        Plan("Extract archive ($info)", reads = listOf(archive), writes = listOf(destination), fileCount = 1)
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult = withContext(Dispatchers.IO) {
        val archive = args.str("archive")!!
        val destination = args.str("destination")!!
        try {
            val info = ctx.fs.stat(archive) ?: return@withContext ToolResult.failure(ErrorKind.NOT_FOUND, "Archive does not exist", archive)
            if (info.isDirectory) return@withContext ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Archive path is a directory", archive)
            if (info.size > ARCHIVE_MAX_BYTES) return@withContext ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Archive exceeds the $ARCHIVE_MAX_BYTES byte limit", archive)
            if (ctx.fs.stat(destination) != null) return@withContext ToolResult.failure(ErrorKind.ALREADY_EXISTS, "Destination already exists", destination)
            val bytes = ctx.fs.readBytes(archive, ARCHIVE_MAX_BYTES + 1)
            val created = ArrayList<UndoAction>()
            var entries = 0
            var total = 0L
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++entries > ARCHIVE_MAX_ENTRIES) throw IllegalArgumentException("Archive exceeds the $ARCHIVE_MAX_ENTRIES entry limit")
                    val safe = safeEntry(entry.name)
                    val target = ctx.fs.child(destination, safe)
                    if (ctx.fs.stat(target) != null) throw IllegalArgumentException("Archive contains duplicate or existing path: ${entry.name}")
                    if (entry.isDirectory || entry.name.endsWith('/')) {
                        ctx.fs.mkdirs(target)
                        created += UndoAction.DeleteCreated(target)
                    } else {
                        val out = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > ARCHIVE_MAX_UNCOMPRESSED_BYTES) throw IllegalArgumentException("Archive expands beyond the $ARCHIVE_MAX_UNCOMPRESSED_BYTES byte limit")
                            out.write(buffer, 0, n)
                        }
                        ctx.fs.parent(target)?.let { ctx.fs.mkdirs(it) }
                        ctx.fs.writeBytes(target, out.toByteArray())
                        created += UndoAction.DeleteCreated(target)
                    }
                    zip.closeEntry()
                }
            }
            ToolResult.success(jsonObjectOf("archive" to archive, "destination" to destination, "entries" to entries, "bytes" to total), created + UndoAction.DeleteCreated(destination))
        } catch (e: dev.deviceagent.core.fs.FsException) {
            ToolResult.failure(e.kind, e.message ?: "Archive error", e.path)
        } catch (e: java.util.zip.ZipException) {
            ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Invalid ZIP archive: ${e.message}", archive)
        } catch (e: IllegalArgumentException) {
            ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, e.message ?: "Unsafe ZIP archive", archive)
        }
    }

    private fun safeEntry(raw: String): String {
        val normalized = raw.replace('\\', '/')
        if (normalized.startsWith('/') || normalized.split('/').any { it == ".." || it.isBlank() && normalized != raw }) {
            throw IllegalArgumentException("ZIP entry escapes destination: $raw")
        }
        val clean = normalized.trimEnd('/')
        if (clean.isBlank() || clean.split('/').any { it == "." || it == ".." }) throw IllegalArgumentException("Invalid ZIP entry: $raw")
        return clean
    }
}
