package com.awkandtea.brutemorse.viewmodel

import com.awkandtea.brutemorse.model.PhaseDescriptor

data class CharacterAttempt(
    val expectedChar: String,
    val expectedPattern: String,
    val userPattern: String,
    val isCorrect: Boolean
)

data class ActiveUiState(
    val currentTokens: List<String> = emptyList(),
    val currentPosition: Int = 0,
    val currentInput: String = "",
    val attempts: List<CharacterAttempt> = emptyList(),
    val isReviewMode: Boolean = false,
    val score: Pair<Int, Int> = 0 to 0,
    val phaseDescriptor: PhaseDescriptor? = null,
    val passIndex: Int = 0,
    val totalPasses: Int = 0,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0
)
