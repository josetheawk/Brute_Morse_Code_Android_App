package com.example.brutemorse.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

class MorseAudioPlayer(private val context: Context) {

    // Reusable audio track for continuous tone
    private var toneTrack: AudioTrack? = null
    private var toneBuffer: ShortArray? = null
    private val sampleRate = 44100
    private var isPlaying = false

    suspend fun playMorsePattern(
        pattern: String,
        frequencyHz: Int,
        wpm: Int
    ) = withContext(Dispatchers.IO) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previousMode = audioManager.mode
        val previousSpeakerphone = audioManager.isSpeakerphoneOn

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        try {
            // Use centralized timing
            val timing = com.example.brutemorse.data.MorseTimingConfig(wpm)
            val totalDurationMs = com.example.brutemorse.data.MorseTimingConfig.calculateDuration(pattern, timing)
            val totalSamples = (totalDurationMs * sampleRate / 1000).toInt()
            val samples = ShortArray(totalSamples)
            var currentSampleOffset = 0

            pattern.forEach { char ->
                val elementDurationMs = when (char) {
                    '.', '·', '•' -> timing.ditMs
                    '-', '−', '–', '—' -> timing.dahMs
                    ' ' -> timing.interCharacterGapMs
                    else -> 0L
                }

                if (elementDurationMs > 0 && char != ' ') {
                    val elementSamples = (elementDurationMs * sampleRate / 1000).toInt()
                    generateTone(samples, currentSampleOffset, elementSamples, frequencyHz, sampleRate)
                    currentSampleOffset += elementSamples
                } else if (char == ' ') {
                    currentSampleOffset += (elementDurationMs * sampleRate / 1000).toInt()
                }
                // Add intra-character gap
                currentSampleOffset += (timing.intraCharacterGapMs * sampleRate / 1000).toInt()
            }

            playAudioBuffer(samples, sampleRate)
        } finally {
            audioManager.mode = previousMode
            audioManager.isSpeakerphoneOn = previousSpeakerphone
        }
    }

    fun startContinuousTone(frequencyHz: Int) {
        // Initialize track and buffer if needed or if frequency changed
        if (toneTrack == null) {
            initializeToneTrack(frequencyHz)
        }

        isPlaying = true
        try {
            toneTrack?.play()
        } catch (e: Exception) {
            android.util.Log.e("MorseAudioPlayer", "Error starting tone", e)
            releaseToneTrack()
            initializeToneTrack(frequencyHz)
            toneTrack?.play()
        }
    }

    fun stopContinuousTone() {
        isPlaying = false
        try {
            toneTrack?.pause()
            toneTrack?.flush()
        } catch (e: Exception) {
            android.util.Log.e("MorseAudioPlayer", "Error stopping tone", e)
        }
    }

    private fun initializeToneTrack(frequencyHz: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // Generate 500ms of continuous tone for smoother looping
        val bufferSamples = sampleRate / 2 // 500ms worth - longer for smoother sound
        toneBuffer = ShortArray(bufferSamples)

        // Generate perfect sine wave with no discontinuities
        val cyclesPerBuffer = (frequencyHz.toDouble() * bufferSamples) / sampleRate
        val samplesPerCycle = sampleRate.toDouble() / frequencyHz

        for (i in 0 until bufferSamples) {
            val phase = (2.0 * Math.PI * i) / samplesPerCycle
            toneBuffer!![i] = (sin(phase) * Short.MAX_VALUE * 0.6).toInt().toShort()
        }

        try {
            toneTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(maxOf(minBufferSize * 4, bufferSamples * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            // Write the buffer once - it will loop automatically in STATIC mode
            toneTrack?.write(toneBuffer!!, 0, bufferSamples)
            toneTrack?.setLoopPoints(0, bufferSamples, -1) // Loop forever
        } catch (e: Exception) {
            android.util.Log.e("MorseAudioPlayer", "Error initializing tone track", e)
            releaseToneTrack()
        }
    }

    private fun releaseToneTrack() {
        try {
            toneTrack?.stop()
            toneTrack?.release()
        } catch (e: Exception) {
            android.util.Log.e("MorseAudioPlayer", "Error releasing tone track", e)
        }
        toneTrack = null
        toneBuffer = null
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

            if (i < fadeSamples) {
                amplitude *= (i.toDouble() / fadeSamples)
            }
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

            var offset = 0
            while (offset < samples.size) {
                val written = track.write(samples, offset, samples.size - offset)
                if (written < 0) break
                offset += written
            }

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
        releaseToneTrack()
    }
}