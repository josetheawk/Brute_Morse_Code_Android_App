package com.example.brutemorse.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

class MorseAudioPlayer {

    suspend fun playMorsePattern(
        pattern: String,
        frequencyHz: Int,
        wpm: Int
    ) = withContext(Dispatchers.IO) {
        val unitMs = (1200f / wpm).toLong()
        val sampleRate = 44100

        // Calculate total duration and generate complete audio buffer
        val totalDurationMs = calculatePatternDuration(pattern, unitMs)
        val totalSamples = (totalDurationMs * sampleRate / 1000).toInt()
        val samples = ShortArray(totalSamples)

        // Generate the complete morse pattern with precise timing
        var currentSampleOffset = 0

        pattern.forEach { char ->
            val elementDurationMs = when (char) {
                '.', '·', '•' -> unitMs        // Dit
                '-', '−', '–', '—' -> unitMs * 3   // Dah
                ' ' -> unitMs * 3               // Word space (just silence)
                else -> 0L
            }

            if (elementDurationMs > 0 && char != ' ') {
                // Generate tone for dit/dah
                val elementSamples = (elementDurationMs * sampleRate / 1000).toInt()
                generateTone(samples, currentSampleOffset, elementSamples, frequencyHz, sampleRate)
                currentSampleOffset += elementSamples
            } else if (char == ' ') {
                // Just advance for space (silence)
                currentSampleOffset += (elementDurationMs * sampleRate / 1000).toInt()
            }

            // Add gap after element (1 unit of silence)
            currentSampleOffset += (unitMs * sampleRate / 1000).toInt()
        }

        // Play the complete buffer
        playAudioBuffer(samples, sampleRate)
    }

    private fun calculatePatternDuration(pattern: String, unitMs: Long): Long {
        var duration = 0L
        pattern.forEach { char ->
            duration += when (char) {
                '.', '·', '•' -> unitMs
                '-', '−', '–', '—' -> unitMs * 3
                ' ' -> unitMs * 3
                else -> 0L
            }
            duration += unitMs // Gap after each element
        }
        duration += unitMs * 2 // Final inter-character gap
        return duration
    }

    private fun generateTone(
        buffer: ShortArray,
        startOffset: Int,
        numSamples: Int,
        frequencyHz: Int,
        sampleRate: Int
    ) {
        val fadeMs = 3
        val fadeSamples = (fadeMs * sampleRate / 1000).coerceAtMost(numSamples / 2)

        for (i in 0 until numSamples.coerceAtMost(buffer.size - startOffset)) {
            val angle = 2.0 * Math.PI * i / (sampleRate / frequencyHz.toDouble())
            var amplitude = 0.5

            // Fade in
            if (i < fadeSamples) {
                amplitude *= (i.toDouble() / fadeSamples)
            }
            // Fade out
            if (i > numSamples - fadeSamples) {
                amplitude *= ((numSamples - i).toDouble() / fadeSamples)
            }

            buffer[startOffset + i] = (sin(angle) * Short.MAX_VALUE * amplitude).toInt().toShort()
        }
    }

    private suspend fun playAudioBuffer(samples: ShortArray, sampleRate: Int) {
        var track: AudioTrack? = null
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufferSize, samples.size * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()

            // Write buffer
            var offset = 0
            while (offset < samples.size) {
                val written = track.write(samples, offset, samples.size - offset)
                if (written < 0) break
                offset += written
            }

            // Wait for playback
            val durationMs = samples.size * 1000L / sampleRate
            kotlinx.coroutines.delay(durationMs)

            track.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            track?.release()
        }
    }

    fun release() {
        // Nothing to release
    }
}