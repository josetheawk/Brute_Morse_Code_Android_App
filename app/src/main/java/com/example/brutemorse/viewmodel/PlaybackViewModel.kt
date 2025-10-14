package com.example.brutemorse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brutemorse.data.SessionRepository
import com.example.brutemorse.data.SettingsRepository
import com.example.brutemorse.audio.MorseAudioPlayer
import com.example.brutemorse.audio.TextToSpeechPlayer
import com.example.brutemorse.model.MorseElement
import com.example.brutemorse.model.SpeechElement
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
    private val settingsRepository: SettingsRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val _settingsState = MutableStateFlow(UserSettings())
    val settingsState: StateFlow<UserSettings> = _settingsState.asStateFlow()

    val scenarios: List<ScenarioScript> = sessionRepository.scenarios

    private var session: List<SessionStep> = emptyList()
    private var tickerJob: Job? = null
    private var playbackJob: Job? = null
    private var stepOffsets: List<Long> = emptyList()
    private val audioPlayer = MorseAudioPlayer()
    private val ttsPlayer = TextToSpeechPlayer(context)

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
            startPlayback()
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
        startPlayback()
    }

    private fun pause() {
        _uiState.value = _uiState.value.copy(isPlaying = false)
        tickerJob?.cancel()
        playbackJob?.cancel()
    }

    fun skipNext() {
        if (session.isEmpty()) return
        val nextIndex = (_uiState.value.currentIndex + 1).coerceAtMost(session.lastIndex)
        updateIndex(nextIndex)
        if (_uiState.value.isPlaying) {
            startPlayback()
        }
    }

    fun skipPrevious() {
        if (session.isEmpty()) return
        val previousIndex = (_uiState.value.currentIndex - 1).coerceAtLeast(0)
        updateIndex(previousIndex)
        if (_uiState.value.isPlaying) {
            startPlayback()
        }
    }

    fun skipPhase() {
        if (session.isEmpty()) return
        val currentStep = _uiState.value.currentStep ?: return
        val nextPhaseIndex = session.indexOfFirst { step ->
            step.descriptor.phaseIndex > currentStep.descriptor.phaseIndex
        }
        if (nextPhaseIndex != -1) {
            updateIndex(nextPhaseIndex)
            if (_uiState.value.isPlaying) {
                startPlayback()
            }
        }
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val updated = transform(settingsState.value)
        _settingsState.value = updated
        viewModelScope.launch { sessionRepository.saveSettings(updated) }
    }

    private fun updateIndex(index: Int) {
        if (session.isEmpty()) return
        playbackJob?.cancel()
        tickerJob?.cancel()
        val newElapsed = stepOffsets.getOrNull(index) ?: 0L
        _uiState.value = _uiState.value.copy(
            currentIndex = index,
            currentStep = session.getOrNull(index),
            elapsedMillis = newElapsed
        )
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        tickerJob?.cancel()

        // Start UI ticker
        tickerJob = viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                delay(100)
                val snapshot = _uiState.value
                val total = snapshot.totalMillis
                if (total == 0L) continue
                val newElapsed = (snapshot.elapsedMillis + 100).coerceAtMost(total)
                val finished = newElapsed >= total
                _uiState.value = snapshot.copy(
                    elapsedMillis = newElapsed,
                    isPlaying = if (finished) false else snapshot.isPlaying
                )
                if (finished) break
            }
        }

        // Start audio playback
        playbackJob = viewModelScope.launch {
            var currentIndex = _uiState.value.currentIndex

            while (currentIndex < session.size && _uiState.value.isPlaying) {
                val step = session[currentIndex]

                // Play each element in the step sequentially
                for (elementIndex in step.elements.indices) {
                    if (!_uiState.value.isPlaying) break

                    val element = step.elements[elementIndex]

                    // Update UI to show ONLY the current element
                    _uiState.value = _uiState.value.copy(
                        currentIndex = currentIndex,
                        currentStep = step.copy(
                            elements = listOf(element)  // Only current element!
                        )
                    )

                    when (element) {
                        is MorseElement -> {
                            audioPlayer.playMorsePattern(
                                pattern = element.symbol,
                                frequencyHz = element.toneFrequencyHz,
                                wpm = element.wpm
                            )
                        }
                        is SpeechElement -> {
                            ttsPlayer.speak(element.text)
                        }
                        else -> {
                            delay(element.durationMillis)
                        }
                    }
                }

                // Move to next step
                currentIndex++
            }

            // Finished
            if (_uiState.value.isPlaying) {
                _uiState.value = _uiState.value.copy(isPlaying = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        playbackJob?.cancel()
        audioPlayer.release()
        ttsPlayer.release()
    }
}

class PlaybackViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaybackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaybackViewModel(sessionRepository, settingsRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}