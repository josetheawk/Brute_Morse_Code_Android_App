package com.example.brutemorse.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("brute_morse_settings")

private object PreferenceKeys {
    val CALLSIGN = stringPreferencesKey("callsign")
    val FRIEND_CALLSIGNS = stringPreferencesKey("friend_callsigns")
    val WPM = intPreferencesKey("wpm")
    val TONE = intPreferencesKey("tone")
    val PHASE_SELECTION = stringPreferencesKey("phases")
}

data class UserSettings(
    val callSign: String = "",
    val friendCallSigns: List<String> = emptyList(),
    val wpm: Int = 25,
    val toneFrequencyHz: Int = 800,
    val phaseSelection: Set<Int> = setOf(1, 2, 3, 4)
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        preferences.toUserSettings()
    }

    suspend fun update(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.CALLSIGN] = settings.callSign
            prefs[PreferenceKeys.FRIEND_CALLSIGNS] = settings.friendCallSigns.joinToString(",")
            prefs[PreferenceKeys.WPM] = settings.wpm
            prefs[PreferenceKeys.TONE] = settings.toneFrequencyHz
            prefs[PreferenceKeys.PHASE_SELECTION] = settings.phaseSelection.sorted().joinToString(",")
        }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val friendString = this[PreferenceKeys.FRIEND_CALLSIGNS].orEmpty()
        val phaseString = this[PreferenceKeys.PHASE_SELECTION].orEmpty()
        return UserSettings(
            callSign = this[PreferenceKeys.CALLSIGN].orEmpty(),
            friendCallSigns = friendString.split(',').filter { it.isNotBlank() },
            wpm = this[PreferenceKeys.WPM] ?: 25,
            toneFrequencyHz = this[PreferenceKeys.TONE] ?: 800,
            phaseSelection = phaseString.split(',').mapNotNull { it.toIntOrNull() }.toSet().ifEmpty { setOf(1, 2, 3, 4) }
        )
    }
}
