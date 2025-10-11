package com.example.brutemorse.viewmodel

import com.example.brutemorse.model.SessionStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val currentStep: SessionStep? = null,
    val currentIndex: Int = 0,
    val totalSteps: Int = 0,
    val elapsedMillis: Long = 0L,
    val totalMillis: Long = 0L
)
