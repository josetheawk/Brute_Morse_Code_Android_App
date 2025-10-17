package com.awkandtea.brutemorse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awkandtea.brutemorse.viewmodel.ActiveUiState
import com.awkandtea.brutemorse.viewmodel.CharacterAttempt

@Composable
fun ActiveScreen(
    state: ActiveUiState,
    onKeyDown: () -> Unit,
    onKeyUp: () -> Unit,
    onNextSet: () -> Unit,
    onBack: () -> Unit,
    onRestartSubPhase: () -> Unit = {},
    onSeekToStep: (Int) -> Unit = {},
    onOpenSettings: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onEnableAudioInput: () -> Unit = {},
    onJumpToPhase: (Int) -> Unit = {},
    onJumpToSubPhase: (Int, Int) -> Unit = { _, _ -> }
) {
    var isKeyPressed by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .let { base ->
                if (!state.isReviewMode) base.verticalScroll(scrollState) else base
            },
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
            Text("Active Practice", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Open settings")
            }
        }

        if (state.currentTokens.isNotEmpty()) {
            // Phase info card
            val descriptor = state.phaseDescriptor
            if (descriptor != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Phase ${descriptor.phaseIndex}.${descriptor.subPhaseIndex}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = descriptor.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Pass ${state.passIndex + 1} / ${state.totalPasses}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Progress bar
            var isUserSeeking by remember { mutableStateOf(false) }
            var seekValue by remember { mutableFloatStateOf(0f) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Progress: Step ${state.stepIndex + 1} / ${state.totalSteps}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = if (isUserSeeking) {
                        seekValue
                    } else {
                        if (state.totalSteps == 0) 0f
                        else state.stepIndex / state.totalSteps.toFloat()
                    },
                    onValueChange = { value ->
                        isUserSeeking = true
                        seekValue = value
                    },
                    onValueChangeFinished = {
                        isUserSeeking = false
                        val targetStep = (seekValue * state.totalSteps).toInt().coerceIn(0, state.totalSteps - 1)
                        onSeekToStep(targetStep)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!state.isReviewMode) {
                // Prompt section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Send these characters:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.currentTokens.forEachIndexed { index, token ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = token,
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = if (index == state.currentPosition) FontWeight.Bold else FontWeight.Normal,
                                        color = if (index < state.currentPosition) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else if (index == state.currentPosition) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Position: ${state.currentPosition + 1} / ${state.currentTokens.size}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current input display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.currentInput.isEmpty()) "..." else state.currentInput,
                            style = MaterialTheme.typography.displayLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

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
                                    tryAwaitRelease()
                                    isKeyPressed = false
                                    onKeyUp()
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

                // Restart subphase button (not in review mode)
                Button(
                    onClick = onRestartSubPhase,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restart Subphase")
                }
            } else {
                // Results section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Score card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Score: ${state.score.first} / ${state.score.second}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Results list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.attempts) { attempt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (attempt.isCorrect)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = attempt.expectedChar,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (attempt.isCorrect) "✓" else "✗",
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                    Text(
                                        text = "Expected: ${attempt.expectedPattern}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "You sent: ${attempt.userPattern.ifEmpty { "(nothing)" }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (attempt.isCorrect)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Navigation buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = null)
                                Text("Back")
                            }

                            Button(
                                onClick = onNextSet,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Next Set")
                            }
                        }

                        Button(
                            onClick = onRestartSubPhase,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restart Subphase")
                        }
                    }
                }
            }

            // Divider before phase navigation
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Phase navigation
            val phases = listOf(
                1 to "Alphabet Mastery",
                2 to "Expanded Set",
                3 to "Words & Abbreviations",
                4 to "Real World QSOs"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                phases.forEach { (phase, label) ->
                    val isCurrentPhase = phase == (state.phaseDescriptor?.phaseIndex ?: 1)

                    Button(
                        onClick = { onJumpToPhase(phase) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isCurrentPhase) {
                            androidx.compose.material3.ButtonDefaults.buttonColors()
                        } else {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text("Phase $phase: $label")
                    }

                    if (isCurrentPhase) {
                        val subPhases = when (phase) {
                            1 -> listOf(
                                1 to "NestedID Forward",
                                2 to "BCT Traversal",
                                3 to "Digraph Build",
                                4 to "Tongue Twisters"
                            )
                            2 -> listOf(
                                1 to "Nested Numbers",
                                2 to "Full BCT Mix",
                                3 to "Number/Letter Confusion",
                                4 to "Number Sequences"
                            )
                            3 -> listOf(
                                1 to "Ham Vocabulary",
                                2 to "Q-Code Drills",
                                3 to "Reduced Vocal",
                                4 to "Mixed Confusion"
                            )
                            4 -> listOf(
                                0 to "Vocab Review",
                                1 to "Normal QSOs",
                                5 to "SKYWARN",
                                9 to "Apocalypse"
                            )
                            else -> emptyList()
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                subPhases.forEach { (subPhaseNum, subPhaseLabel) ->
                                    val isCurrentSubPhase = subPhaseNum == (state.phaseDescriptor?.subPhaseIndex ?: 1)
                                    Text(
                                        text = "${phase}.${subPhaseNum}: $subPhaseLabel",
                                        style = if (isCurrentSubPhase) {
                                            MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            MaterialTheme.typography.bodySmall
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onJumpToSubPhase(phase, subPhaseNum) }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Press Resume or start to begin active practice",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRestartSubPhase,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resume Practice")
                }
            }
        }
    }
}