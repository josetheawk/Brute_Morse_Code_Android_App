package com.example.brutemorse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brutemorse.data.SessionRepository
import com.example.brutemorse.data.SettingsRepository
import com.example.brutemorse.audio.MorseAudioPlayer
import com.example.brutemorse.audio.TextToSpeechPlayer
import com.example.brutemorse.audio.MorseInputDetector
import com.example.brutemorse.audio.MorseInputEvent
import com.example.brutemorse.model.MorseElement
import com.example.brutemorse.model.SpeechElement
import com.example.brutemorse.data.UserSettings
import com.example.brutemorse.model.SessionStep
import com.example.brutemorse.model.ScenarioScript
import com.example.brutemorse.model.MorseDefinitions
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

    private val _activeState = MutableStateFlow(ActiveUiState())
    val activeState: StateFlow<ActiveUiState> = _activeState.asStateFlow()

    val scenarios: List<ScenarioScript> = sessionRepository.scenarios

    private var session: List<SessionStep> = emptyList()
    private var tickerJob: Job? = null
    private var playbackJob: Job? = null
    private var stepOffsets: List<Long> = emptyList()
    private val audioPlayer = MorseAudioPlayer(context)
    private val ttsPlayer = TextToSpeechPlayer(context)

    private var inputDetector = MorseInputDetector(wpm = 25)
    private var activeSession: List<SessionStep> = emptyList()
    private var currentStepIndex = 0
    private var completionCheckJob: Job? = null
    private var audioInputEnabled = false

    // Store completed results history
    private data class CompletedSet(
        val stepIndex: Int,
        val tokens: List<String>,
        val attempts: List<CharacterAttempt>,
        val score: Pair<Int, Int>,
        val descriptor: com.example.brutemorse.model.PhaseDescriptor
    )
    private val completedSets = mutableListOf<CompletedSet>()

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

    fun pausePlayback() {
        // Called when leaving Listen screen
        pause()
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

    fun jumpToPhase(phaseNumber: Int) {
        if (session.isEmpty()) return
        val phaseStartIndex = session.indexOfFirst { step ->
            step.descriptor.phaseIndex == phaseNumber
        }
        if (phaseStartIndex != -1) {
            updateIndex(phaseStartIndex)
            if (_uiState.value.isPlaying) {
                startPlayback()
            }
        }
    }

    fun jumpToSubPhase(phaseNumber: Int, subPhaseNumber: Int) {
        if (session.isEmpty()) return
        val subPhaseStartIndex = session.indexOfFirst { step ->
            step.descriptor.phaseIndex == phaseNumber &&
                    step.descriptor.subPhaseIndex == subPhaseNumber
        }
        if (subPhaseStartIndex != -1) {
            updateIndex(subPhaseStartIndex)
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

    // Active Practice Methods

    fun generateActiveSession() {
        viewModelScope.launch {
            val settings = settingsState.value
            activeSession = sessionRepository.generateSession(settings)
            currentStepIndex = 0
            completedSets.clear()

            if (activeSession.isEmpty()) {
                _activeState.value = ActiveUiState()
                return@launch
            }

            loadActiveStep(0)
        }
    }

    fun enableAudioInput() {
        try {
            audioInputEnabled = true
            // Use audio detection for straight key
            inputDetector.startAudioListening()
        } catch (e: Exception) {
            // Audio failed - that's okay, screen tap still works
            e.printStackTrace()
            audioInputEnabled = false
        }
    }

    private fun loadActiveStep(index: Int) {
        if (index >= activeSession.size) {
            // Session complete
            _activeState.value = ActiveUiState()
            if (audioInputEnabled) {
                inputDetector.stopAudioListening()
            }
            return
        }

        val step = activeSession[index]
        currentStepIndex = index

        // Extract tokens from step (only morse elements with distinct characters)
        val tokens = step.elements
            .filterIsInstance<MorseElement>()
            .map { it.character }
            .distinct()

        _activeState.value = ActiveUiState(
            currentTokens = tokens,
            currentPosition = 0,
            currentInput = "",
            attempts = emptyList(),
            isReviewMode = false,
            phaseDescriptor = step.descriptor,
            passIndex = step.passIndex,
            totalPasses = step.passCount
        )

        // Start completion checker
        startCompletionChecker()
    }

    fun onActiveKeyDown() {
        inputDetector.onKeyDown()
    }

    fun onActiveKeyUp() {
        inputDetector.onKeyUp()
        _activeState.value = _activeState.value.copy(
            currentInput = inputDetector.currentInput.value
        )
    }

    private fun startCompletionChecker() {
        completionCheckJob?.cancel()
        completionCheckJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val state = _activeState.value

                if (!state.isReviewMode) {
                    // Determine timeout based on whether we're doing single letters or phrases
                    val isSingleLetterDrill = state.currentTokens.size <= 3 &&
                            state.currentTokens.all { it.length == 1 }

                    // Shorter timeout for single letters, longer for phrases
                    val timeoutMs = if (isSingleLetterDrill) 800L else 2500L

                    // Check if current character is complete
                    val event = inputDetector.checkCompletion(completionTimeoutMs = timeoutMs)

                    if (event != null) {
                        handleCharacterComplete(event)
                    }
                }
            }
        }
    }

    private fun handleCharacterComplete(event: MorseInputEvent) {
        val state = _activeState.value
        val expectedChar = state.currentTokens.getOrNull(state.currentPosition) ?: return
        val expectedPattern = MorseDefinitions.morseMap[expectedChar] ?: ""

        // Convert to display format (• and —)
        val expectedDisplay = expectedPattern.replace(".", "•").replace("-", "—")

        val attempt = CharacterAttempt(
            expectedChar = expectedChar,
            expectedPattern = expectedDisplay,
            userPattern = event.pattern,
            isCorrect = event.pattern == expectedDisplay
        )

        val newAttempts = state.attempts + attempt
        val newPosition = state.currentPosition + 1

        if (newPosition >= state.currentTokens.size) {
            // All characters complete, show review
            val correct = newAttempts.count { it.isCorrect }
            val total = newAttempts.size

            // Save to history
            val completed = CompletedSet(
                stepIndex = currentStepIndex,
                tokens = state.currentTokens,
                attempts = newAttempts,
                score = correct to total,
                descriptor = state.phaseDescriptor!!
            )
            completedSets.add(completed)

            _activeState.value = state.copy(
                attempts = newAttempts,
                isReviewMode = true,
                score = correct to total,
                currentInput = ""
            )

            completionCheckJob?.cancel()
        } else {
            // Move to next character
            _activeState.value = state.copy(
                currentPosition = newPosition,
                currentInput = "",
                attempts = newAttempts
            )
            inputDetector.reset()
        }
    }

    fun onActiveNextSet() {
        loadActiveStep(currentStepIndex + 1)
    }

    fun onActiveBackToResults() {
        // Go back to previous completed set's results
        if (completedSets.size >= 2) {
            // Remove current set from history
            completedSets.removeLastOrNull()

            // Get previous set
            val previousSet = completedSets.lastOrNull()
            if (previousSet != null) {
                currentStepIndex = previousSet.stepIndex
                _activeState.value = ActiveUiState(
                    currentTokens = previousSet.tokens,
                    currentPosition = previousSet.tokens.size, // Already completed
                    currentInput = "",
                    attempts = previousSet.attempts,
                    isReviewMode = true,
                    score = previousSet.score,
                    phaseDescriptor = previousSet.descriptor,
                    passIndex = previousSet.descriptor.phaseIndex,
                    totalPasses = activeSession.size
                )
            }
        }
        // If only 1 or 0 completed sets, do nothing (stay on current results)
    }

    fun jumpToPhaseActive(phaseNumber: Int) {
        if (activeSession.isEmpty()) return
        val phaseStartIndex = activeSession.indexOfFirst { step ->
            step.descriptor.phaseIndex == phaseNumber
        }
        if (phaseStartIndex != -1) {
            loadActiveStep(phaseStartIndex)
        }
    }

    fun jumpToSubPhaseActive(phaseNumber: Int, subPhaseNumber: Int) {
        if (activeSession.isEmpty()) return
        val subPhaseStartIndex = activeSession.indexOfFirst { step ->
            step.descriptor.phaseIndex == phaseNumber &&
                    step.descriptor.subPhaseIndex == subPhaseNumber
        }
        if (subPhaseStartIndex != -1) {
            loadActiveStep(subPhaseStartIndex)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        playbackJob?.cancel()
        completionCheckJob?.cancel()
        audioPlayer.release()
        ttsPlayer.release()
        if (audioInputEnabled) {
            inputDetector.stopAudioListening()
        }
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