package com.awkandtea.brutemorse.model

/**
 * Standardized Morse code symbols used throughout the application.
 *
 * This object defines canonical representations for Morse code elements
 * to ensure consistency across the entire codebase.
 */
object MorseSymbols {
    // Standard Morse code symbols (for morse map and internal processing)
    const val DIT_STANDARD = "."
    const val DAH_STANDARD = "-"

    // Display symbols (prettier for UI)
    const val DIT_DISPLAY = "\u2022"  // • bullet
    const val DAH_DISPLAY = "\u2014"  // — em dash

    // Legacy symbol variations (for backward compatibility during migration)
    // Using Unicode escapes to avoid encoding issues
    val DIT_VARIANTS = listOf(
        ".",           // standard period
        "\u00B7",      // · middle dot
        "\u2022"       // • bullet
    )

    val DAH_VARIANTS = listOf(
        "-",           // standard hyphen-minus
        "\u2212",      // − minus sign
        "\u2013",      // – en dash
        "\u2014"       // — em dash
    )

    // Gap representation
    const val ELEMENT_GAP = " "

    /**
     * Normalizes a morse pattern to use standard symbols.
     * Converts any variant dit/dah to the standard representation.
     */
    fun normalizeToStandard(pattern: String): String {
        var result = pattern
        DIT_VARIANTS.forEach { variant ->
            result = result.replace(variant, DIT_STANDARD)
        }
        DAH_VARIANTS.forEach { variant ->
            result = result.replace(variant, DAH_STANDARD)
        }
        return result
    }

    /**
     * Converts standard morse symbols to display symbols for UI.
     */
    fun toDisplaySymbols(pattern: String): String {
        return pattern
            .replace(DIT_STANDARD, DIT_DISPLAY)
            .replace(DAH_STANDARD, DAH_DISPLAY)
    }

    /**
     * Checks if a character is a valid morse element (dit or dah).
     */
    fun isMorseElement(char: Char): Boolean {
        val str = char.toString()
        return DIT_VARIANTS.contains(str) || DAH_VARIANTS.contains(str)
    }
}