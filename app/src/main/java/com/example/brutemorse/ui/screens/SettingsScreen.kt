package com.example.brutemorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.brutemorse.data.UserSettings

@Composable
fun SettingsScreen(
    settingsState: UserSettings,
    onSettingsChange: ((UserSettings) -> UserSettings) -> Unit,
    onNavigateUp: () -> Unit
) {
    var callSign by remember(settingsState.callSign) { mutableStateOf(TextFieldValue(settingsState.callSign)) }
    var friends by remember(settingsState.friendCallSigns) {
        mutableStateOf(TextFieldValue(settingsState.friendCallSigns.joinToString(", ")))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = callSign,
            onValueChange = {
                callSign = it
                onSettingsChange { settings -> settings.copy(callSign = it.text.uppercase()) }
            },
            label = { Text("Call sign") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = friends,
            onValueChange = {
                friends = it
                val entries = it.text.split(',').map { value -> value.trim().uppercase() }.filter { value -> value.isNotEmpty() }
                onSettingsChange { settings -> settings.copy(friendCallSigns = entries.take(10)) }
            },
            label = { Text("Friend call signs") },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Training Phases", style = MaterialTheme.typography.titleMedium)

        PhaseSelection(
            selected = settingsState.phaseSelection,
            onSelectionChange = { phases ->
                onSettingsChange { settings -> settings.copy(phaseSelection = phases) }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Words per minute: ${settingsState.wpm}")
        Slider(
            value = settingsState.wpm.toFloat(),
            onValueChange = { value -> onSettingsChange { settings -> settings.copy(wpm = value.toInt()) } },
            valueRange = 15f..40f
        )

        Text("Tone frequency: ${settingsState.toneFrequencyHz} Hz")
        Slider(
            value = settingsState.toneFrequencyHz.toFloat(),
            onValueChange = { value -> onSettingsChange { settings -> settings.copy(toneFrequencyHz = value.toInt()) } },
            valueRange = 400f..1200f
        )

        Button(onClick = onNavigateUp, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Checkbox(
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
}