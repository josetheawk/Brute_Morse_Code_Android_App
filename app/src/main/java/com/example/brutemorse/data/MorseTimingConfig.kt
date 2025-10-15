package com.example.brutemorse.data

/**
 * Centralized Morse code timing configuration based on WPM.
 * All timing values follow standard Morse code conventions.
 *
 * Standard timing:
 * - 1 dit = base unit
 * - 1 dah = 3 dits
 * - Inter-element gap (within character) = 1 dit
 * - Inter-character gap = 3 dits
 * - Inter-word gap = 7 dits
 */
data class MorseTimingConfig(
    val wpm: Int
) {
    // Base unit: 1 dit length in milliseconds
    // Formula: 1200ms per minute / WPM = time for "PARIS " (50 dits)
    val ditMs: Long = (1200f / wpm).toLong()

    // Dah is 3 times a dit
    val dahMs: Long = ditMs * 3

    // Gap between dots and dashes within a character (like in K: -·-)
    val intraCharacterGapMs: Long = ditMs

    // Gap between characters (like between K and C)
    val interCharacterGapMs: Long = ditMs * 3

    // Gap between words
    val interWordGapMs: Long = ditMs * 7

    companion object {
        /**
         * Calculates the total duration of a morse pattern in milliseconds
         */
        fun calculateDuration(pattern: String, config: MorseTimingConfig): Long {
            var duration = 0L

            pattern.forEach { char ->
                duration += when (char) {
                    '.', '·', '•' -> config.ditMs
                    '-', '−', '–', '—' -> config.dahMs
                    ' ' -> config.interCharacterGapMs
                    else -> 0L
                }
                // Add intra-character gap after each element (except spaces)
                if (char != ' ') {
                    duration += config.intraCharacterGapMs
                }
            }

            // Add final inter-character gap
            duration += config.interCharacterGapMs
            return duration
        }
    }
}