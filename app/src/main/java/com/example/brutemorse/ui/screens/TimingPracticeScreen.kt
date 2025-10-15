package com.example.brutemorse.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.brutemorse.data.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PracticeMode {
    DIT, DAH, BOTH
}

@Composable
fun TimingPracticeScreen(
    onNavigateHome: () -> Unit,
    onKeyDown: () -> Unit,
    onKeyUp: () -> Unit,
    settingsState: UserSettings
) {
    var score by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var isPressed by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<FeedbackState>(FeedbackState.None) }
    var currentMode by remember { mutableStateOf(PracticeMode.BOTH) }
    var currentTarget by remember { mutableStateOf<TargetType>(TargetType.DIT) }
    var pressStartTime by remember { mutableLongStateOf(0L) }

    val sliderPosition = remember { Animatable(1f) } // Start from right (1.0)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Use centralized timing configuration
    val timing = settingsState.timing

    // Total time for slider to cross (make it visible)
    val totalSliderTimeMs = 250L

    // Calculate zones as percentages of the slider
    // Allow 20% tolerance on timing
    val ditGreenStart = (timing.ditMs.toFloat() / totalSliderTimeMs) * 0.8f
    val ditGreenEnd = (timing.ditMs.toFloat() / totalSliderTimeMs) * 1.2f
    val ditRedEnd = ditGreenEnd + 0.15f // Red zone after green

    val dahGreenStart = (timing.dahMs.toFloat() / totalSliderTimeMs) * 0.8f
    val dahGreenEnd = (timing.dahMs.toFloat() / totalSliderTimeMs) * 1.2f
    val dahRedEnd = dahGreenEnd + 0.15f

    // Select target based on mode
    LaunchedEffect(currentMode) {
        // Set the FIRST target when mode changes
        currentTarget = when (currentMode) {
            PracticeMode.DIT -> TargetType.DIT
            PracticeMode.DAH -> TargetType.DAH
            PracticeMode.BOTH -> TargetType.DIT // Start with DIT
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != FeedbackState.None) {
            delay(800)
            feedback = FeedbackState.None

            // After feedback clears, set up NEXT target
            currentTarget = when (currentMode) {
                PracticeMode.DIT -> TargetType.DIT
                PracticeMode.DAH -> TargetType.DAH
                PracticeMode.BOTH -> {
                    // Alternate: if we just did DIT, next is DAH, and vice versa
                    if (currentTarget == TargetType.DIT) TargetType.DAH else TargetType.DIT
                }
            }
        }
    }

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
            IconButton(onClick = onNavigateHome) {
                Icon(imageVector = Icons.Filled.Home, contentDescription = "Home")
            }
            Text("Timing Drill", style = MaterialTheme.typography.titleLarge)
            Text("") // Spacer
        }

        // Compact Score Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreCard(label = "Score", value = score.toString())
            ScoreCard(label = "Attempts", value = attempts.toString())
            ScoreCard(
                label = "Accuracy",
                value = if (attempts > 0) "${(score * 100 / attempts)}%" else "0%"
            )
        }

        // Mode Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { currentMode = PracticeMode.DIT },
                modifier = Modifier.weight(1f),
                colors = if (currentMode == PracticeMode.DIT) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text("Dit (•)")
            }
            OutlinedButton(
                onClick = { currentMode = PracticeMode.DAH },
                modifier = Modifier.weight(1f),
                colors = if (currentMode == PracticeMode.DAH) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text("Dah (—)")
            }
            OutlinedButton(
                onClick = { currentMode = PracticeMode.BOTH },
                modifier = Modifier.weight(1f),
                colors = if (currentMode == PracticeMode.BOTH) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text("Both")
            }
        }

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Next: ${if (currentTarget == TargetType.DIT) "DIT (•)" else "DAH (—)"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap and hold, release in the GREEN zone!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Timing Slider
        val backgroundColor = when (feedback) {
            FeedbackState.Correct -> Color(0xFF4CAF50)
            FeedbackState.Wrong -> Color(0xFFE53935)
            FeedbackState.None -> MaterialTheme.colorScheme.surfaceVariant
        }

        val (greenStart, greenEnd, redEnd) = if (currentTarget == TargetType.DIT) {
            Triple(ditGreenStart, ditGreenEnd, ditRedEnd)
        } else {
            Triple(dahGreenStart, dahGreenEnd, dahRedEnd)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(backgroundColor, shape = MaterialTheme.shapes.medium)
                .pointerInput(feedback) {
                    if (feedback == FeedbackState.None) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                pressStartTime = System.currentTimeMillis()
                                onKeyDown()

                                // Start slider animation when pressed
                                scope.launch {
                                    sliderPosition.snapTo(1f) // Reset to right
                                    sliderPosition.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(
                                            durationMillis = totalSliderTimeMs.toInt(),
                                            easing = LinearEasing
                                        )
                                    )
                                }

                                tryAwaitRelease()

                                // Evaluate on release
                                val pressDuration = System.currentTimeMillis() - pressStartTime
                                val position = 1f - sliderPosition.value // Convert to time-based position

                                isPressed = false
                                onKeyUp()

                                scope.launch {
                                    sliderPosition.stop()

                                    val isInGreenZone = position in greenStart..greenEnd
                                    val isInRedZone = position > greenEnd && position <= redEnd

                                    attempts++
                                    if (isInGreenZone) {
                                        score++
                                        feedback = FeedbackState.Correct
                                    } else {
                                        feedback = FeedbackState.Wrong
                                    }

                                    // Wait for feedback to clear
                                    delay(800)
                                    sliderPosition.snapTo(1f) // Reset to right
                                }
                            }
                        )
                    }
                }
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // Draw blue zone (safe - too short)
                    drawRect(
                        color = Color(0xFF2196F3),
                        topLeft = Offset(0f, 0f),
                        size = Size(width * (1f - greenStart), height)
                    )

                    // Draw green zone (perfect timing)
                    drawRect(
                        color = Color(0xFF8BC34A),
                        topLeft = Offset(width * (1f - greenEnd), 0f),
                        size = Size(width * (greenEnd - greenStart), height)
                    )

                    // Draw yellow zone (acceptable)
                    drawRect(
                        color = Color(0xFFFFC107),
                        topLeft = Offset(width * (1f - redEnd), 0f),
                        size = Size(width * (redEnd - greenEnd), height)
                    )

                    // Draw red zone (too long)
                    drawRect(
                        color = Color(0xFFF44336),
                        topLeft = Offset(0f, 0f),
                        size = Size(width * (1f - redEnd), height)
                    )

                    // Draw slider position indicator (white bar moving left)
                    val sliderX = width * sliderPosition.value
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(sliderX - 5f, 0f),
                        size = Size(10f, height)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (feedback == FeedbackState.None && !isPressed) {
                Text(
                    "TAP TO START",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else if (isPressed) {
                Text(
                    "RELEASE IN GREEN!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Timing Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dit Target", style = MaterialTheme.typography.labelSmall)
                    Text("${timing.ditMs}ms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dah Target", style = MaterialTheme.typography.labelSmall)
                    Text("${timing.dahMs}ms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WPM", style = MaterialTheme.typography.labelSmall)
                    Text("${settingsState.wpm}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScoreCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private enum class FeedbackState {
    None, Correct, Wrong
}

private enum class TargetType {
    DIT, DAH
}