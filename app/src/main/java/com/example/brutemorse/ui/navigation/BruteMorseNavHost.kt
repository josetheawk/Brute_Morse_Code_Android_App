package com.example.brutemorse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.brutemorse.LocalPlaybackViewModel
import com.example.brutemorse.ui.screens.ActiveScreen
import com.example.brutemorse.ui.screens.ChallengesScreen
import com.example.brutemorse.ui.screens.FreePracticeScreen
import com.example.brutemorse.ui.screens.KeyerTestScreen
import com.example.brutemorse.ui.screens.ListenScreen
import com.example.brutemorse.ui.screens.MainMenuScreen
import com.example.brutemorse.ui.screens.ScenarioLibraryScreen
import com.example.brutemorse.ui.screens.SettingsScreen
import com.example.brutemorse.ui.screens.TimingPracticeScreen

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
            ListenScreen(
                state = playbackViewModel.uiState.collectAsState().value,
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
            ActiveScreen(
                state = playbackViewModel.activeState.collectAsState().value,
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
                settingsState = playbackViewModel.settingsState.collectAsState().value
            )
        }

        composable(Screen.TimingPractice.route) {
            TimingPracticeScreen(
                onNavigateHome = {
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                    }
                },
                onKeyDown = playbackViewModel::onActiveKeyDown,
                onKeyUp = playbackViewModel::onActiveKeyUp,
                settingsState = playbackViewModel.settingsState.collectAsState().value
            )
        }

        composable(Screen.Challenges.route) {
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
                settingsState = playbackViewModel.settingsState.collectAsState().value
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsState = playbackViewModel.settingsState.collectAsState().value,
                onSettingsChange = playbackViewModel::updateSettings,
                onNavigateUp = { navController.popBackStack() },
                onOpenKeyerTest = { navController.navigate(Screen.KeyerTest.route) }
            )
        }

        composable(Screen.Scenarios.route) {
            ScenarioLibraryScreen(
                scenarios = playbackViewModel.scenarios,
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(Screen.KeyerTest.route) {
            KeyerTestScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartListening = playbackViewModel::startKeyerTest,
                onStopListening = playbackViewModel::stopKeyerTest,
                keyerState = playbackViewModel.keyerTestState.collectAsState().value
            )
        }
    }
}