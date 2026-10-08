package dev.deviceagent.core.tools.media

import dev.deviceagent.core.fs.FileSystem
import java.io.File
import java.util.concurrent.TimeUnit

/** Media implementation supplied by the host. Android can provide an SAF/native backend. */
interface MediaBackend {
    fun probe(path: String): MediaProbe
    fun convert(source: String, destination: String, format: String?): MediaConversion
}

data class MediaProbe(
    val format: String,
    val durationSeconds: Double? = null,
    val sizeBytes: Long? = null,
    val playable: Boolean,
)

data class MediaConversion(
    val format: String,
    val playable: Boolean,
    val bytesWritten: Long,
)

/** Direct-distribution/JVM backend. It never invokes a shell; arguments are passed directly. */
class JvmFfmpegBackend(
    private val fs: FileSystem,
    private val timeoutSeconds: Long = 300,
) : MediaBackend {
    override fun probe(path: String): MediaProbe {
        val output = run(
            listOf("ffprobe", "-v", "error", "-show_entries", "format=format_name,duration,size", "-of", "default=noprint_wrappers=1:nokey=0", path),
        )
        val values = output.lines().mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
        return MediaProbe(
            format = values["format_name"] ?: "unknown",
            durationSeconds = values["duration"]?.toDoubleOrNull(),
            sizeBytes = values["size"]?.toLongOrNull(),
            playable = true,
        )
    }

    override fun convert(source: String, destination: String, format: String?): MediaConversion {
        run(listOf("ffmpeg", "-v", "error", "-nostdin", "-y", "-i", source) + listOfNotNull(format?.let { "-f" }, format) + destination)
        val info = fs.stat(destination) ?: error("FFmpeg output was not created")
        require(!info.isDirectory && info.size > 0) { "FFmpeg output is empty" }
        val probe = probe(destination)
        require(probe.playable) { "FFmpeg output failed media verification" }
        return MediaConversion(probe.format, true, info.size)
    }

    private fun run(command: List<String>): String {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Media command timed out after ${timeoutSeconds}s")
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.exitValue() != 0) error("Media command failed: ${output.take(500)}")
        return output
    }
}
