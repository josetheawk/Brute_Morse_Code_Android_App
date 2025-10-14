package com.example.brutemorse.data

import com.example.brutemorse.model.ChimeElement
import com.example.brutemorse.model.MorseDefinitions
import com.example.brutemorse.model.MorseElement
import com.example.brutemorse.model.PhaseDescriptor
import com.example.brutemorse.model.ScenarioCategory
import com.example.brutemorse.model.ScenarioScript
import com.example.brutemorse.model.SessionStep
import com.example.brutemorse.model.SilenceElement
import com.example.brutemorse.model.SpeechElement
import kotlinx.coroutines.flow.first
import kotlin.math.ceil

class SessionRepository(private val settingsRepository: SettingsRepository) {

    val scenarios: List<ScenarioScript> = ScenarioLibrary.defaultScenarios

    suspend fun latestSettings(): UserSettings = settingsRepository.settings.first()

    suspend fun saveSettings(settings: UserSettings) {
        settingsRepository.update(settings)
    }

    suspend fun generateSession(settings: UserSettings): List<SessionStep> {
        val session = mutableListOf<SessionStep>()
        if (settings.phaseSelection.contains(1)) {
            session += generatePhase1(settings, phaseIndex = 1)
        }
        if (settings.phaseSelection.contains(2)) {
            session += generatePhase2(settings, phaseIndex = 2)
        }
        if (settings.phaseSelection.contains(3)) {
            session += generatePhase3(settings, phaseIndex = 3)
        }
        if (settings.phaseSelection.contains(4)) {
            session += generatePhase4(settings, phaseIndex = 4)
        }
        return session
    }

    private fun generatePhase1(settings: UserSettings, phaseIndex: Int): List<SessionStep> {
        val sequence = mutableListOf<SessionStep>()
        val letters = MorseDefinitions.alphabet
        var subPhase = 1

        // Phase 1.1 Nested Incremental
        // Active Recall: repeat each letter 3 times
        val nestedPasses = letters.indices.map { index ->
            letters.subList(0, index + 1).flatMap { letter -> List(3) { letter } }
        }
        nestedPasses.forEachIndexed { passIndex, pass ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "NestedID", "Forward incremental alphabet"),
                passIndex = passIndex,
                passCount = nestedPasses.size,
                tokens = pass
            )
        }

        // Phase 1.2 BCT traversal
        subPhase++
        val bctSequence = bctTraversal(center = letters.size / 2, size = letters.size, direction = 1, coprime = 5)
        bctSequence.forEachIndexed { index, letterIndex ->
            val letter = listOf(letters[letterIndex])
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "BCT", "Balanced coprime traversal"),
                passIndex = index,
                passCount = bctSequence.size,
                tokens = letter,
                voiceMultiplier = (index % 4) + 1
            )
        }

        // Phase 1.3 Digraph progressive build
        subPhase++
        MorseDefinitions.digraphs.forEachIndexed { passIndex, digraph ->
            val builds = digraph.indices.map { idx -> digraph.substring(0, idx + 1) }
            builds.forEachIndexed { buildIndex, token ->
                sequence += createSessionStep(
                    settings = settings,
                    descriptor = PhaseDescriptor(phaseIndex, subPhase, "Progressive build", "Common digraph construction"),
                    passIndex = passIndex,
                    passCount = MorseDefinitions.digraphs.size,
                    tokens = listOf(token),
                    elementIndex = buildIndex
                )
            }
        }

        // Phase 1.4 Tongue twisters (confusable sets)
        subPhase++
        val confusionSets = listOf(
            listOf("E", "I", "S", "H"),
            listOf("T", "M", "O"),
            listOf("B", "D", "G", "K"),
            listOf("U", "V", "W"),
            listOf("F", "L", "P"),
            listOf("N", "R", "X"),
            listOf("C", "Y", "Q", "Z"),
            listOf("A", "N", "R", "W")
        )
        confusionSets.forEachIndexed { passIndex, set ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "TongueTwister", "Confusable letters"),
                passIndex = passIndex,
                passCount = confusionSets.size,
                tokens = set
            )
        }

        return sequence
    }

    private fun generatePhase2(settings: UserSettings, phaseIndex: Int): List<SessionStep> {
        val sequence = mutableListOf<SessionStep>()
        var subPhase = 1

        val numbers = MorseDefinitions.numbers

        // Phase 2.1 Nested numbers
        val nestedNumbers = numbers.indices.map { index ->
            numbers.subList(0, index + 1).flatMap { number -> List(3) { number } }
        }
        nestedNumbers.forEachIndexed { passIndex, pass ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "NestedID", "Numbers incremental"),
                passIndex = passIndex,
                passCount = nestedNumbers.size,
                tokens = pass
            )
        }

        // Phase 2.2 Full BCT letters + numbers
        subPhase++
        val combined = MorseDefinitions.alphabet + numbers
        val bctSequence = bctTraversal(center = combined.size / 2, size = combined.size, direction = -1, coprime = 7)
        bctSequence.forEachIndexed { index, tokenIndex ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "BCT", "Mixed set traversal"),
                passIndex = index,
                passCount = bctSequence.size,
                tokens = listOf(combined[tokenIndex])
            )
        }

        // Phase 2.3 Number-letter confusion
        subPhase++
        val confusionSets = listOf(
            listOf("5", "S", "H"),
            listOf("1", "T", "J"),
            listOf("0", "O", "M"),
            listOf("6", "B"),
            listOf("9", "N"),
            listOf("2", "I", "U"),
            listOf("3", "V", "S"),
            listOf("4", "V"),
            listOf("7", "G", "Z"),
            listOf("8", "D", "B")
        )
        confusionSets.forEachIndexed { passIndex, set ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "TongueTwister", "Number-letter confusion"),
                passIndex = passIndex,
                passCount = confusionSets.size,
                tokens = set
            )
        }

        // Phase 2.4 Number sequences
        subPhase++
        val sequences = listOf(
            listOf("5", "9", "9"),
            listOf("1", "4", ".", "0", "6", "0"),
            listOf("7", ".", "0", "3", "0"),
            listOf("2", "8", ".", "3", "0", "0"),
            listOf("1", "4", "5", ".", "5", "0")
        )
        sequences.forEachIndexed { passIndex, set ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "Progressive build", "Number sequences"),
                passIndex = passIndex,
                passCount = sequences.size,
                tokens = set
            )
        }

        return sequence
    }

    private fun generatePhase3(settings: UserSettings, phaseIndex: Int): List<SessionStep> {
        val sequence = mutableListOf<SessionStep>()
        var subPhase = 1

        // Phase 3.1 Vocabulary introduction
        MorseDefinitions.hamVocabulary.forEachIndexed { passIndex, token ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "Vocabulary", "Ham terms"),
                passIndex = passIndex,
                passCount = MorseDefinitions.hamVocabulary.size,
                tokens = listOf(token)
            )
        }

        // Phase 3.2 Q-code tongue twisters
        subPhase++
        val qcodes = listOf(
            listOf("CQ", "QRZ", "QTH"),
            listOf("RST", "599", "DE"),
            listOf("WX", "QSL", "QSB"),
            listOf("QRM", "QRP", "QSY")
        )
        qcodes.forEachIndexed { passIndex, set ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "TongueTwister", "Q-code drills"),
                passIndex = passIndex,
                passCount = qcodes.size,
                tokens = set
            )
        }

        // Phase 3.3 Reduced vocal support
        subPhase++
        val reducedSupportTerms = MorseDefinitions.hamVocabulary.shuffled().take(12)
        reducedSupportTerms.forEachIndexed { passIndex, token ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "Reduced vocal", "Sparse TTS"),
                passIndex = passIndex,
                passCount = reducedSupportTerms.size,
                tokens = listOf(token),
                voiceMultiplier = if (passIndex % 2 == 0) 2 else 0
            )
        }

        // Phase 3.4 Mixed confusion sets
        subPhase++
        val mixedSets = listOf(
            listOf("CQ", "DE", "K"),
            listOf("SOS", "599", "RST"),
            listOf("73", "88", "SK"),
            listOf("CQ", "QRZ", "QTH"),
            listOf("DE", "K", "AR")
        )
        mixedSets.forEachIndexed { passIndex, set ->
            sequence += createSessionStep(
                settings = settings,
                descriptor = PhaseDescriptor(phaseIndex, subPhase, "Mixed confusion", "Morse + prosigns"),
                passIndex = passIndex,
                passCount = mixedSets.size,
                tokens = set
            )
        }

        return sequence
    }

    private fun generatePhase4(settings: UserSettings, phaseIndex: Int): List<SessionStep> {
        val sequence = mutableListOf<SessionStep>()
        var subPhase = 0
        var scriptCounter = 0

        // Phase 4.0 Vocabulary review
        sequence += createSessionStep(
            settings = settings,
            descriptor = PhaseDescriptor(phaseIndex, subPhase, "Vocabulary review", "Morse majority"),
            passIndex = 0,
            passCount = 1,
            tokens = MorseDefinitions.hamVocabulary,
            voiceMultiplier = 1
        )

        // Scenario playback sections
        ScenarioLibrary.defaultScenarios.groupBy { it.category }.toSortedMap(compareBy { it.ordinal }).forEach { (category, scripts) ->
            scripts.forEachIndexed { index, script ->
                subPhase++
                val resolvedLines = script.lines.map { line ->
                    replaceScenarioTokens(line, settings, scriptCounter++)
                }
                val tokens = resolvedLines.flatMap { line ->
                    line.split(' ').filter { it.isNotBlank() }
                }
                sequence += createSessionStep(
                    settings = settings,
                    descriptor = PhaseDescriptor(
                        phaseIndex = phaseIndex,
                        subPhaseIndex = subPhase,
                        title = when (category) {
                            ScenarioCategory.NORMAL -> "Normal QSO"
                            ScenarioCategory.SKYWARN -> "SKYWARN"
                            ScenarioCategory.APOCALYPSE -> "Apocalypse"
                        },
                        description = script.title
                    ),
                    passIndex = index,
                    passCount = scripts.size,
                    tokens = tokens
                )
            }
        }

        return sequence
    }

    private fun createSessionStep(
        settings: UserSettings,
        descriptor: PhaseDescriptor,
        passIndex: Int,
        passCount: Int,
        tokens: List<String>,
        voiceMultiplier: Int = 1,
        elementIndex: Int = 0
    ): SessionStep {
        val playback = mutableListOf<com.example.brutemorse.model.PlaybackElement>()
        val baseUnit = (1200f / settings.wpm).toLong()

        tokens.forEachIndexed { index, token ->
            val morse = MorseDefinitions.morseMap[token] ?: token.toCharArray().joinToString(" ") { char ->
                MorseDefinitions.morseMap[char.toString()].orEmpty()
            }
            val normalized = if (morse.isBlank()) "·" else morse
            val duration = morseDurationMillis(normalized, baseUnit)
            playback += MorseElement(
                symbol = normalized,
                character = token,
                wpm = settings.wpm,
                toneFrequencyHz = settings.toneFrequencyHz,
                durationMillis = duration
            )
            if (voiceMultiplier > 0 && index % voiceMultiplier == 0) {
                playback += SpeechElement(token, durationMillis = 750L)
            } else if (voiceMultiplier == 0) {
                // no speech element
            }
            playback += SilenceElement(durationMillis = baseUnit * 3)
        }
        playback += ChimeElement()

        return SessionStep(
            descriptor = descriptor,
            passIndex = passIndex,
            passCount = passCount,
            elementIndex = elementIndex,
            elements = playback
        )
    }

    private fun morseDurationMillis(pattern: String, unit: Long): Long {
        var duration = 0L
        pattern.forEach { char ->
            duration += when (char) {
                '.' -> unit
                '-' -> unit * 3
                else -> unit
            }
            duration += unit // intra-character gap
        }
        return duration + unit * 2
    }

    private fun bctTraversal(center: Int, size: Int, direction: Int, coprime: Int): List<Int> {
        val sequence = mutableListOf<Int>()
        for (i in 0 until size) {
            val offset = ceil(i / 2.0).toInt()
            val sign = if ((i + 1) % 2 == 0) -1 else 1
            val position = (center + direction * coprime * sign * offset).mod(size)
            sequence += position
        }
        return sequence
    }

    private fun replaceScenarioTokens(template: String, settings: UserSettings, order: Int): String {
        val callSign = settings.callSign.ifBlank { "N0CALL" }
        val friendList = settings.friendCallSigns.ifEmpty { listOf("W1AW", "K7QRP", "N5XYZ") }
        val friendCall = friendList[order % friendList.size]
        val qth = qthPool[order % qthPool.size]
        val signal = signalPool[order % signalPool.size]
        val weather = weatherPool[order % weatherPool.size]
        val freq = frequencyPool[order % frequencyPool.size]
        val rig = rigPool[order % rigPool.size]
        val power = powerPool[order % powerPool.size]

        val replacements = mapOf(
            "{MY_CALL}" to callSign,
            "{FRIEND_CALL}" to friendCall,
            "{MY_QTH}" to qth,
            "{SIGNAL}" to signal,
            "{WX}" to weather,
            "{FREQ}" to freq,
            "{RIG}" to rig,
            "{PWR}" to power
        )
        var result = template
        replacements.forEach { (token, value) ->
            result = result.replace(token, value)
        }
        return result
    }

    companion object {
        private val qthPool = listOf("Austin TX", "Seattle WA", "Denver CO", "Boston MA", "Tucson AZ")
        private val signalPool = listOf("599", "579", "559", "449", "339")
        private val weatherPool = listOf("SUNNY", "CLOUDY", "RAIN", "SNOW", "WINDY")
        private val frequencyPool = listOf("14.060", "7.030", "21.060", "10.110", "18.085")
        private val rigPool = listOf("ICOM IC-7300", "YAESU FT-991", "KENWOOD TS-590", "ELECRAFT K3", "YAESU FT-818")
        private val powerPool = listOf("5W", "25W", "50W", "100W", "QRP")
    }
}

private object ScenarioLibrary {
    val defaultScenarios = listOf(
        ScenarioScript(
            id = "normal1",
            title = "CQ Call and Contest Exchange",
            category = ScenarioCategory.NORMAL,
            lines = listOf(
                "CQ CQ CQ DE {MY_CALL} {MY_CALL} K",
                "{MY_CALL} DE {FRIEND_CALL} UR {SIGNAL} QTH {MY_QTH} K",
                "{FRIEND_CALL} DE {MY_CALL} R R UR {SIGNAL} QTH TEXAS WX {WX} RIG {RIG} K",
                "{MY_CALL} DE {FRIEND_CALL} R R FB OM PWR {PWR} 73 SK"
            )
        ),
        ScenarioScript(
            id = "normal2",
            title = "Technical Rig Discussion",
            category = ScenarioCategory.NORMAL,
            lines = listOf(
                "CQ CQ DE {MY_CALL} K",
                "{MY_CALL} DE {FRIEND_CALL} RIG {RIG} ANT DIPOLE",
                "{FRIEND_CALL} DE {MY_CALL} FB HW CPY {PWR} W",
                "{MY_CALL} DE {FRIEND_CALL} R R 73 SK"
            )
        ),
        ScenarioScript(
            id = "skywarn1",
            title = "Tornado Spotted",
            category = ScenarioCategory.SKYWARN,
            lines = listOf(
                "NET CONTROL DE {MY_CALL} TORNADO SPOTTED 5 MILES WEST OF {MY_QTH} MOVING NORTHEAST K",
                "{MY_CALL} DE NET CONTROL ROGER RELAY TO NWS CONTINUE REPORTS K"
            )
        ),
        ScenarioScript(
            id = "apocalypse1",
            title = "Grid Down Supply Check",
            category = ScenarioCategory.APOCALYPSE,
            lines = listOf(
                "ANY STATION DE {MY_CALL} EMERGENCY TRAFFIC GRID DOWN NEED SUPPLY INFO K",
                "{MY_CALL} DE {FRIEND_CALL} COPY EMERGENCY SAFE ZONE AT {MY_QTH} FOOD WATER AVAILABLE K"
            )
        )
    )
}
