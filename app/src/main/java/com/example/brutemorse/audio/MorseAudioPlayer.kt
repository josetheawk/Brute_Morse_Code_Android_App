package com.example.brutemorse.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

class MorseAudioPlayer {
    private var audioTrack: AudioTrack? = null
    
    suspend fun playTone(frequencyHz: Int, durationMillis: Long) = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMillis * sampleRate / 1000).toInt()
            val samples = ShortArray(numSamples)
            
            // Generate sine wave
            for (i in samples.indices) {
                val angle = 2.0 * Math.PI * i / (sampleRate / frequencyHz.toDouble())
                samples[i] = (sin(angle) * Short.MAX_VALUE * 0.5).toInt().toShort()
            }
            
            // Create and play audio track
            val track = AudioTrack.Builder()
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
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            
            audioTrack = track
            track.write(samples, 0, samples.size)
            track.play()
            
            // Wait for playback to finish
            kotlinx.coroutines.delay(durationMillis)
            
            track.stop()
            track.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        audioTrack?.release()
        audioTrack = null
    }
}
