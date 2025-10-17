package com.awkandtea.brutemorse.model

import com.awkandtea.brutemorse.model.MorseSymbols
import kotlinx.serialization.Serializable

enum class SegmentType {
    MORSE_TONE,
    TEXT_TO_SPEECH,
    CHIME,
    SILENCE
}

@Serializable
sealed interface PlaybackElement {
    val durationMillis: Long
}

@Serializable
data class MorseElement(
    val symbol: String,
    val character: String,
    val wpm: Int,
    val toneFrequencyHz: Int,
    override val durationMillis: Long
) : PlaybackElement

@Serializable
data class SpeechElement(
    val text: String,
    override val durationMillis: Long
) : PlaybackElement

@Serializable
data class SilenceElement(
    override val durationMillis: Long
) : PlaybackElement

@Serializable
data class ChimeElement(
    override val durationMillis: Long = 1200L
) : PlaybackElement

@Serializable
data class PhaseDescriptor(
    val phaseIndex: Int,
    val subPhaseIndex: Int,
    val title: String,
    val description: String
)

@Serializable
data class SessionStep(
    val descriptor: PhaseDescriptor,
    val passIndex: Int,
    val passCount: Int,
    val elementIndex: Int,
    val elements: List<PlaybackElement>
)

@Serializable
data class ScenarioScript(
    val id: String,
    val title: String,
    val category: ScenarioCategory,
    val lines: List<String>
)

enum class ScenarioCategory { NORMAL, SKYWARN, APOCALYPSE }

/**
 * User settings data class with repetition counts for active recall training.
 * Each phase can have different repetition settings based on learning theory.
 */
data class UserSettings(
    val callSign: String = "",
    val friendCallSigns: List<String> = emptyList(),
    val wpm: Int = DEFAULT_WPM,
    val toneFrequencyHz: Int = DEFAULT_TONE_HZ,
    val phaseSelection: Set<Int> = setOf(1, 2, 3, 4),

    // Phase 1: Alphabet Learning
    val repetitionPhase1NestedID: Int = DEFAULT_REPETITION_LETTERS,  // Phase 1.1: Individual letters (A, AB, ABC...)
    val repetitionPhase1BCT: Int = DEFAULT_REPETITION_BCT,           // Phase 1.2: BCT traversal

    // Phase 2: Numbers + Mixed
    val repetitionPhase2NestedID: Int = DEFAULT_REPETITION_NUMBERS,  // Phase 2.1: Numbers (0, 01, 012...)
    val repetitionPhase2BCT: Int = DEFAULT_REPETITION_BCT_MIX,       // Phase 2.2: Letters + Numbers mixed

    // Phase 3: Vocabulary
    val repetitionPhase3Vocab: Int = DEFAULT_REPETITION_VOCAB        // Phase 3.1: Words (CQ, QTH, RST...)
) {
    // Centralized timing configuration - computed from WPM
    val timing: com.awkandtea.brutemorse.data.MorseTimingConfig
        get() = com.awkandtea.brutemorse.data.MorseTimingConfig(wpm)

    companion object {
        // Default values
        const val DEFAULT_WPM = 25
        const val DEFAULT_TONE_HZ = 800

        // Active recall repetition defaults
        const val DEFAULT_REPETITION_LETTERS = 3    // Letters: 3 reps (easier, familiar)
        const val DEFAULT_REPETITION_BCT = 3        // BCT traversal: 3 reps
        const val DEFAULT_REPETITION_NUMBERS = 3    // Numbers: 3 reps
        const val DEFAULT_REPETITION_BCT_MIX = 3    // Mixed set: 3 reps
        const val DEFAULT_REPETITION_VOCAB = 3      // Vocabulary: 3 reps (whole words)
    }
}