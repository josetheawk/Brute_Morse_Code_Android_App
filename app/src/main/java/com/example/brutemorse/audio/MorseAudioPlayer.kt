package com.example.brutemorse.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MorseAudioPlayer {

    suspend fun playTone(frequencyHz: Int, durationMillis: Long) = withContext(Dispatchers.IO) {
        var track: AudioTrack? = null
        try {
            val sampleRate = 44100
            val numSamples = (durationMillis * sampleRate / 1000).toInt()
            val samples = ShortArray(numSamples)

            // Attack/release envelope duration (5ms each to prevent clicks)
            val fadeMs = 5
            val fadeSamples = (fadeMs * sampleRate / 1000).coerceAtMost(numSamples / 2)

            // Generate sine wave with fade in/out
            for (i in samples.indices) {
                val angle = 2.0 * Math.PI * i / (sampleRate / frequencyHz.toDouble())
                var amplitude = 0.5

                // Fade in at start
                if (i < fadeSamples) {
                    amplitude *= (i.toDouble() / fadeSamples)
                }
                // Fade out at end
                if (i > numSamples - fadeSamples) {
                    amplitude *= ((numSamples - i).toDouble() / fadeSamples)
                }

                samples[i] = (kotlin.math.sin(angle) * Short.MAX_VALUE * amplitude).toInt().toShort()
            }

            // Create audio track in STREAM mode for sequential playback
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
                .setBufferSizeInBytes(numSamples * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()

            // Write samples in chunks and wait for them to play
            var offset = 0
            val chunkSize = 4096
            while (offset < samples.size) {
                val toWrite = minOf(chunkSize, samples.size - offset)
                val written = track.write(samples, offset, toWrite)
                if (written < 0) break
                offset += written
            }

            // Wait for all audio to finish playing
            // Calculate actual playback time
            val playbackTimeMs = (numSamples * 1000L / sampleRate)
            kotlinx.coroutines.delay(playbackTimeMs)

            track.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            track?.release()
        }
    }

    fun release() {
        // Nothing to release anymore since we create/release per tone
    }
}