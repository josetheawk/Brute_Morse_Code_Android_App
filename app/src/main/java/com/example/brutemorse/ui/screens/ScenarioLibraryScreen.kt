package com.example.brutemorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.brutemorse.model.ScenarioCategory
import com.example.brutemorse.model.ScenarioScript

@Composable
fun ScenarioLibraryScreen(
    scenarios: List<ScenarioScript>,
    onNavigateUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Scenario Library", style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(scenarios) { scenario ->
                ScenarioCard(scenario)
            }
        }
        Button(onClick = onNavigateUp, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun ScenarioCard(scenario: ScenarioScript) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = scenario.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = when (scenario.category) {
                    ScenarioCategory.NORMAL -> "Normal Rag-Chew"
                    ScenarioCategory.SKYWARN -> "SKYWARN"
                    ScenarioCategory.APOCALYPSE -> "Apocalypse"
                },
                style = MaterialTheme.typography.labelLarge
            )
            scenario.lines.forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
