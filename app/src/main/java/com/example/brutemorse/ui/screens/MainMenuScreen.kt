package com.example.brutemorse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onNavigateListen: () -> Unit,
    onNavigateActive: () -> Unit,
    onNavigateFreePractice: () -> Unit,
    onNavigateTimingPractice: () -> Unit,
    onNavigateChallenges: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateScenarios: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Brute Morse",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Code Trainer",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Training Modes Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "5 Training Modes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Listen Mode
                Button(
                    onClick = onNavigateListen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LISTEN", style = MaterialTheme.typography.titleMedium)
                        Text("Passive Learning", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Active Mode
                Button(
                    onClick = onNavigateActive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ACTIVE", style = MaterialTheme.typography.titleMedium)
                        Text("Guided Practice", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Free Practice
                Button(
                    onClick = onNavigateFreePractice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FREE PRACTICE", style = MaterialTheme.typography.titleMedium)
                        Text("Type Anything", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Timing Practice
                Button(
                    onClick = onNavigateTimingPractice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TIMING DRILL", style = MaterialTheme.typography.titleMedium)
                        Text("Perfect Your • and —", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Challenges
                Button(
                    onClick = onNavigateChallenges,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CHALLENGES", style = MaterialTheme.typography.titleMedium)
                        Text("Type Scenarios", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Utilities Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onNavigateSettings,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Settings")
                }
            }

            FilledTonalButton(
                onClick = onNavigateScenarios,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.LibraryBooks, contentDescription = null)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Scenarios")
                }
            }
        }
    }
}