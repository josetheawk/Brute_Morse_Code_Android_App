package com.awkandtea.brutemorse.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.awkandtea.brutemorse.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("brute_morse_settings")

private object PreferenceKeys {
    val CALLSIGN = stringPreferencesKey("callsign")
    val FRIEND_CALLSIGNS = stringPreferencesKey("friend_callsigns")
    val WPM = intPreferencesKey("wpm")
    val TONE = intPreferencesKey("tone")
    val PHASE_SELECTION = stringPreferencesKey("phases")

    // Per-phase repetition keys
    val REPETITION_PHASE1_NESTED = intPreferencesKey("rep_phase1_nested")
    val REPETITION_PHASE1_BCT = intPreferencesKey("rep_phase1_bct")
    val REPETITION_PHASE2_NESTED = intPreferencesKey("rep_phase2_nested")
    val REPETITION_PHASE2_BCT = intPreferencesKey("rep_phase2_bct")
    val REPETITION_PHASE3_VOCAB = intPreferencesKey("rep_phase3_vocab")

    val MIGRATION_DONE = stringPreferencesKey("migration_v2_done")
    val LAST_PLAYBACK_INDEX = intPreferencesKey("last_playback_index")
    val LAST_ACTIVE_INDEX = intPreferencesKey("last_active_index")
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

            // Save all repetition counts
            prefs[PreferenceKeys.REPETITION_PHASE1_NESTED] = settings.repetitionPhase1NestedID
            prefs[PreferenceKeys.REPETITION_PHASE1_BCT] = settings.repetitionPhase1BCT
            prefs[PreferenceKeys.REPETITION_PHASE2_NESTED] = settings.repetitionPhase2NestedID
            prefs[PreferenceKeys.REPETITION_PHASE2_BCT] = settings.repetitionPhase2BCT
            prefs[PreferenceKeys.REPETITION_PHASE3_VOCAB] = settings.repetitionPhase3Vocab
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
            wpm = this[PreferenceKeys.WPM] ?: UserSettings.DEFAULT_WPM,
            toneFrequencyHz = this[PreferenceKeys.TONE] ?: UserSettings.DEFAULT_TONE_HZ,
            phaseSelection = phaseString.split(',').mapNotNull { it.toIntOrNull() }.toSet()
                .ifEmpty { setOf(1, 2, 3, 4) },

            // Load all repetition counts with defaults
            repetitionPhase1NestedID = this[PreferenceKeys.REPETITION_PHASE1_NESTED]
                ?: UserSettings.DEFAULT_REPETITION_LETTERS,
            repetitionPhase1BCT = this[PreferenceKeys.REPETITION_PHASE1_BCT]
                ?: UserSettings.DEFAULT_REPETITION_BCT,
            repetitionPhase2NestedID = this[PreferenceKeys.REPETITION_PHASE2_NESTED]
                ?: UserSettings.DEFAULT_REPETITION_NUMBERS,
            repetitionPhase2BCT = this[PreferenceKeys.REPETITION_PHASE2_BCT]
                ?: UserSettings.DEFAULT_REPETITION_BCT_MIX,
            repetitionPhase3Vocab = this[PreferenceKeys.REPETITION_PHASE3_VOCAB]
                ?: UserSettings.DEFAULT_REPETITION_VOCAB
        )
    }
}