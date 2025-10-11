package com.example.brutemorse.model

object MorseDefinitions {
    val alphabet = ('A'..'Z').map { it.toString() }
    val numbers = (0..9).map { it.toString() }

    val morseMap = mapOf(
        "A" to ".-",
        "B" to "-...",
        "C" to "-.-.",
        "D" to "-..",
        "E" to ".",
        "F" to "..-.",
        "G" to "--.",
        "H" to "....",
        "I" to "..",
        "J" to ".---",
        "K" to "-.-",
        "L" to ".-..",
        "M" to "--",
        "N" to "-.",
        "O" to "---",
        "P" to ".--.",
        "Q" to "--.-",
        "R" to ".-.",
        "S" to "...",
        "T" to "-",
        "U" to "..-",
        "V" to "...-",
        "W" to ".--",
        "X" to "-..-",
        "Y" to "-.--",
        "Z" to "--..",
        "0" to "-----",
        "1" to ".----",
        "2" to "..---",
        "3" to "...--",
        "4" to "....-",
        "5" to ".....",
        "6" to "-....",
        "7" to "--...",
        "8" to "---..",
        "9" to "----.",
        "." to ".-.-.-"
    )

    val digraphs = listOf("TH", "AN", "ER", "ON", "RE", "ST", "ND", "AT", "EN", "OR", "CQ", "DE")
    val hamVocabulary = listOf(
        "CQ", "DX", "QTH", "QSL", "RST", "73", "88", "SK", "NET", "QRZ", "QRM", "QRP",
        "ANT", "RIG", "WX", "PWR", "HF", "VHF", "UHF", "CW", "SSB"
    )
}
