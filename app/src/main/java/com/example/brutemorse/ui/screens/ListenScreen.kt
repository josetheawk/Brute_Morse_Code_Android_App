package com.example.brutemorse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.brutemorse.model.MorseElement
import com.example.brutemorse.model.PlaybackElement
import com.example.brutemorse.model.SpeechElement
import com.example.brutemorse.viewmodel.PlaybackUiState

@Composable
private fun PhaseNavigationButtons(
    currentPhase: Int,
    currentSubPhase: Int,
    onPhaseSelect: (Int) -> Unit,
    onSubPhaseSelect: (Int, Int) -> Unit
) {
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
            val isCurrentPhase = phase == currentPhase

            // Phase button
            Button(
                onClick = { onPhaseSelect(phase) },
                modifier = Modifier.fillMaxWidth(),
                colors = if (isCurrentPhase) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text("Phase $phase: $label")
            }

            // Auto-expand current phase to show subphases
            if (isCurrentPhase) {
                SubPhaseList(
                    phase = phase,
                    currentSubPhase = currentSubPhase,
                    onSubPhaseSelect = onSubPhaseSelect
                )
            }
        }
    }
}

@Composable
private fun SubPhaseList(
    phase: Int,
    currentSubPhase: Int,
    onSubPhaseSelect: (Int, Int) -> Unit
) {
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
                val isCurrentSubPhase = subPhaseNum == currentSubPhase
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
                        .clickable { onSubPhaseSelect(phase, subPhaseNum) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ListenScreen(
    state: PlaybackUiState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipPhase: () -> Unit,
    onRestartSubPhase: () -> Unit = {},
    onSeekToTime: (Long) -> Unit = {},
    onRegenerateSession: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onPauseOnExit: () -> Unit = {},
    onJumpToPhase: (Int) -> Unit = {},
    onJumpToSubPhase: (Int, Int) -> Unit = { _, _ -> }
) {
    val currentStep = state.currentStep
    val scrollState = rememberScrollState()

    // Pause playback when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            onPauseOnExit()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateHome) {
                Icon(imageVector = Icons.Filled.Home, contentDescription = "Home")
            }
            Text("Listen", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Open settings")
            }
        }

        if (currentStep != null) {
            PhaseHeader(state)
            PlaybackVisualizer(currentStep.elements)
            PlaybackDetails(state, onSeekToTime)
        } else {
            EmptyState()
        }

        // Fixed spacer instead of weighted
        Spacer(modifier = Modifier.height(16.dp))

        PlaybackControls(
            isPlaying = state.isPlaying,
            onPlayPause = onPlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSkipPhase = onSkipPhase,
            onRestartSubPhase = onRestartSubPhase,
            onRegenerateSession = onRegenerateSession
        )

        if (currentStep != null) {
            // Divider before phase navigation
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            PhaseNavigationButtons(
                currentPhase = state.currentStep?.descriptor?.phaseIndex ?: 1,
                currentSubPhase = state.currentStep?.descriptor?.subPhaseIndex ?: 1,
                onPhaseSelect = onJumpToPhase,
                onSubPhaseSelect = onJumpToSubPhase
            )
        }
    }
}

@Composable
private fun PhaseHeader(state: PlaybackUiState) {
    val descriptor = state.currentStep?.descriptor ?: return

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Phase ${descriptor.phaseIndex}.${descriptor.subPhaseIndex}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(descriptor.title, style = MaterialTheme.typography.bodyLarge)
            Text(descriptor.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Pass ${state.currentStep?.passIndex?.plus(1)} / ${state.currentStep?.passCount}",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun PlaybackVisualizer(elements: List<PlaybackElement>) {
    val morse = elements.filterIsInstance<MorseElement>().firstOrNull()
    val speech = elements.filterIsInstance<SpeechElement>().firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // During morse: show ONLY morse pattern (with nice symbols)
            // During speech: show ONLY letter
            // Otherwise: blank
            if (morse != null) {
                // Convert old symbols to nice ones
                val displaySymbol = morse.symbol
                    .replace(".", "•")
                    .replace("-", "—")
                    .replace("·", "•")  // In case old symbol is there

                Text(
                    text = displaySymbol,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )
            } else if (speech != null) {
                Text(
                    text = speech.text,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                // Completely blank during silence
                Text(
                    text = "",
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PlaybackDetails(state: PlaybackUiState, onSeekToTime: (Long) -> Unit) {
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Elapsed ${formatDuration(state.elapsedMillis)} / ${formatDuration(state.totalMillis)}",
            style = MaterialTheme.typography.bodyLarge
        )

        // Interactive slider for seeking
        Slider(
            value = if (isUserSeeking) {
                seekValue
            } else {
                if (state.totalMillis == 0L) 0f
                else state.elapsedMillis / state.totalMillis.toFloat()
            },
            onValueChange = { value ->
                isUserSeeking = true
                seekValue = value
            },
            onValueChangeFinished = {
                isUserSeeking = false
                val targetMillis = (seekValue * state.totalMillis).toLong()
                onSeekToTime(targetMillis)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipPhase: () -> Unit,
    onRestartSubPhase: () -> Unit,
    onRegenerateSession: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main playback controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrevious) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Previous"
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = "Next"
                    )
                }
                IconButton(onClick = onSkipPhase) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardDoubleArrowRight,
                        contentDescription = "Next phase"
                    )
                }
            }
        }

        // Button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resume button - only show when paused
            if (!isPlaying) {
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resume")
                }
            }

            // Restart subphase button
            Button(
                onClick = onRestartSubPhase,
                modifier = Modifier.weight(1f)
            ) {
                Text("Restart Subphase")
            }
        }

        // Regenerate session button
        OutlinedButton(
            onClick = onRegenerateSession,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Regenerate Session (Apply WPM Changes)")
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Press start to generate a marathon session",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%d:%02d:%02d".format(hours, minutes, seconds)
}