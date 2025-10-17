package com.awkandtea.brutemorse.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

class TextToSpeechPlayer(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isInitializing = false
    private val initTimeout = 5000L

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        if (isInitializing || isInitialized) return

        isInitializing = true
        try {
            tts = TextToSpeech(context) { status ->
                isInitializing = false
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        android.util.Log.e("TextToSpeechPlayer", "Language not supported")
                        isInitialized = false
                    } else {
                        tts?.setSpeechRate(0.9f)
                        isInitialized = true
                        android.util.Log.d("TextToSpeechPlayer", "TTS initialized successfully")
                    }
                } else {
                    android.util.Log.e("TextToSpeechPlayer", "TTS initialization failed with status: $status")
                    isInitialized = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TextToSpeechPlayer", "Error initializing TTS", e)
            isInitializing = false
            isInitialized = false
        }
    }

    suspend fun speak(text: String): Boolean {
        if (text.isBlank()) {
            android.util.Log.w("TextToSpeechPlayer", "Attempted to speak blank text")
            return false
        }

        if (!isInitialized) {
            android.util.Log.w("TextToSpeechPlayer", "TTS not initialized, attempting to initialize")

            if (!isInitializing) {
                initializeTTS()
            }

            val initWaitResult = withTimeoutOrNull(initTimeout) {
                var waited = 0L
                while (!isInitialized && waited < initTimeout) {
                    delay(100L)
                    waited += 100L
                }
                isInitialized
            }

            if (initWaitResult != true) {
                android.util.Log.e("TextToSpeechPlayer", "TTS initialization timed out or failed")
                return false
            }
        }

        return suspendCancellableCoroutine { continuation ->
            if (!isInitialized || tts == null) {
                android.util.Log.e("TextToSpeechPlayer", "TTS still not available after initialization attempt")
                if (continuation.isActive) {
                    continuation.resume(false)
                }
                return@suspendCancellableCoroutine
            }

            val utteranceId = "utterance_${System.currentTimeMillis()}"

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    android.util.Log.d("TextToSpeechPlayer", "Speech started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    android.util.Log.d("TextToSpeechPlayer", "Speech completed: $utteranceId")
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onError(utteranceId: String?) {
                    android.util.Log.e("TextToSpeechPlayer", "Speech error: $utteranceId")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }

                @Deprecated("Deprecated in API level 21")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    android.util.Log.e("TextToSpeechPlayer", "Speech error: $utteranceId, code: $errorCode")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            })

            try {
                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

                if (result != TextToSpeech.SUCCESS) {
                    android.util.Log.e("TextToSpeechPlayer", "speak() returned failure: $result")
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TextToSpeechPlayer", "Exception during speak()", e)
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }

            continuation.invokeOnCancellation {
                try {
                    tts?.stop()
                    android.util.Log.d("TextToSpeechPlayer", "Speech cancelled")
                } catch (e: Exception) {
                    android.util.Log.e("TextToSpeechPlayer", "Error stopping TTS on cancellation", e)
                }
            }
        }
    }

    fun isTTSInitialized(): Boolean = isInitialized

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            android.util.Log.d("TextToSpeechPlayer", "TTS released")
        } catch (e: Exception) {
            android.util.Log.e("TextToSpeechPlayer", "Error releasing TTS", e)
        } finally {
            tts = null
            isInitialized = false
            isInitializing = false
        }
    }
}
