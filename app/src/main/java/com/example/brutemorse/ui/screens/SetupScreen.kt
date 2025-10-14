package com.example.brutemorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.brutemorse.data.UserSettings

@Composable
fun SetupScreen(
    onStartTraining: () -> Unit,
    onStartActive: () -> Unit = {},
    settingsState: UserSettings,
    onSettingsChange: ((UserSettings) -> UserSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenScenarios: () -> Unit
) {
    var callSign by remember(settingsState.callSign) { mutableStateOf(TextFieldValue(settingsState.callSign)) }
    var friends by remember(settingsState.friendCallSigns) {
        mutableStateOf(TextFieldValue(settingsState.friendCallSigns.joinToString(", ")))
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Brute Force Passive Immersion",
            style = MaterialTheme.typography.titleLarge
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("One-time setup", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = callSign,
                    onValueChange = {
                        callSign = it
                        onSettingsChange { settings ->
                            settings.copy(callSign = it.text.uppercase())
                        }
                    },
                    label = { Text("Your call sign") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    )
                )
                OutlinedTextField(
                    value = friends,
                    onValueChange = {
                        friends = it
                        val entries = it.text.split(',')
                            .map { value -> value.trim().uppercase() }
                            .filter { value -> value.isNotEmpty() }
                        onSettingsChange { settings ->
                            settings.copy(friendCallSigns = entries.take(10))
                        }
                    },
                    label = { Text("Friends' call signs (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    )
                )

                Text("Training phases", style = MaterialTheme.typography.titleLarge)
                PhaseSelection(
                    selected = settingsState.phaseSelection,
                    onSelectionChange = { phases ->
                        onSettingsChange { settings -> settings.copy(phaseSelection = phases) }
                    }
                )

                Text("Words per minute: ${settingsState.wpm}")
                Slider(
                    value = settingsState.wpm.toFloat(),
                    onValueChange = { value ->
                        onSettingsChange { settings -> settings.copy(wpm = value.toInt()) }
                    },
                    valueRange = 15f..40f
                )

                Text("Tone frequency: ${settingsState.toneFrequencyHz} Hz")
                Slider(
                    value = settingsState.toneFrequencyHz.toFloat(),
                    onValueChange = { value ->
                        onSettingsChange { settings -> settings.copy(toneFrequencyHz = value.toInt()) }
                    },
                    valueRange = 400f..1200f
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Two training mode buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStartTraining,
                        modifier = Modifier.weight(1f),
                        enabled = settingsState.phaseSelection.isNotEmpty()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LISTEN")
                            Text("(Passive)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedButton(
                        onClick = onStartActive,
                        modifier = Modifier.weight(1f),
                        enabled = settingsState.phaseSelection.isNotEmpty()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ACTIVE")
                            Text("(Send)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        FilledTonalButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Fine-tune playback settings")
        }
        FilledTonalButton(onClick = onOpenScenarios) {
            Icon(Icons.Filled.LibraryBooks, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Preview scenario library")
        }
    }
}

@Composable
private fun PhaseSelection(
    selected: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit
) {
    val phases = listOf(
        1 to "Phase 1: Alphabet Mastery",
        2 to "Phase 2: Expanded Set",
        3 to "Phase 3: Words & Abbreviations",
        4 to "Phase 4: Real World QSOs"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        phases.forEach { (index, title) ->
            val isChecked = selected.contains(index)
            PhaseRow(
                title = title,
                checked = isChecked,
                onCheckedChange = { checked ->
                    val newSelection = selected.toMutableSet()
                    if (checked) newSelection += index else newSelection -= index
                    onSelectionChange(newSelection)
                }
            )
        }
    }
}

@Composable
private fun PhaseRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}