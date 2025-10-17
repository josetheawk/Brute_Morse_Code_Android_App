package com.awkandtea.brutemorse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.awkandtea.brutemorse.data.SessionRepository
import com.awkandtea.brutemorse.data.SettingsRepository
import com.awkandtea.brutemorse.audio.TextToSpeechPlayer
import com.awkandtea.brutemorse.audio.MorseInputDetector
import com.awkandtea.brutemorse.audio.MorseInputEvent
import com.awkandtea.brutemorse.audio.MorseAudioPlayer
import com.awkandtea.brutemorse.model.MorseElement
import com.awkandtea.brutemorse.model.SpeechElement
import com.awkandtea.brutemorse.model.UserSettings
import com.awkandtea.brutemorse.model.SessionStep
import com.awkandtea.brutemorse.model.ScenarioScript
import com.awkandtea.brutemorse.model.MorseDefinitions
import com.awkandtea.brutemorse.ui.screens.KeyerTestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.awkandtea.brutemorse.model.MorseSymbols
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

    private val _keyerTestState = MutableStateFlow(KeyerTestState())
    val keyerTestState: StateFlow<KeyerTestState> = _keyerTestState.asStateFlow()

    val scenarios: List<ScenarioScript> = sessionRepository.scenarios

    private var session: List<SessionStep> = emptyList()
    private var tickerJob: Job? = null
    private var playbackJob: Job? = null
    private var morsePlaybackJob: Job? = null
    private var stepOffsets: List<Long> = emptyList()
    private val audioPlayer = MorseAudioPlayer(context)
    private val ttsPlayer = TextToSpeechPlayer(context)

    // Add separate audio player for Active mode feedback
    private val activeAudioPlayer = MorseAudioPlayer(context)

    private var inputDetector = MorseInputDetector(wpm = 25)
    private var activeSession: List<SessionStep> = emptyList()
    private var currentStepIndex = 0
    private var completionCheckJob: Job? = null
    private var audioInputEnabled = false

    // Keyer test
    private var testInputDetector: MorseInputDetector? = null
    private var testMonitorJob: Job? = null

    // Store completed results history
    private data class CompletedSet(
        val stepIndex: Int,
        val tokens: List<String>,
        val attempts: List<CharacterAttempt>,
        val score: Pair<Int, Int>,
        val descriptor: com.awkandtea.brutemorse.model.PhaseDescriptor
    )
    private val completedSets = mutableListOf<CompletedSet>()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _settingsState.value = settings
            }
        }

        // Restore session on app restart
        viewModelScope.launch {
            val savedIndex = settingsRepository.getLastPlaybackIndex()
            if (savedIndex >= 0) {
                // Generate session and restore position
                val settings = settingsState.value
                session = sessionRepository.generateSession(settings)
                val offsets = mutableListOf<Long>()
                var running = 0L
                session.forEach { step ->
                    offsets += running
                    running += step.elements.sumOf { it.durationMillis }
                }
                stepOffsets = offsets

                if (session.isNotEmpty() && savedIndex < session.size) {
                    val restoredElapsed = stepOffsets.getOrNull(savedIndex) ?: 0L
                    _uiState.value = PlaybackUiState(
                        isPlaying = false,
                        currentStep = session.getOrNull(savedIndex),
                        currentIndex = savedIndex,
                        totalSteps = session.size,
                        elapsedMillis = restoredElapsed,
                        totalMillis = running
                    )
                }
            }
        }

        // Restore active session on app restart
        viewModelScope.launch {
            val savedActiveIndex = settingsRepository.getLastActiveIndex()
            if (savedActiveIndex >= 0) {
                val settings = settingsState.value
                activeSession = sessionRepository.generateSession(settings)

                if (activeSession.isNotEmpty() && savedActiveIndex < activeSession.size) {
                    currentStepIndex = savedActiveIndex
                    loadActiveStep(savedActiveIndex, isResume = true)
                }
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
                settingsRepository.saveLastPlaybackIndex(-1) // Clear saved position
                return@launch
            }
            _uiState.value = PlaybackUiState(
                isPlaying = false,  // Start paused so user can press play/resume
                currentStep = session.firstOrNull(),
                currentIndex = 0,
                totalSteps = session.size,
                elapsedMillis = 0L,
                totalMillis = running
            )
            settingsRepository.saveLastPlaybackIndex(0) // Save new starting position
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

    fun restartSubPhase() {
        if (session.isEmpty()) return
        val currentStep = _uiState.value.currentStep ?: return
        val subPhaseStartIndex = session.indexOfFirst { step ->
            step.descriptor.phaseIndex == currentStep.descriptor.phaseIndex &&
                    step.descriptor.subPhaseIndex == currentStep.descriptor.subPhaseIndex
        }
        if (subPhaseStartIndex != -1) {
            updateIndex(subPhaseStartIndex)
            if (_uiState.value.isPlaying) {
                startPlayback()
            }
        }
    }

    fun seekToTime(targetMillis: Long) {
        if (session.isEmpty() || stepOffsets.isEmpty()) return

        // Find the step that contains this time
        val targetIndex = stepOffsets.indexOfLast { offset -> offset <= targetMillis }
            .coerceAtLeast(0)

        updateIndex(targetIndex)
        if (_uiState.value.isPlaying) {
            startPlayback()
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

    fun playMorsePattern(pattern: String) {
        // Cancel any existing playback first
        morsePlaybackJob?.cancel()

        morsePlaybackJob = viewModelScope.launch {
            try {
                val settings = _settingsState.value
                audioPlayer.playMorsePattern(
                    pattern = pattern,
                    frequencyHz = settings.toneFrequencyHz,
                    wpm = settings.wpm
                )
            } catch (e: Exception) {
                android.util.Log.e("PlaybackViewModel", "Error playing morse pattern", e)
            }
        }
    }

    fun stopMorsePlayback() {
        morsePlaybackJob?.cancel()
        morsePlaybackJob = null
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
        // Save position for app restart
        viewModelScope.launch {
            settingsRepository.saveLastPlaybackIndex(index)
        }
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        tickerJob?.cancel()

        // Reset elapsed time to match current step's start position
        val currentStepOffset = stepOffsets.getOrNull(_uiState.value.currentIndex) ?: 0L
        _uiState.value = _uiState.value.copy(elapsedMillis = currentStepOffset)

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

        playbackJob = viewModelScope.launch {
            var currentIndex = _uiState.value.currentIndex

            while (currentIndex < session.size && _uiState.value.isPlaying) {
                val step = session[currentIndex]

                for (elementIndex in step.elements.indices) {
                    if (!_uiState.value.isPlaying) break

                    val element = step.elements[elementIndex]

                    _uiState.value = _uiState.value.copy(
                        currentIndex = currentIndex,
                        currentStep = step.copy(
                            elements = listOf(element)
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

                currentIndex++
                // Save position as we progress
                if (currentIndex < session.size) {
                    settingsRepository.saveLastPlaybackIndex(currentIndex)
                }
            }

            if (_uiState.value.isPlaying) {
                _uiState.value = _uiState.value.copy(isPlaying = false)
            }
        }
    }

    fun generateActiveSession() {
        viewModelScope.launch {
            val settings = settingsState.value
            activeSession = sessionRepository.generateSession(settings)
            currentStepIndex = 0
            completedSets.clear()

            if (activeSession.isEmpty()) {
                _activeState.value = ActiveUiState()
                settingsRepository.saveLastActiveIndex(-1)
                return@launch
            }

            loadActiveStep(0, isResume = false)
            settingsRepository.saveLastActiveIndex(0)
        }
    }

    fun enableAudioInput() {
        try {
            audioInputEnabled = true
            inputDetector.startAudioListening()
        } catch (e: Exception) {
            e.printStackTrace()
            audioInputEnabled = false
        }
    }

    private fun loadActiveStep(index: Int, isResume: Boolean = false) {
        if (index >= activeSession.size) {
            _activeState.value = ActiveUiState()
            if (audioInputEnabled) {
                inputDetector.stopAudioListening()
            }
            viewModelScope.launch {
                settingsRepository.saveLastActiveIndex(-1)
            }
            return
        }

        val step = activeSession[index]
        currentStepIndex = index

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
            totalPasses = step.passCount,
            stepIndex = index,
            totalSteps = activeSession.size
        )

        if (!isResume) {
            startCompletionChecker()
        }

        viewModelScope.launch {
            settingsRepository.saveLastActiveIndex(index)
        }
    }

    fun seekToActiveStep(targetStep: Int) {
        if (activeSession.isEmpty()) return
        val clampedStep = targetStep.coerceIn(0, activeSession.size - 1)
        loadActiveStep(clampedStep, isResume = false)
    }

    fun restartActiveSubPhase() {
        if (activeSession.isEmpty()) {
            // If no active session exists, generate one
            generateActiveSession()
            return
        }

        val currentStep = activeSession.getOrNull(currentStepIndex) ?: return
        val subPhaseStartIndex = activeSession.indexOfFirst { step ->
            step.descriptor.phaseIndex == currentStep.descriptor.phaseIndex &&
                    step.descriptor.subPhaseIndex == currentStep.descriptor.subPhaseIndex
        }
        if (subPhaseStartIndex != -1) {
            loadActiveStep(subPhaseStartIndex, isResume = false)
        }
    }

    fun onActiveKeyDown() {
        inputDetector.onKeyDown()

        // Start playing continuous tone when key is pressed
        val settings = _settingsState.value
        activeAudioPlayer.startContinuousTone(settings.toneFrequencyHz)
    }

    fun onActiveKeyUp() {
        inputDetector.onKeyUp()

        // Stop the tone immediately when key is released
        activeAudioPlayer.stopContinuousTone()

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
                    val isSingleLetterDrill = state.currentTokens.size <= 3 &&
                            state.currentTokens.all { it.length == 1 }

                    val timeoutMs = if (isSingleLetterDrill) 800L else 2500L

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

        val expectedDisplay = expectedPattern.replace(".", MorseSymbols.DIT_DISPLAY).replace("-", MorseSymbols.DAH_DISPLAY)

        val attempt = CharacterAttempt(
            expectedChar = expectedChar,
            expectedPattern = expectedDisplay,
            userPattern = event.pattern,
            isCorrect = event.pattern == expectedDisplay
        )

        val newAttempts = state.attempts + attempt
        val newPosition = state.currentPosition + 1

        if (newPosition >= state.currentTokens.size) {
            val correct = newAttempts.count { it.isCorrect }
            val total = newAttempts.size

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
            _activeState.value = state.copy(
                currentPosition = newPosition,
                currentInput = "",
                attempts = newAttempts
            )
            inputDetector.reset()
        }
    }

    fun onActiveNextSet() {
        loadActiveStep(currentStepIndex + 1, isResume = false)
    }

    fun onActiveBackToResults() {
        if (completedSets.size >= 2) {
            completedSets.removeLastOrNull()

            val previousSet = completedSets.lastOrNull()
            if (previousSet != null) {
                currentStepIndex = previousSet.stepIndex
                _activeState.value = ActiveUiState(
                    currentTokens = previousSet.tokens,
                    currentPosition = previousSet.tokens.size,
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
    }

    fun jumpToPhaseActive(phaseNumber: Int) {
        if (activeSession.isEmpty()) return
        val phaseStartIndex = activeSession.indexOfFirst { step ->
            step.descriptor.phaseIndex == phaseNumber
        }
        if (phaseStartIndex != -1) {
            loadActiveStep(phaseStartIndex, isResume = false)
        }
    }

    fun jumpToSubPhaseActive(phaseNumber: Int, subPhaseNumber: Int) {
        if (activeSession.isEmpty()) return
        val subPhaseStartIndex = activeSession.indexOfFirst { step ->
            step.descriptor.phaseIndex == phaseNumber &&
                    step.descriptor.subPhaseIndex == subPhaseNumber
        }
        if (subPhaseStartIndex != -1) {
            loadActiveStep(subPhaseStartIndex, isResume = false)
        }
    }

    fun startKeyerTest(sensitivity: Float) {
        testInputDetector?.stopAudioListening()
        testInputDetector = MorseInputDetector(wpm = 25).apply {
            updateSensitivity(sensitivity)
        }

        _keyerTestState.value = KeyerTestState(isListening = true)

        try {
            testInputDetector?.startAudioListening()
            startKeyerTestMonitoring()
        } catch (e: Exception) {
            android.util.Log.e("PlaybackViewModel", "Failed to start keyer test", e)
            _keyerTestState.value = KeyerTestState(isListening = false)
        }
    }

    fun stopKeyerTest() {
        testMonitorJob?.cancel()
        testInputDetector?.stopAudioListening()
        testInputDetector = null
        _keyerTestState.value = KeyerTestState(isListening = false)
    }

    private fun startKeyerTestMonitoring() {
        testMonitorJob?.cancel()
        testMonitorJob = viewModelScope.launch {
            var detectionCount = 0
            while (_keyerTestState.value.isListening) {
                delay(50)

                val detector = testInputDetector ?: break
                val audioMetrics = detector.getAudioMetrics()

                _keyerTestState.value = _keyerTestState.value.copy(
                    currentRMS = audioMetrics.currentRMS,
                    noiseFloor = audioMetrics.noiseFloor,
                    threshold = audioMetrics.threshold,
                    isKeyDown = audioMetrics.isKeyDown,
                    currentInput = detector.currentInput.value
                )

                val event = detector.checkCompletion(completionTimeoutMs = 800L)
                if (event != null) {
                    detectionCount++
                    _keyerTestState.value = _keyerTestState.value.copy(
                        lastDetection = "${event.pattern} = ${event.character ?: "?"}",
                        detectionCount = detectionCount
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        playbackJob?.cancel()
        morsePlaybackJob?.cancel()
        completionCheckJob?.cancel()
        testMonitorJob?.cancel()
        audioPlayer.release()
        activeAudioPlayer.release()
        ttsPlayer.release()
        if (audioInputEnabled) {
            inputDetector.stopAudioListening()
        }
        testInputDetector?.stopAudioListening()
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
