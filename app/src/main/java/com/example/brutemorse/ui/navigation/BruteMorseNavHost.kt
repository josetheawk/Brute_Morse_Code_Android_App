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
import com.example.brutemorse.ui.screens.ListenScreen
import com.example.brutemorse.ui.screens.ScenarioLibraryScreen
import com.example.brutemorse.ui.screens.SettingsScreen
import com.example.brutemorse.ui.screens.SetupScreen

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Listen : Screen("listen")
    data object Active : Screen("active")
    data object Settings : Screen("settings")
    data object Scenarios : Screen("scenarios")
}

@Composable
fun BruteMorseNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val playbackViewModel = LocalPlaybackViewModel.current

    NavHost(
        navController = navController,
        startDestination = Screen.Setup.route,
        modifier = modifier
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onStartTraining = {
                    playbackViewModel.generateSession()
                    navController.navigate(Screen.Listen.route)
                },
                onStartActive = {
                    playbackViewModel.generateActiveSession()
                    navController.navigate(Screen.Active.route)
                },
                settingsState = playbackViewModel.settingsState.collectAsState().value,
                onSettingsChange = playbackViewModel::updateSettings,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenScenarios = { navController.navigate(Screen.Scenarios.route) }
            )
        }
        composable(Screen.Listen.route) {
            ListenScreen(
                state = playbackViewModel.uiState.collectAsState().value,
                onPlayPause = playbackViewModel::togglePlayback,
                onSkipNext = playbackViewModel::skipNext,
                onSkipPrevious = playbackViewModel::skipPrevious,
                onSkipPhase = playbackViewModel::skipPhase,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateHome = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
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
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateHome = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
                onEnableAudioInput = playbackViewModel::enableAudioInput,
                onJumpToPhase = playbackViewModel::jumpToPhaseActive,
                onJumpToSubPhase = playbackViewModel::jumpToSubPhaseActive
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsState = playbackViewModel.settingsState.collectAsState().value,
                onSettingsChange = playbackViewModel::updateSettings,
                onNavigateUp = { navController.popBackStack() }
            )
        }
        composable(Screen.Scenarios.route) {
            ScenarioLibraryScreen(
                scenarios = playbackViewModel.scenarios,
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}