package com.example.brutemorse

import android.app.Application
import com.example.brutemorse.data.SessionRepository
import com.example.brutemorse.data.SettingsRepository

class BruteMorseApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var sessionRepository: SessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        sessionRepository = SessionRepository(settingsRepository)
    }
}
