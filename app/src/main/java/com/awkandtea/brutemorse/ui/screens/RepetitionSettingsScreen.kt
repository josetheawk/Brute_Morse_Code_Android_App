package com.awkandtea.brutemorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awkandtea.brutemorse.model.UserSettings

@Composable
fun RepetitionSettingsScreen(
    settingsState: UserSettings,
    onSettingsChange: ((UserSettings) -> UserSettings) -> Unit,
    onNavigateUp: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                "Repetition Settings",
                style = MaterialTheme.typography.titleLarge
            )
            // Spacer for symmetry
            IconButton(onClick = {}) {
                // Empty to maintain layout
            }
        }

        Text(
            "Configure how many times each element repeats during active recall training",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // Phase 1.1 NestedID
        RepetitionSlider(
            title = "Phase 1.1: Alphabet (NestedID)",
            description = "A, B, C... individual letters",
            currentValue = settingsState.repetitionPhase1NestedID,
            range = 3f..15f,
            onValueChange = { value ->
                onSettingsChange { it.copy(repetitionPhase1NestedID = value.toInt()) }
            }
        )

        // Phase 1.2 BCT
        RepetitionSlider(
            title = "Phase 1.2: Alphabet (BCT)",
            description = "Balanced coprime traversal",
            currentValue = settingsState.repetitionPhase1BCT,
            range = 3f..15f,
            onValueChange = { value ->
                onSettingsChange { it.copy(repetitionPhase1BCT = value.toInt()) }
            }
        )

        HorizontalDivider()

        // Phase 2.1 Numbers NestedID
        RepetitionSlider(
            title = "Phase 2.1: Numbers (NestedID)",
            description = "0, 1, 2... individual numbers",
            currentValue = settingsState.repetitionPhase2NestedID,
            range = 3f..15f,
            onValueChange = { value ->
                onSettingsChange { it.copy(repetitionPhase2NestedID = value.toInt()) }
            }
        )

        // Phase 2.2 Mixed BCT
        RepetitionSlider(
            title = "Phase 2.2: Mixed Set (BCT)",
            description = "Letters + Numbers combined",
            currentValue = settingsState.repetitionPhase2BCT,
            range = 3f..15f,
            onValueChange = { value ->
                onSettingsChange { it.copy(repetitionPhase2BCT = value.toInt()) }
            }
        )

        HorizontalDivider()

        // Phase 3.1 Vocabulary
        RepetitionSlider(
            title = "Phase 3.1: Vocabulary",
            description = "CQ, QTH, RST... whole words",
            currentValue = settingsState.repetitionPhase3Vocab,
            range = 3f..7f,  // Capped at 7 to prevent super long sessions
            onValueChange = { value ->
                onSettingsChange { it.copy(repetitionPhase3Vocab = value.toInt()) }
            }
        )

        HorizontalDivider()

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Pattern Explanation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "* First 3 reps: Morse + Vocal (M-V)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "* Reps 4+: Morse only (M)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "* Every 5th after position 3: Morse + Vocal (M-V)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Example at 10 reps: M-V, M-V, M-V, M, M, M, M, M-V, M, M",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Reset button
        OutlinedButton(
            onClick = {
                onSettingsChange {
                    it.copy(
                        repetitionPhase1NestedID = UserSettings.DEFAULT_REPETITION_LETTERS,
                        repetitionPhase1BCT = UserSettings.DEFAULT_REPETITION_BCT,
                        repetitionPhase2NestedID = UserSettings.DEFAULT_REPETITION_NUMBERS,
                        repetitionPhase2BCT = UserSettings.DEFAULT_REPETITION_BCT_MIX,
                        repetitionPhase3Vocab = UserSettings.DEFAULT_REPETITION_VOCAB
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset All to Defaults (3)")
        }

        // Back button
        Button(
            onClick = onNavigateUp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Settings")
        }
    }
}

@Composable
private fun RepetitionSlider(
    title: String,
    description: String,
    currentValue: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = currentValue.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = currentValue.toFloat(),
                onValueChange = onValueChange,
                valueRange = range,
                steps = (range.endInclusive - range.start).toInt() - 1
            )

            // Preview pattern
            val previewPattern = buildString {
                repeat(currentValue) { index ->
                    val hasVocal = index < 3 || (index >= 3 && (index - 2) % 5 == 0)
                    append(if (hasVocal) "M-V " else "M ")
                }
            }.trim()

            Text(
                text = "Pattern: $previewPattern",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
