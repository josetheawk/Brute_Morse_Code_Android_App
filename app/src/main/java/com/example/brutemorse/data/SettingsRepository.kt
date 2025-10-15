package com.example.brutemorse.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("brute_morse_settings")

private object PreferenceKeys {
    val CALLSIGN = stringPreferencesKey("callsign")
    val FRIEND_CALLSIGNS = stringPreferencesKey("friend_callsigns")
    val WPM = intPreferencesKey("wpm")
    val TONE = intPreferencesKey("tone")
    val PHASE_SELECTION = stringPreferencesKey("phases")
    val MIGRATION_DONE = stringPreferencesKey("migration_v2_done")
    val LAST_PLAYBACK_INDEX = intPreferencesKey("last_playback_index")
    val LAST_ACTIVE_INDEX = intPreferencesKey("last_active_index")
}

data class UserSettings(
    val callSign: String = "",
    val friendCallSigns: List<String> = emptyList(),
    val wpm: Int = 25,
    val toneFrequencyHz: Int = 800,
    val phaseSelection: Set<Int> = setOf(1, 2, 3, 4)
) {
    // Centralized timing configuration - computed from WPM
    val timing: MorseTimingConfig
        get() = MorseTimingConfig(wpm)
}

class SettingsRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            try {
                val prefs = context.dataStore.data.first()
                if (prefs[PreferenceKeys.MIGRATION_DONE] != "true") {
                    android.util.Log.d("SettingsRepository", "Migration v2 needed, running...")
                    migrateReversedData()
                } else {
                    android.util.Log.d("SettingsRepository", "Migration v2 already done")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsRepository", "Migration error", e)
            }
        }
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        preferences.toUserSettings()
    }

    private suspend fun migrateReversedData() {
        context.dataStore.edit { prefs ->
            android.util.Log.d("SettingsRepository", "Starting migration v2...")

            // ALWAYS reverse any existing data - the old code was buggy
            val callSign = prefs[PreferenceKeys.CALLSIGN]
            android.util.Log.d("SettingsRepository", "Current callsign: '$callSign'")

            if (!callSign.isNullOrEmpty()) {
                val fixed = callSign.reversed()
                prefs[PreferenceKeys.CALLSIGN] = fixed
                android.util.Log.d("SettingsRepository", "Reversed callsign: '$callSign' -> '$fixed'")
            }

            val friendString = prefs[PreferenceKeys.FRIEND_CALLSIGNS]
            android.util.Log.d("SettingsRepository", "Current friends: '$friendString'")

            if (!friendString.isNullOrEmpty()) {
                val friends = friendString.split(',').map { it.trim() }
                val fixedFriends = friends.map { friend ->
                    if (friend.isNotEmpty()) {
                        friend.reversed()
                    } else {
                        friend
                    }
                }
                prefs[PreferenceKeys.FRIEND_CALLSIGNS] = fixedFriends.joinToString(",")
                android.util.Log.d("SettingsRepository", "Reversed friends: '$friendString' -> '${fixedFriends.joinToString(",")}'")
            }

            // Mark migration as complete
            prefs[PreferenceKeys.MIGRATION_DONE] = "true"
            android.util.Log.d("SettingsRepository", "Migration v2 complete!")
        }
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

    suspend fun saveLastPlaybackIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_PLAYBACK_INDEX] = index
        }
    }

    suspend fun getLastPlaybackIndex(): Int {
        val prefs = context.dataStore.data.first()
        return prefs[PreferenceKeys.LAST_PLAYBACK_INDEX] ?: -1
    }

    suspend fun saveLastActiveIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_ACTIVE_INDEX] = index
        }
    }

    suspend fun getLastActiveIndex(): Int {
        val prefs = context.dataStore.data.first()
        return prefs[PreferenceKeys.LAST_ACTIVE_INDEX] ?: -1
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