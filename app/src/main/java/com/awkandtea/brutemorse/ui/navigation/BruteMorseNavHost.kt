/*
 * ============================================================================
 * FILE: BruteMorseNavHost.kt
 * LOCATION: app/src/main/java/com/example/brutemorse/ui/navigation/BruteMorseNavHost.kt
 * STATUS: ✅ FIXED - All StateFlow collections use proper delegation
 *
 * CHANGES MADE:
 * - Added import: androidx.compose.runtime.getValue (Line ~8)
 * - Line ~62: Listen screen - extracted state with 'by' delegation
 * - Line ~89: Active screen - extracted state with 'by' delegation
 * - Line ~108: FreePractice screen - extracted settingsState with 'by' delegation
 * - Line ~122: TimingPractice screen - extracted settingsState with 'by' delegation
 * - Line ~134: Challenges screen - extracted settingsState with 'by' delegation
 * - Line ~150: Settings screen - extracted settingsState with 'by' delegation
 * - Line ~164: KeyerTest screen - extracted keyerState with 'by' delegation
 * - Line ~173: RepetitionSettings screen - extracted settingsState with 'by' delegation
 * ============================================================================
 */

package com.awkandtea.brutemorse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue  // ✅ FIX: Added import for 'by' delegation
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.awkandtea.brutemorse.LocalPlaybackViewModel
import com.awkandtea.brutemorse.ui.screens.ActiveScreen
import com.awkandtea.brutemorse.ui.screens.ChallengesScreen
import com.awkandtea.brutemorse.ui.screens.FreePracticeScreen
import com.awkandtea.brutemorse.ui.screens.KeyerTestScreen
import com.awkandtea.brutemorse.ui.screens.ListenScreen
import com.awkandtea.brutemorse.ui.screens.MainMenuScreen
import com.awkandtea.brutemorse.ui.screens.RepetitionSettingsScreen
import com.awkandtea.brutemorse.ui.screens.ScenarioLibraryScreen
import com.awkandtea.brutemorse.ui.screens.SettingsScreen
import com.awkandtea.brutemorse.ui.screens.TimingPracticeScreen

sealed class Screen(val route: String) {
    data object MainMenu : Screen("main_menu")
    data object Listen : Screen("listen")
    data object Active : Screen("active")
    data object FreePractice : Screen("free_practice")
    data object TimingPractice : Screen("timing_practice")
    data object Challenges : Screen("challenges")
    data object Settings : Screen("settings")
    data object Scenarios : Screen("scenarios")
    data object KeyerTest : Screen("keyer_test")
    data object RepetitionSettings : Screen("repetition_settings")
}

@Composable
fun BruteMorseNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val playbackViewModel = LocalPlaybackViewModel.current

    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route,
        modifier = modifier
    ) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onNavigateListen = {
                    // Only generate new session if one doesn't exist
                    val currentState = playbackViewModel.uiState.value
                    if (currentState.totalSteps == 0) {
                        playbackViewModel.generateSession()
                    }
                    navController.navigate(Screen.Listen.route)
                },
                onNavigateActive = {
                    // Only generate new session if one doesn't exist
                    val currentActiveState = playbackViewModel.activeState.value
                    if (currentActiveState.currentTokens.isEmpty()) {
                        playbackViewModel.generateActiveSession()
                    }
                    navController.navigate(Screen.Active.route)
                },
                onNavigateFreePractice = {
                    navController.navigate(Screen.FreePractice.route)
                },
                onNavigateTimingPractice = {
                    navController.navigate(Screen.TimingPractice.route)
                },
                onNavigateChallenges = {
                    navController.navigate(Screen.Challenges.route)
                },
                onNavigateSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateScenarios = {
                    navController.navigate(Screen.Scenarios.route)
                }
            )
        }

        composable(Screen.Listen.route) {
            // ✅ FIX: Extract state using 'by' delegation instead of .value
            val state by playbackViewModel.uiState.collectAsState()

            ListenScreen(
                state = state,
                onPlayPause = playbackViewModel::togglePlayback,
                onSkipNext = playbackViewModel::skipNext,
                onSkipPrevious = playbackViewModel::skipPrevious,
                onSkipPhase = playbackViewModel::skipPhase,
                onRestartSubPhase = playbackViewModel::restartSubPhase,
                onSeekToTime = playbackViewModel::seekToTime,
                onRegenerateSession = playbackViewModel::generateSession,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateHome = {
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                },
                onPauseOnExit = playbackViewModel::pausePlayback,
                onJumpToPhase = playbackViewModel::jumpToPhase,
                onJumpToSubPhase = playbackViewModel::jumpToSubPhase
            )
        }

        composable(Screen.Active.route) {
            // ✅ FIX: Extract state using 'by' delegation
            val state by playbackViewModel.activeState.collectAsState()

            ActiveScreen(
                state = state,
                onKeyDown = playbackViewModel::onActiveKeyDown,
                onKeyUp = playbackViewModel::onActiveKeyUp,
                onNextSet = playbackViewModel::onActiveNextSet,
                onBack = playbackViewModel::onActiveBackToResults,
                onRestartSubPhase = playbackViewModel::restartActiveSubPhase,
                onSeekToStep = playbackViewModel::seekToActiveStep,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateHome = {
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                },
                onEnableAudioInput = playbackViewModel::enableAudioInput,
                onJumpToPhase = playbackViewModel::jumpToPhaseActive,
                onJumpToSubPhase = playbackViewModel::jumpToSubPhaseActive
            )
        }

        composable(Screen.FreePractice.route) {
            // ✅ FIX: Extract settingsState using 'by' delegation
            val settingsState by playbackViewModel.settingsState.collectAsState()

            FreePracticeScreen(
                onNavigateHome = {
                    playbackViewModel.stopMorsePlayback()
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                },
                onKeyDown = playbackViewModel::onActiveKeyDown,
                onKeyUp = playbackViewModel::onActiveKeyUp,
                onPlayback = playbackViewModel::playMorsePattern,
                onStopPlayback = playbackViewModel::stopMorsePlayback,
                settingsState = settingsState
            )
        }

        composable(Screen.TimingPractice.route) {
            // ✅ FIX: Extract settingsState using 'by' delegation
            val settingsState by playbackViewModel.settingsState.collectAsState()

            TimingPracticeScreen(
                onNavigateHome = {
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                },
                onKeyDown = playbackViewModel::onActiveKeyDown,
                onKeyUp = playbackViewModel::onActiveKeyUp,
                settingsState = settingsState
            )
        }

        composable(Screen.Challenges.route) {
            // ✅ FIX: Extract settingsState using 'by' delegation
            val settingsState by playbackViewModel.settingsState.collectAsState()

            ChallengesScreen(
                onNavigateHome = {
                    playbackViewModel.stopMorsePlayback()
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                },
                onKeyDown = playbackViewModel::onActiveKeyDown,
                onKeyUp = playbackViewModel::onActiveKeyUp,
                onPlayback = playbackViewModel::playMorsePattern,
                onStopPlayback = playbackViewModel::stopMorsePlayback,
                settingsState = settingsState
            )
        }

        composable(Screen.Settings.route) {
            // ✅ FIX: Extract settingsState using 'by' delegation
            val settingsState by playbackViewModel.settingsState.collectAsState()

            SettingsScreen(
                settingsState = settingsState,
                onSettingsChange = playbackViewModel::updateSettings,
                onNavigateUp = { navController.popBackStack() },
                onOpenKeyerTest = { navController.navigate(Screen.KeyerTest.route) },
                onOpenRepetitionSettings = { navController.navigate(Screen.RepetitionSettings.route) }
            )
        }

        composable(Screen.Scenarios.route) {
            ScenarioLibraryScreen(
                scenarios = playbackViewModel.scenarios,
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(Screen.KeyerTest.route) {
            // ✅ FIX: Extract keyerState using 'by' delegation
            val keyerState by playbackViewModel.keyerTestState.collectAsState()

            KeyerTestScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartListening = playbackViewModel::startKeyerTest,
                onStopListening = playbackViewModel::stopKeyerTest,
                keyerState = keyerState
            )
        }

        composable(Screen.RepetitionSettings.route) {
            // ✅ FIX: Extract settingsState using 'by' delegation
            val settingsState by playbackViewModel.settingsState.collectAsState()

            RepetitionSettingsScreen(
                settingsState = settingsState,
                onSettingsChange = playbackViewModel::updateSettings,
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}
