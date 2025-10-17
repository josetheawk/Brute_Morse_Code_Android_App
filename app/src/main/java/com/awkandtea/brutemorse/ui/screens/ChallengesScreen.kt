package com.awkandtea.brutemorse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awkandtea.brutemorse.model.UserSettings
import com.awkandtea.brutemorse.model.MorseDefinitions
import kotlinx.coroutines.delay

@Composable
fun ChallengesScreen(
    onNavigateHome: () -> Unit,
    onKeyDown: () -> Unit,
    onKeyUp: () -> Unit,
    onPlayback: (String) -> Unit,
    onStopPlayback: () -> Unit,
    settingsState: UserSettings
) {
    var isKeyPressed by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    var currentLetter by remember { mutableStateOf("") }
    var decodedInput by remember { mutableStateOf("") }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var isComplete by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Sample challenges - you can expand this
    val challenges = listOf(
        "CQ CQ DE N0CALL K",
        "UR RST 599 K",
        "73 SK",
        "QTH TEXAS K"
    )
    var currentChallengeIndex by remember { mutableStateOf(0) }
    val currentChallenge = challenges[currentChallengeIndex]

    // Stop playback when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            onStopPlayback()
        }
    }

    // Helper to decode morse to character
    fun decodeMorse(pattern: String): String {
        val standardPattern = pattern.replace("•", ".").replace("—", "-")
        return MorseDefinitions.morseMap.entries
            .firstOrNull { it.value == standardPattern }?.key ?: "?"
    }

    // Helper function to convert text to morse pattern
    fun textToMorse(text: String): String {
        return text.map { char ->
            when {
                char == ' ' -> "   " // Word space
                char.isWhitespace() -> " "
                else -> MorseDefinitions.morseMap[char.toString()] ?: ""
            }
        }.joinToString(" ") // Letter space between characters
    }

    val ditUnit = (1200f / settingsState.wpm).toLong()
    val letterGap = ditUnit * 3
    val wordGap = ditUnit * 7

    // Check for gaps to add spaces
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            val now = System.currentTimeMillis()
            val timeSinceLastTap = now - lastTapTime

            if (lastTapTime > 0 && currentLetter.isNotEmpty() && !isComplete) {
                when {
                    timeSinceLastTap > wordGap -> {
                        val decoded = decodeMorse(currentLetter)
                        userInput += currentLetter + "   "
                        decodedInput += decoded + "   "
                        currentLetter = ""
                        lastTapTime = 0
                    }
                    timeSinceLastTap > letterGap -> {
                        val decoded = decodeMorse(currentLetter)
                        userInput += currentLetter + " "
                        decodedInput += decoded + " "
                        currentLetter = ""
                        lastTapTime = 0
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateHome) {
                Icon(imageVector = Icons.Filled.Home, contentDescription = "Home")
            }
            Text("Challenges", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(0.1f))
        }

        // Challenge card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Type this:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentChallenge,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Challenge ${currentChallengeIndex + 1} of ${challenges.size}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // User input display - shows decoded text above morse
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Your input:",
                    style = MaterialTheme.typography.labelLarge
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Decoded text (letters/numbers)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (decodedInput.isEmpty() && currentLetter.isEmpty())
                                ""
                            else {
                                val currentDecoded = if (currentLetter.isNotEmpty())
                                    decodeMorse(currentLetter)
                                else ""
                                decodedInput + currentDecoded
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Morse code symbols
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userInput.isEmpty() && currentLetter.isEmpty())
                                "Start typing..."
                            else
                                userInput + currentLetter,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (!isComplete) {
            // Key button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        if (isKeyPressed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isKeyPressed = true
                                onKeyDown()
                                val startTime = System.currentTimeMillis()
                                tryAwaitRelease()
                                val duration = System.currentTimeMillis() - startTime
                                isKeyPressed = false
                                onKeyUp()

                                val ditMax = ditUnit * 2.0
                                val symbol = if (duration < ditMax) "•" else "—"
                                currentLetter += symbol
                                lastTapTime = System.currentTimeMillis()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isKeyPressed) "SENDING..." else "TAP & HOLD",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isKeyPressed)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isComplete = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done - Check")
                }

                Button(
                    onClick = {
                        userInput = ""
                        currentLetter = ""
                        decodedInput = ""
                        lastTapTime = 0
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
        } else {
            // Results section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Expected: $currentChallenge",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "You typed: ${userInput.trim()}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val match = userInput.trim() == currentChallenge
                    Text(
                        text = if (match) "✓ Perfect!" else "✗ Not quite",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (match)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            // Playback buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Play user's attempt - convert display symbols to morse
                        val morsePattern = userInput
                            .replace("•", ".")
                            .replace("—", "-")
                        onPlayback(morsePattern)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play Mine")
                }

                OutlinedButton(
                    onClick = {
                        // Play correct version - convert text to morse
                        val morsePattern = textToMorse(currentChallenge)
                        onPlayback(morsePattern)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play Correct")
                }
            }

            // Stop button
            OutlinedButton(
                onClick = onStopPlayback,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Playback")
            }

            // Next challenge button
            Button(
                onClick = {
                    currentChallengeIndex = (currentChallengeIndex + 1) % challenges.size
                    userInput = ""
                    currentLetter = ""
                    decodedInput = ""
                    isComplete = false
                    lastTapTime = 0
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next Challenge")
            }
        }
    }
}