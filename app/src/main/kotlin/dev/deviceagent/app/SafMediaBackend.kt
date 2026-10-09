package dev.deviceagent.app

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import dev.deviceagent.core.tools.media.MediaBackend
import dev.deviceagent.core.tools.media.MediaConversion
import dev.deviceagent.core.tools.media.MediaProbe
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * On-device, SAF-aware baseline media backend. It never uploads data and uses the platform
 * extractor/muxer to probe and remux already-encoded tracks. It does not re-encode codecs or
 * provide FFmpeg's arbitrary format/filter support; outputs are MP4 (or audio-only M4A).
 */
internal class SafMediaBackend(
    private val context: Context,
    private val fs: SafFileSystem,
) : MediaBackend {
    override fun probe(path: String): MediaProbe {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, fs.contentUri(path), null)
            val mimes = (0 until extractor.trackCount).mapNotNull { index ->
                extractor.getTrackFormat(index).stringOrNull(MediaFormat.KEY_MIME)
            }
            val durationUs = (0 until extractor.trackCount).mapNotNull { index ->
                extractor.getTrackFormat(index).longOrNull(MediaFormat.KEY_DURATION)
            }.maxOrNull()
            val extension = fs.name(path).substringAfterLast('.', "").lowercase()
            val detectedFormat = extension.ifBlank { mimes.distinct().joinToString(",").ifBlank { "unknown" } }
            return MediaProbe(
                format = detectedFormat,
                durationSeconds = durationUs?.takeIf { it > 0 }?.div(1_000_000.0),
                sizeBytes = fs.stat(path)?.size,
                playable = mimes.any { it.startsWith("audio/") || it.startsWith("video/") },
            )
        } finally {
            extractor.release()
        }
    }

    override fun convert(source: String, destination: String, format: String?): MediaConversion {
        val requested = (format ?: extension(destination)).lowercase()
        require(requested == "mp4") {
            "The Android SAF backend currently remuxes to MP4 only; arbitrary FFmpeg conversion is not available on this host"
        }
        return remux(source, destination, audioOnly = false, outputFormat = "mp4")
    }

    override fun extractAudio(source: String, destination: String, format: String?): MediaConversion {
        val requested = (format ?: extension(destination)).lowercase()
        require(requested == "m4a" || requested == "mp4") {
            "The Android SAF backend currently extracts audio to M4A/MP4 only"
        }
        return remux(source, destination, audioOnly = true, outputFormat = requested)
    }

    private fun remux(
        source: String,
        destination: String,
        audioOnly: Boolean,
        outputFormat: String,
    ): MediaConversion {
        val temp = File.createTempFile("device-agent-media-", ".mp4", context.cacheDir)
        check(temp.delete()) { "Could not prepare the temporary media output" }
        try {
            val sampleCount = remuxToFile(source, temp, audioOnly)
            require(sampleCount > 0 && temp.isFile && temp.length() > 0) { "Media output contains no samples" }
            val bytes = fs.copyFromLocalFile(temp, destination)
            try {
                require(probe(destination).playable) { "Media output failed track verification" }
            } catch (failure: Exception) {
                runCatching { fs.delete(destination) }
                throw failure
            }
            return MediaConversion(outputFormat, playable = true, bytesWritten = bytes)
        } finally {
            temp.delete()
        }
    }

    private fun remuxToFile(source: String, output: File, audioOnly: Boolean): Long {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            extractor.setDataSource(context, fs.contentUri(source), null)
            val trackCandidates = (0 until extractor.trackCount).mapNotNull { index ->
                val trackFormat = extractor.getTrackFormat(index)
                val mime = trackFormat.stringOrNull(MediaFormat.KEY_MIME) ?: return@mapNotNull null
                if (audioOnly && !mime.startsWith("audio/")) return@mapNotNull null
                if (!audioOnly && !mime.startsWith("audio/") && !mime.startsWith("video/")) return@mapNotNull null
                index to trackFormat
            }
            val availableTracks = if (audioOnly) trackCandidates.take(1) else trackCandidates
            require(availableTracks.isNotEmpty()) {
                if (audioOnly) "Source has no audio track" else "Source has no audio or video tracks"
            }
            require(availableTracks.size <= MAX_TRACKS) { "Source has too many media tracks" }

            val maxSampleSize = availableTracks.mapNotNull { (_, trackFormat) ->
                trackFormat.intOrNull(MediaFormat.KEY_MAX_INPUT_SIZE)
            }.maxOrNull() ?: DEFAULT_SAMPLE_BUFFER_BYTES
            require(maxSampleSize <= MAX_SAMPLE_BUFFER_BYTES) {
                "A media sample exceeds the ${MAX_SAMPLE_BUFFER_BYTES / (1024 * 1024)} MiB processing buffer limit"
            }
            val sampleBuffer = ByteBuffer.allocateDirect(maxOf(DEFAULT_SAMPLE_BUFFER_BYTES, maxSampleSize))
            val bufferInfo = MediaCodec.BufferInfo()
            val activeMuxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = activeMuxer

            val videoFormat = availableTracks.firstOrNull { (_, trackFormat) ->
                trackFormat.stringOrNull(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            }?.second
            val rotation = videoFormat?.intOrNull(MediaFormat.KEY_ROTATION)
            if (rotation != null && rotation != 0) {
                require(rotation in SUPPORTED_ROTATIONS) { "Unsupported video rotation metadata: $rotation" }
                activeMuxer.setOrientationHint(rotation)
            }

            val outputTrackByInput = availableTracks.associate { (inputIndex, trackFormat) ->
                extractor.selectTrack(inputIndex)
                inputIndex to activeMuxer.addTrack(trackFormat)
            }
            activeMuxer.start()
            muxerStarted = true

            var written = 0L
            while (true) {
                sampleBuffer.clear()
                val sampleSize = try {
                    extractor.readSampleData(sampleBuffer, 0)
                } catch (failure: RuntimeException) {
                    throw IOException("Could not read a media sample within the bounded buffer", failure)
                }
                if (sampleSize < 0) break
                val outputTrack = outputTrackByInput[extractor.sampleTrackIndex]
                    ?: throw IOException("Extractor returned a sample for an unselected track")
                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs < 0) throw IOException("Source has a media sample with an invalid timestamp")
                val sourceFlags = extractor.sampleFlags
                if (sourceFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED != 0) {
                    throw IOException("Encrypted media samples cannot be remuxed without decryption")
                }
                if (sourceFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
                    throw IOException("Partial media frames cannot be safely remuxed by this backend")
                }
                val outputFlags = if (sourceFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                sampleBuffer.position(0)
                sampleBuffer.limit(sampleSize)
                bufferInfo.set(0, sampleSize, presentationTimeUs, outputFlags)
                activeMuxer.writeSampleData(outputTrack, sampleBuffer, bufferInfo)
                written++
                if (!extractor.advance()) break
            }
            if (written == 0L) throw IOException("Source contains no media samples")
            activeMuxer.stop()
            muxerStarted = false
            return written
        } finally {
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            extractor.release()
        }
    }

    private fun extension(path: String): String = fs.name(path).substringAfterLast('.', "").lowercase()

    private fun MediaFormat.stringOrNull(key: String): String? =
        if (containsKey(key)) runCatching { getString(key) }.getOrNull() else null

    private fun MediaFormat.intOrNull(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private fun MediaFormat.longOrNull(key: String): Long? =
        if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null

    private companion object {
        const val MAX_TRACKS = 32
        const val DEFAULT_SAMPLE_BUFFER_BYTES = 1024 * 1024
        const val MAX_SAMPLE_BUFFER_BYTES = 32 * 1024 * 1024
        val SUPPORTED_ROTATIONS = setOf(90, 180, 270)
    }
}
