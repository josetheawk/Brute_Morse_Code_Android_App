package com.example.brutemorse.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class TextToSpeechPlayer(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.9f) // Slightly slower for clarity
                isInitialized = true
            }
        }
    }

    suspend fun speak(text: String): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isInitialized || tts == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val utteranceId = "utterance_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }

            override fun onError(utteranceId: String?) {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        })

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        if (result != TextToSpeech.SUCCESS) {
            continuation.resume(false)
        }

        continuation.invokeOnCancellation {
            tts?.stop()
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}