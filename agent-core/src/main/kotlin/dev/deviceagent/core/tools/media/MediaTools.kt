package dev.deviceagent.core.tools.media

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
import dev.deviceagent.core.util.jsonObjectOf
import dev.deviceagent.core.util.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class MediaProbeTool : Tool {
    override val spec = ToolSpec(
        "media.probe",
        "Inspect media format, duration and size using the host media backend without sending media to the model.",
        listOf(ParamSpec("path", STRING, "Absolute path of a media file")),
        RiskLevel.READ,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) = Plan("Probe ${args.str("path")}", reads = listOfNotNull(args.str("path")))

    override suspend fun execute(args: JsonObject, ctx: ToolContext) = mediaGuarded {
        val path = args.str("path")!!
        val info = ctx.fs.stat(path) ?: return@mediaGuarded ToolResult.failure(ErrorKind.NOT_FOUND, "No such file", path)
        if (info.isDirectory) return@mediaGuarded ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Media path is a directory", path)
        val backend = ctx.mediaBackend ?: return@mediaGuarded ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Media backend is unavailable on this host", path)
        try {
            val probe = backend.probe(path)
            ToolResult.success(jsonObjectOf("path" to path, "format" to probe.format, "duration_seconds" to probe.durationSeconds, "size_bytes" to (probe.sizeBytes ?: info.size), "playable" to probe.playable))
        } catch (e: Exception) {
            ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Media probe failed: ${e.message ?: "unknown error"}", path)
        }
    }
}

class MediaConvertTool : Tool {
    override val spec = ToolSpec(
        "media.convert",
        "Convert or remux one media file with the host media backend. Android SAF currently remuxes supported tracks to MP4; the JVM backend uses FFmpeg. The new destination is verified playable and non-empty.",
        listOf(
            ParamSpec("source", STRING, "Absolute source media path"),
            ParamSpec("destination", STRING, "Absolute new output path"),
            ParamSpec("format", STRING, "Optional output format supported by the current host backend", required = false),
        ),
        RiskLevel.MODERATE,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) = mediaGuardedPlan("Convert ${args.str("source")} → ${args.str("destination")}") {
        val source = args.str("source")!!
        val destination = args.str("destination")!!
        val sourceInfo = ctx.fs.stat(source) ?: return@mediaGuardedPlan Plan("Convert $source → $destination", error = dev.deviceagent.core.tools.ToolError(ErrorKind.NOT_FOUND, "No such source", source))
        if (sourceInfo.isDirectory) return@mediaGuardedPlan Plan("Convert $source → $destination", error = dev.deviceagent.core.tools.ToolError(ErrorKind.INVALID_ARGUMENTS, "Source is a directory", source))
        if (ctx.fs.stat(destination) != null) return@mediaGuardedPlan Plan("Convert $source → $destination", error = dev.deviceagent.core.tools.ToolError(ErrorKind.ALREADY_EXISTS, "Destination already exists", destination))
        Plan("Convert $source → $destination", reads = listOf(source), writes = listOf(destination), bytes = sourceInfo.size)
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext) = mediaGuarded {
        val source = args.str("source")!!
        val destination = args.str("destination")!!
        val sourceInfo = ctx.fs.stat(source) ?: return@mediaGuarded ToolResult.failure(ErrorKind.NOT_FOUND, "No such source", source)
        if (sourceInfo.isDirectory) return@mediaGuarded ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Source is a directory", source)
        if (ctx.fs.stat(destination) != null) return@mediaGuarded ToolResult.failure(ErrorKind.ALREADY_EXISTS, "Destination already exists", destination)
        val backend = ctx.mediaBackend ?: return@mediaGuarded ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Media backend is unavailable on this host", destination)
        try {
            ctx.fs.parent(destination)?.let { ctx.fs.mkdirs(it) }
            val result = backend.convert(source, destination, args.str("format"))
            ToolResult.success(jsonObjectOf("source" to source, "destination" to destination, "format" to result.format, "playable" to result.playable, "bytes" to result.bytesWritten), listOf(UndoAction.DeleteCreated(destination)))
        } catch (e: Exception) {
            ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Media conversion failed: ${e.message ?: "unknown error"}", destination)
        }
    }
}

class MediaExtractAudioTool : Tool {
    override val spec = ToolSpec(
        "media.extract_audio",
        "Extract the first audio track into a new audio file. The host backend determines supported output formats; the source is never sent to the model.",
        listOf(
            ParamSpec("source", STRING, "Absolute source audio/video path"),
            ParamSpec("destination", STRING, "Absolute new output path"),
            ParamSpec("format", STRING, "Optional output format (mp3, m4a, aac, wav, flac, ogg, or opus for the JVM FFmpeg backend)", required = false),
        ),
        RiskLevel.MODERATE,
    )

    override suspend fun plan(args: JsonObject, ctx: ToolContext) = mediaGuardedPlan("Extract audio from ${args.str("source")} → ${args.str("destination")}") {
        val source = args.str("source")!!
        val destination = args.str("destination")!!
        val sourceInfo = ctx.fs.stat(source) ?: return@mediaGuardedPlan Plan("Extract audio from $source → $destination", error = dev.deviceagent.core.tools.ToolError(ErrorKind.NOT_FOUND, "No such source", source))
        if (sourceInfo.isDirectory) return@mediaGuardedPlan Plan("Extract audio from $source → $destination", error = dev.deviceagent.core.tools.ToolError(ErrorKind.INVALID_ARGUMENTS, "Source is a directory", source))
        if (ctx.fs.stat(destination) != null) return@mediaGuardedPlan Plan("Extract audio from $source → $destination", error = dev.deviceagent.core.tools.ToolError(ErrorKind.ALREADY_EXISTS, "Destination already exists", destination))
        Plan("Extract audio from $source → $destination", reads = listOf(source), writes = listOf(destination), bytes = sourceInfo.size)
    }

    override suspend fun execute(args: JsonObject, ctx: ToolContext) = mediaGuarded {
        val source = args.str("source")!!
        val destination = args.str("destination")!!
        val sourceInfo = ctx.fs.stat(source) ?: return@mediaGuarded ToolResult.failure(ErrorKind.NOT_FOUND, "No such source", source)
        if (sourceInfo.isDirectory) return@mediaGuarded ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Source is a directory", source)
        if (ctx.fs.stat(destination) != null) return@mediaGuarded ToolResult.failure(ErrorKind.ALREADY_EXISTS, "Destination already exists", destination)
        val backend = ctx.mediaBackend ?: return@mediaGuarded ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Media backend is unavailable on this host", destination)
        try {
            ctx.fs.parent(destination)?.let { ctx.fs.mkdirs(it) }
            val result = backend.extractAudio(source, destination, args.str("format"))
            ToolResult.success(
                jsonObjectOf("source" to source, "destination" to destination, "format" to result.format, "playable" to result.playable, "bytes" to result.bytesWritten),
                listOf(UndoAction.DeleteCreated(destination)),
            )
        } catch (e: Exception) {
            ToolResult.failure(ErrorKind.INVALID_ARGUMENTS, "Audio extraction failed: ${e.message ?: "unknown error"}", destination)
        }
    }
}

private suspend fun mediaGuarded(block: () -> ToolResult): ToolResult = withContext(Dispatchers.IO) {
    try { block() } catch (e: dev.deviceagent.core.fs.FsException) {
        ToolResult.failure(e.kind, e.message ?: "I/O error", e.path)
    }
}

private suspend fun mediaGuardedPlan(summary: String, block: () -> Plan): Plan = withContext(Dispatchers.IO) {
    try { block() } catch (e: dev.deviceagent.core.fs.FsException) {
        Plan(summary, error = dev.deviceagent.core.tools.ToolError(e.kind, e.message ?: "I/O error", e.path))
    }
}
