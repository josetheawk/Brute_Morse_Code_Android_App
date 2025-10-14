package com.example.brutemorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FastForward
import androidx.compose.material.icons.automirrored.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.brutemorse.model.MorseElement
import com.example.brutemorse.model.PlaybackElement
import com.example.brutemorse.model.SpeechElement
import com.example.brutemorse.viewmodel.PlaybackUiState

@Composable
fun ListenScreen(
    state: PlaybackUiState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipPhase: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val currentStep = state.currentStep

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("≡ Listen", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Open settings")
            }
        }

        if (currentStep != null) {
            PhaseHeader(state)
            PlaybackVisualizer(currentStep.elements)
            PlaybackDetails(state)
        } else {
            EmptyState()
        }

        Spacer(modifier = Modifier.weight(1f))

        PlaybackControls(
            isPlaying = state.isPlaying,
            onPlayPause = onPlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSkipPhase = onSkipPhase
        )
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
    val morse = elements.filterIsInstance<MorseElement>().lastOrNull()
    val speech = elements.filterIsInstance<SpeechElement>().lastOrNull()

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
            Text(
                text = morse?.symbol ?: "· · ·",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = morse?.character ?: "Waiting",
                style = MaterialTheme.typography.titleLarge
            )
            speech?.let { Text(text = it.text, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun PlaybackDetails(state: PlaybackUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Elapsed ${formatDuration(state.elapsedMillis)} / ${formatDuration(state.totalMillis)}",
            style = MaterialTheme.typography.bodyLarge
        )
        LinearProgressIndicator(
            progress = if (state.totalMillis == 0L) 0f else state.elapsedMillis / state.totalMillis.toFloat(),
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
    onSkipPhase: () -> Unit
) {
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
                    imageVector = Icons.AutoMirrored.Filled.FastRewind,
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
                    imageVector = Icons.AutoMirrored.Filled.FastForward,
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
