package com.example.brutemorse.model

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
