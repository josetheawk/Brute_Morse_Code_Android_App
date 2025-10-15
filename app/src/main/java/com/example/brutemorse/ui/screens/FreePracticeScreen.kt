package com.example.brutemorse.ui.screens

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
import com.example.brutemorse.data.UserSettings
import com.example.brutemorse.model.MorseDefinitions
import kotlinx.coroutines.delay

@Composable
fun FreePracticeScreen(
    onNavigateHome: () -> Unit,
    onKeyDown: () -> Unit,
    onKeyUp: () -> Unit,
    onPlayback: (String) -> Unit,
    onStopPlayback: () -> Unit,
    settingsState: UserSettings
) {
    var isKeyPressed by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var currentLetter by remember { mutableStateOf("") }
    var decodedText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val ditUnit = (1200f / settingsState.wpm).toLong()
    val letterGap = ditUnit * 3  // 3 units between letters
    val wordGap = ditUnit * 7    // 7 units between words

    // Helper to decode morse to character
    fun decodeMorse(pattern: String): String {
        val standardPattern = pattern.replace("•", ".").replace("—", "-")
        return MorseDefinitions.morseMap.entries
            .firstOrNull { it.value == standardPattern }?.key ?: "?"
    }

    // Stop playback when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            onStopPlayback()
        }
    }

    // Check for gaps to add spaces
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            val now = System.currentTimeMillis()
            val timeSinceLastTap = now - lastTapTime

            if (lastTapTime > 0 && currentLetter.isNotEmpty()) {
                when {
                    timeSinceLastTap > wordGap -> {
                        // Word gap detected - decode and add
                        val decoded = decodeMorse(currentLetter)
                        typedText += currentLetter + "   "
                        decodedText += decoded + "   "
                        currentLetter = ""
                        lastTapTime = 0
                    }
                    timeSinceLastTap > letterGap -> {
                        // Letter gap detected - decode and add
                        val decoded = decodeMorse(currentLetter)
                        typedText += currentLetter + " "
                        decodedText += decoded + " "
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
            Text("Free Practice", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(0.1f))
        }

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Practice freely at your own pace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pause 3 units for letter space, 7 units for word space",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Speed: ${settingsState.wpm} WPM",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Display area - shows decoded text above morse
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .heightIn(min = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Decoded text (letters/numbers)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (decodedText.isEmpty() && currentLetter.isEmpty())
                            ""
                        else {
                            val currentDecoded = if (currentLetter.isNotEmpty())
                                decodeMorse(currentLetter)
                            else ""
                            decodedText + currentDecoded
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
                        .heightIn(min = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (typedText.isEmpty() && currentLetter.isEmpty())
                            "Start tapping..."
                        else
                            typedText + currentLetter,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Key button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
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

                            // Determine dit or dah based on duration
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
                text = if (isKeyPressed) "SENDING..." else "TAP & HOLD TO SEND",
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
                    // Convert display symbols back to standard morse and play
                    val fullText = typedText + currentLetter
                    if (fullText.isNotEmpty()) {
                        val morsePattern = fullText
                            .replace("•", ".")
                            .replace("—", "-")
                        onPlayback(morsePattern)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = (typedText + currentLetter).isNotEmpty()
            ) {
                Text("Replay")
            }

            OutlinedButton(
                onClick = onStopPlayback,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }

            Button(
                onClick = {
                    typedText = ""
                    currentLetter = ""
                    decodedText = ""
                    lastTapTime = 0
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
        }
    }
}