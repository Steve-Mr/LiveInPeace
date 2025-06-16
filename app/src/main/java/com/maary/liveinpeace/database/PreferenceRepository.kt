package com.maary.liveinpeace.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maary.liveinpeace.Constants
import com.maary.liveinpeace.Constants.Companion.SHARED_PREF
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.datastore: DataStore<Preferences> by preferencesDataStore(
    name = "live_in_peace_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, SHARED_PREF))
    }
)

class PreferenceRepository @Inject constructor(@ApplicationContext context: Context) {

    private val datastore = context.datastore

    companion object {
        val PREF_WATCHING_CONNECTING_TIME = booleanPreferencesKey(Constants.PREF_WATCHING_CONNECTING_TIME)
        val PREF_ENABLE_EAR_PROTECTION = booleanPreferencesKey(Constants.PREF_ENABLE_EAR_PROTECTION)
        val PREF_WELCOME_FINISHED = booleanPreferencesKey(Constants.PREF_WELCOME_FINISHED)
        val PREF_SERVICE_RUNNING = booleanPreferencesKey(Constants.PREF_SERVICE_RUNNING)
        val PREF_VISIBLE_IN_LAUNCHER = booleanPreferencesKey(Constants.PREF_HIDE_IN_LAUNCHER)
        val PREF_EAR_PROTECTION_THRESHOLD_MAX = intPreferencesKey(Constants.PREF_EAR_PROTECTION_THRESHOLD_MAX)
        val PREF_EAR_PROTECTION_THRESHOLD_MIN = intPreferencesKey(Constants.PREF_EAR_PROTECTION_THRESHOLD_MIN)
    }

    fun getWatchingState(): Flow<Boolean> {
        return datastore.data.map { pref ->
            pref[PREF_WATCHING_CONNECTING_TIME] ?: false
        }
    }

    suspend fun setWatchingState(state: Boolean) {
        datastore.edit { pref ->
            pref[PREF_WATCHING_CONNECTING_TIME] = state
        }
    }

    fun isEarProtectionOn() : Flow<Boolean> {
        return datastore.data.map { pref ->
            pref[PREF_ENABLE_EAR_PROTECTION] ?: false
        }
    }

    suspend fun setEarProtection(state: Boolean) {
        datastore.edit { pref ->
            pref[PREF_ENABLE_EAR_PROTECTION] = state
        }
    }

    fun isWelcomeFinished(): Flow<Boolean> {
        return datastore.data.map { pref ->
            pref[PREF_WELCOME_FINISHED] ?: false
        }
    }

    suspend fun setWelcomeFinished(state: Boolean) {
        datastore.edit { pref ->
            pref[PREF_WELCOME_FINISHED] = state
        }
    }

    fun isServiceRunning() : Flow<Boolean> {
        return datastore.data.map { pref ->
            pref[PREF_SERVICE_RUNNING] ?: false
        }
    }

    suspend fun setServiceRunning(state: Boolean) {
        datastore.edit { pref ->
            pref[PREF_SERVICE_RUNNING] = state
        }
    }

    fun isIconShown() : Flow<Boolean> {
        return datastore.data.map { pref ->
            pref[PREF_VISIBLE_IN_LAUNCHER] ?: false
        }
    }

    suspend fun toggleIconVisibility() {
        datastore.edit { pref ->
            val currentState = pref[PREF_VISIBLE_IN_LAUNCHER] ?: false
            pref[PREF_VISIBLE_IN_LAUNCHER] = !currentState
        }
    }

    private fun getEarProtectionThresholdMax() : Flow<Int> {
        return datastore.data.map { pref ->
            pref[PREF_EAR_PROTECTION_THRESHOLD_MAX] ?: Constants.EAR_PROTECTION_UPPER_THRESHOLD
        }
    }

    private fun getEarProtectionThresholdMin() : Flow<Int> {
        return datastore.data.map { pref ->
            pref[PREF_EAR_PROTECTION_THRESHOLD_MIN] ?: Constants.EAR_PROTECTION_LOWER_THRESHOLD
        }
    }

    fun getEarProtectionThreshold(): Flow<IntRange> {
        return combine(
            getEarProtectionThresholdMin(),
            getEarProtectionThresholdMax()
        ) { min, max ->
            min..max
        }
    }

    suspend fun setEarProtectionThreshold(range: IntRange) {
        datastore.edit { pref ->
            pref[PREF_EAR_PROTECTION_THRESHOLD_MIN] = range.first
            pref[PREF_EAR_PROTECTION_THRESHOLD_MAX] = range.last
        }
    }
}