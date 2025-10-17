package com.awkandtea.brutemorse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.awkandtea.brutemorse.ui.navigation.BruteMorseNavHost
import com.awkandtea.brutemorse.ui.theme.BruteMorseTheme
import com.awkandtea.brutemorse.viewmodel.PlaybackViewModel
import com.awkandtea.brutemorse.viewmodel.PlaybackViewModelFactory

val LocalPlaybackViewModel = staticCompositionLocalOf<PlaybackViewModel> {
    error("PlaybackViewModel not provided")
}

class MainActivity : ComponentActivity() {

    private val playbackViewModel: PlaybackViewModel by viewModels {
        val app = application as BruteMorseApp
        PlaybackViewModelFactory(app.sessionRepository, app.settingsRepository, applicationContext)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BruteMorseTheme {
                CompositionLocalProvider(LocalPlaybackViewModel provides playbackViewModel) {
                    BruteMorseNavHost()
                }
            }
        }
    }
}

