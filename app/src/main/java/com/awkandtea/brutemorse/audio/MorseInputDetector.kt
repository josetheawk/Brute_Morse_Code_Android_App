package com.awkandtea.brutemorse.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.awkandtea.brutemorse.model.MorseDefinitions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.sqrt

data class MorseInputEvent(
    val pattern: String,  // e.g., "• —"
    val durationMillis: List<Long>,  // timing of each element
    val character: String?  // decoded character or null if invalid
)

data class AudioMetrics(
    val currentRMS: Float,
    val noiseFloor: Float,
    val threshold: Float,
    val isKeyDown: Boolean
)

class MorseInputDetector(private val wpm: Int = 25) {
    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private var keyDownTime = 0L
    private var lastKeyUpTime = 0L
    private val timings = mutableListOf<Long>()

    // Use centralized timing configuration
    private val timing = com.awkandtea.brutemorse.data.MorseTimingConfig(wpm)
    private val ditMax = (timing.ditMs * 1.5).toLong()  // 1.5 units = boundary between dit and dah
    private val interCharGap = timing.interCharacterGapMs

    // Audio detection
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var isKeyCurrentlyDown = false
    private val sampleRate = 16000
    private var noiseFloor = 100.0  // Even lower starting noise floor for emulator
    private val noiseAlpha = 0.02  // Faster adaptation for noise floor
    private var pressFactor = 2.5  // Even more sensitive threshold
    private val debounceMs = 10  // Even shorter debounce

    // Expose current audio metrics for testing
    private var currentRMS = 0.0

    fun getAudioMetrics(): AudioMetrics {
        return AudioMetrics(
            currentRMS = currentRMS.toFloat(),
            noiseFloor = noiseFloor.toFloat(),
            threshold = (noiseFloor * pressFactor).toFloat(),
            isKeyDown = isKeyCurrentlyDown
        )
    }

    fun updateSensitivity(sensitivity: Float) {
        pressFactor = sensitivity.toDouble()
    }

    fun startAudioListening() {
        if (isListening) return

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            audioRecord?.startRecording()
            isListening = true

            // Start monitoring audio in background
            Thread {
                val buffer = ShortArray(bufferSize)
                var lastChange = SystemClock.elapsedRealtime()
                var sampleCount = 0

                while (isListening) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        // Calculate RMS amplitude
                        var sum = 0.0
                        for (i in 0 until read) {
                            val v = buffer[i].toDouble()
                            sum += v * v
                        }
                        val rms = sqrt(sum / read)
                        currentRMS = rms  // Store for metrics

                        // Adaptive noise floor - only adapt when clearly silent
                        val threshold = noiseFloor * pressFactor
                        if (rms < threshold * 0.5) {
                            noiseFloor = (1 - noiseAlpha) * noiseFloor + noiseAlpha * rms
                        }

                        // Debug logging every 100 samples
                        sampleCount++
                        if (sampleCount % 100 == 0) {
                            android.util.Log.d("MorseInputDetector",
                                "RMS: $rms, Threshold: $threshold, NoiseFloor: $noiseFloor, Pressed: $isKeyCurrentlyDown")
                        }

                        // Detect key press/release based on amplitude with debounce
                        val now = SystemClock.elapsedRealtime()
                        val wantPressed = rms > threshold

                        if (wantPressed != isKeyCurrentlyDown && (now - lastChange) > debounceMs) {
                            isKeyCurrentlyDown = wantPressed
                            lastChange = now

                            android.util.Log.d("MorseInputDetector",
                                "Key state changed: ${if (wantPressed) "DOWN" else "UP"}, RMS: $rms")

                            if (isKeyCurrentlyDown) {
                                onKeyDown()
                            } else {
                                onKeyUp()
                            }
                        }
                    }
                }
            }.start()
        } catch (e: SecurityException) {
            // Permission not granted
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAudioListening() {
        isListening = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }

    // For screen tap fallback
    fun onKeyDown() {
        keyDownTime = SystemClock.elapsedRealtime()
        android.util.Log.d("MorseInputDetector", "onKeyDown called at $keyDownTime")
    }

    fun onKeyUp() {
        val now = SystemClock.elapsedRealtime()
        android.util.Log.d("MorseInputDetector", "onKeyUp called at $now")
        if (keyDownTime > 0) {
            val duration = now - keyDownTime
            timings.add(duration)

            // Determine if dit or dah - using nice symbols
            val symbol = if (duration < ditMax) "•" else "—"
            _currentInput.value += symbol

            android.util.Log.d("MorseInputDetector",
                "Key held for ${duration}ms, symbol: $symbol, ditMax: $ditMax")

            lastKeyUpTime = now
            keyDownTime = 0
        }
    }

    fun checkCompletion(completionTimeoutMs: Long = interCharGap): MorseInputEvent? {
        val now = SystemClock.elapsedRealtime()

        // If enough time has passed since last key up, consider it complete
        if (lastKeyUpTime > 0 && (now - lastKeyUpTime) > completionTimeoutMs) {
            val pattern = _currentInput.value
            if (pattern.isNotEmpty()) {
                // Convert display symbols back to standard morse for lookup
                val standardPattern = pattern
                    .replace("•", ".")
                    .replace("—", "-")

                val decoded = MorseDefinitions.morseMap.entries
                    .firstOrNull { it.value == standardPattern }?.key

                val event = MorseInputEvent(
                    pattern = pattern,  // Keep the nice display symbols
                    durationMillis = timings.toList(),
                    character = decoded
                )

                // Reset for next character
                reset()
                return event
            }
        }
        return null
    }

    fun reset() {
        _currentInput.value = ""
        timings.clear()
        keyDownTime = 0
        lastKeyUpTime = 0
    }

    fun forceComplete(): MorseInputEvent? {
        val pattern = _currentInput.value
        if (pattern.isNotEmpty()) {
            val standardPattern = pattern
                .replace("•", ".")
                .replace("—", "-")

            val decoded = MorseDefinitions.morseMap.entries
                .firstOrNull { it.value == standardPattern }?.key

            val event = MorseInputEvent(
                pattern = pattern,
                durationMillis = timings.toList(),
                character = decoded
            )
            reset()
            return event
        }
        return null
    }
}