package com.example.brutemorse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brutemorse.data.SessionRepository
import com.example.brutemorse.data.SettingsRepository
import com.example.brutemorse.data.UserSettings
import com.example.brutemorse.model.SessionStep
import com.example.brutemorse.model.ScenarioScript
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackViewModel(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _settingsState = MutableStateFlow(UserSettings())
    val settingsState: StateFlow<UserSettings> = _settingsState.asStateFlow()

    val scenarios: List<ScenarioScript> = sessionRepository.scenarios

    private var session: List<SessionStep> = emptyList()
    private var tickerJob: Job? = null
    private var stepOffsets: List<Long> = emptyList()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _settingsState.value = settings
            }
        }
    }

    fun generateSession() {
        viewModelScope.launch {
            val settings = settingsState.value
            session = sessionRepository.generateSession(settings)
            val offsets = mutableListOf<Long>()
            var running = 0L
            session.forEach { step ->
                offsets += running
                running += step.elements.sumOf { it.durationMillis }
            }
            stepOffsets = offsets
            if (session.isEmpty()) {
                _uiState.value = PlaybackUiState(totalMillis = 0L, totalSteps = 0)
                return@launch
            }
            _uiState.value = PlaybackUiState(
                isPlaying = true,
                currentStep = session.firstOrNull(),
                currentIndex = 0,
                totalSteps = session.size,
                elapsedMillis = 0L,
                totalMillis = running
            )
            startTicker()
        }
    }

    fun togglePlayback() {
        val current = _uiState.value
        if (current.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        if (session.isEmpty()) return
        _uiState.value = _uiState.value.copy(isPlaying = true)
        startTicker()
    }

    private fun pause() {
        _uiState.value = _uiState.value.copy(isPlaying = false)
        tickerJob?.cancel()
    }

    fun skipNext() {
        if (session.isEmpty()) return
        val nextIndex = (_uiState.value.currentIndex + 1).coerceAtMost(session.lastIndex)
        updateIndex(nextIndex)
    }

    fun skipPrevious() {
        if (session.isEmpty()) return
        val previousIndex = (_uiState.value.currentIndex - 1).coerceAtLeast(0)
        updateIndex(previousIndex)
    }

    fun skipPhase() {
        if (session.isEmpty()) return
        val currentStep = _uiState.value.currentStep ?: return
        val nextPhaseIndex = session.indexOfFirst { step ->
            step.descriptor.phaseIndex > currentStep.descriptor.phaseIndex
        }
        if (nextPhaseIndex != -1) {
            updateIndex(nextPhaseIndex)
        }
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val updated = transform(settingsState.value)
        _settingsState.value = updated
        viewModelScope.launch { sessionRepository.saveSettings(updated) }
    }

    private fun updateIndex(index: Int) {
        if (session.isEmpty()) return
        val newElapsed = stepOffsets.getOrNull(index) ?: 0L
        _uiState.value = _uiState.value.copy(
            currentIndex = index,
            currentStep = session.getOrNull(index),
            elapsedMillis = newElapsed
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val snapshot = _uiState.value
                if (!snapshot.isPlaying) continue
                val total = snapshot.totalMillis
                if (total == 0L) continue
                val newElapsed = (snapshot.elapsedMillis + 1000).coerceAtMost(total)
                if (session.isEmpty() || stepOffsets.isEmpty()) continue
                val newIndex = stepOffsets.indexOfLast { offset -> offset <= newElapsed }
                    .coerceAtLeast(0)
                    .coerceAtMost(session.lastIndex)
                val finished = newElapsed >= total
                _uiState.value = snapshot.copy(
                    elapsedMillis = newElapsed,
                    currentIndex = newIndex,
                    currentStep = session.getOrNull(newIndex),
                    isPlaying = if (finished) false else snapshot.isPlaying
                )
                if (finished) break
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}

class PlaybackViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaybackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaybackViewModel(sessionRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
