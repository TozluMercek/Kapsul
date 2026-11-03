package com.example.kapsul.ui.attachments

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

/** Waveform bars (0..1) and the silent stretches (ms) of a recording. */
class AudioProfile(val bars: FloatArray, val silences: List<LongRange>)

private const val WINDOW_US = 50_000L       // 50 ms loudness windows
private const val MIN_SILENCE_MS = 700L      // shorter pauses are natural speech
private const val SILENCE_PADDING_MS = 150L  // keep a little breath around words

/**
 * Decodes the file to PCM on-device and measures loudness per 50 ms window.
 * Returns null if the format can't be decoded.
 */
suspend fun analyzeAudio(file: File, bins: Int = 72): AudioProfile? = withContext(Dispatchers.Default) {
    runCatching {
        val extractor = MediaExtractor().apply { setDataSource(file.absolutePath) }
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return@runCatching null
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val windows = ArrayList<Float>()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val out = codec.dequeueOutputBuffer(info, 10_000)
                if (out >= 0) {
                    val buffer = codec.getOutputBuffer(out)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val samples = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        var sum = 0.0
                        var count = 0
                        while (samples.hasRemaining()) {
                            val s = samples.get() / 32768.0
                            sum += s * s
                            count++
                        }
                        val rms = if (count > 0) sqrt(sum / count).toFloat() else 0f
                        val window = (info.presentationTimeUs / WINDOW_US).toInt()
                        while (windows.size <= window) windows += 0f
                        if (rms > windows[window]) windows[window] = rms
                    }
                    codec.releaseOutputBuffer(out, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        } finally {
            codec.stop(); codec.release(); extractor.release()
        }
        if (windows.isEmpty()) return@runCatching null

        val peak = windows.max().coerceAtLeast(1e-4f)
        val bars = FloatArray(bins) { b ->
            val from = b * windows.size / bins
            val to = ((b + 1) * windows.size / bins).coerceAtLeast(from + 1).coerceAtMost(windows.size)
            (windows.subList(from, to).max() / peak).coerceIn(0.05f, 1f)
        }

        // Quiet relative to this recording's own loudness (works for whispers and loud rooms).
        val threshold = peak * 0.08f
        val windowMs = WINDOW_US / 1000
        val silences = mutableListOf<LongRange>()
        var start = -1
        for (i in 0..windows.size) {
            val quiet = i < windows.size && windows[i] < threshold
            if (quiet && start < 0) start = i
            if (!quiet && start >= 0) {
                val from = start * windowMs + SILENCE_PADDING_MS
                val to = i * windowMs - SILENCE_PADDING_MS
                if (to - from >= MIN_SILENCE_MS - 2 * SILENCE_PADDING_MS) silences += from..to
                start = -1
            }
        }
        AudioProfile(bars, silences)
    }.getOrNull()
}
