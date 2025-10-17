package com.awkandtea.brutemorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.awkandtea.brutemorse.model.UserSettings

@Composable
fun SettingsScreen(
    settingsState: UserSettings,
    onSettingsChange: ((UserSettings) -> UserSettings) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenKeyerTest: () -> Unit = {},
    onOpenRepetitionSettings: () -> Unit = {}
) {
    var callSign by remember(settingsState.callSign) { mutableStateOf(settingsState.callSign) }
    var friends by remember(settingsState.friendCallSigns) {
        mutableStateOf(settingsState.friendCallSigns.joinToString(", "))
    }

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
            Text("Settings", style = MaterialTheme.typography.titleLarge)
            // Empty spacer for symmetry
            IconButton(onClick = {}) {}
        }

        // Personal Info Section
        Text("Personal Information", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = callSign,
            onValueChange = { newValue ->
                callSign = newValue
                val uppercased = newValue.uppercase()
                onSettingsChange { settings -> settings.copy(callSign = uppercased) }
            },
            label = { Text("Your call sign") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii
            ),
            singleLine = true,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        OutlinedTextField(
            value = friends,
            onValueChange = { newValue ->
                friends = newValue
                val entries = newValue.split(',')
                    .map { value -> value.trim().uppercase() }
                    .filter { value -> value.isNotEmpty() }
                onSettingsChange { settings -> settings.copy(friendCallSigns = entries.take(10)) }
            },
            label = { Text("Friend call signs (comma separated)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii
            ),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Training Phases Section
        Text("Training Phases", style = MaterialTheme.typography.titleMedium)
        Text(
            "Select which phases to include in Listen and Active modes",
            style = MaterialTheme.typography.bodySmall
        )

        PhaseSelection(
            selected = settingsState.phaseSelection,
            onSelectionChange = { phases ->
                onSettingsChange { settings -> settings.copy(phaseSelection = phases) }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Audio Settings Section
        Text("Audio Settings", style = MaterialTheme.typography.titleMedium)

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

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Repetition Settings Button
        Text("Active Recall Settings", style = MaterialTheme.typography.titleMedium)

        Button(
            onClick = onOpenRepetitionSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Configure Repetition Settings →")
        }

        Text(
            "Customize how many times each element repeats per phase",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Hardware Testing Section
        Text("Hardware Testing", style = MaterialTheme.typography.titleMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "⚠️ Important: Straight Key Compatibility",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "This app works ONLY with straight keys that produce an audio tone when closed (like the VK-5).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Keys that simply close a circuit (like traditional telegraph keys) will NOT work. The app detects audio input through the microphone jack.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Button(
            onClick = onOpenKeyerTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Physical Straight Key / Audio Input")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Button(onClick = onNavigateUp, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Menu")
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