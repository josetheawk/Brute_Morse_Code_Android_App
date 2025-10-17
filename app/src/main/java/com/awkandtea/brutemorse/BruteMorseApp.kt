package com.awkandtea.brutemorse

import android.app.Application
import com.awkandtea.brutemorse.data.SessionRepository
import com.awkandtea.brutemorse.data.SettingsRepository

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
