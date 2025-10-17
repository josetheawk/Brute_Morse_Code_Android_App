package com.awkandtea.brutemorse.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class KeyerTestState(
    val isListening: Boolean = false,
    val currentRMS: Float = 0f,
    val noiseFloor: Float = 100f,
    val threshold: Float = 300f,
    val isKeyDown: Boolean = false,
    val currentInput: String = "",
    val lastDetection: String = "",
    val detectionCount: Int = 0
)

@Composable
fun KeyerTestScreen(
    onNavigateBack: () -> Unit,
    onStartListening: (Float) -> Unit = {},
    onStopListening: () -> Unit = {},
    keyerState: KeyerTestState = KeyerTestState()
) {
    var permissionGranted by remember { mutableStateOf(false) }
    var showPermissionNeeded by remember { mutableStateOf(true) }
    var sensitivity by remember { mutableFloatStateOf(2.5f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        showPermissionNeeded = false
        if (isGranted) {
            onStartListening(sensitivity)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopListening()
        }
    }

    val scrollState = rememberScrollState()

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
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Keyer Test", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Permission section
        if (showPermissionNeeded && !permissionGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Microphone Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("This test requires microphone access to detect your straight key.")
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        // Sensitivity control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Sensitivity: ${String.format("%.1f", sensitivity)}")
                Slider(
                    value = sensitivity,
                    onValueChange = {
                        sensitivity = it
                        if (keyerState.isListening) {
                            onStopListening()
                            onStartListening(sensitivity)
                        }
                    },
                    valueRange = 1.5f..5.0f
                )
                Text(
                    "Higher = more sensitive (easier to trigger)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Audio levels visualization
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Audio Levels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // RMS Level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current RMS:")
                    Text(
                        String.format("%.1f", keyerState.currentRMS),
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { (keyerState.currentRMS / 2000f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Noise Floor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Noise Floor:")
                    Text(
                        String.format("%.1f", keyerState.noiseFloor),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Threshold:")
                    Text(
                        String.format("%.1f", keyerState.threshold),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Key State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            if (keyerState.isKeyDown) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (keyerState.isKeyDown) "KEY DOWN" else "KEY UP",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (keyerState.isKeyDown)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Detection display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Detection Output",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("Current Input:", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        keyerState.currentInput.ifEmpty { "..." },
                        style = MaterialTheme.typography.displaySmall
                    )
                }

                if (keyerState.lastDetection.isNotEmpty()) {
                    Text("Last Detected:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        keyerState.lastDetection,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Total Detections: ${keyerState.detectionCount}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!keyerState.isListening) {
                Button(
                    onClick = {
                        if (permissionGranted) {
                            onStartListening(sensitivity)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = permissionGranted || !showPermissionNeeded
                ) {
                    Text("Start Listening")
                }
            } else {
                OutlinedButton(
                    onClick = onStopListening,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Listening")
                }
            }
        }

        // Help text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Tips:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("* Make sure your straight key is plugged into the microphone jack")
                Text("* Or use a USB-C audio adapter if needed")
                Text("* The RMS should jump when you press the key")
                Text("* If RMS stays near noise floor, increase sensitivity")
                Text("* Wait a few seconds for noise floor to stabilize")
            }
        }
    }
}
