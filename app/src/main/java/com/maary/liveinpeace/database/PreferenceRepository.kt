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
import com.maary.liveinpeace.Constants.Companion.MODE_IMG
import com.maary.liveinpeace.Constants.Companion.MODE_NUM
import com.maary.liveinpeace.Constants.Companion.PREF_ENABLE_EAR_PROTECTION
import com.maary.liveinpeace.Constants.Companion.PREF_ICON
import com.maary.liveinpeace.Constants.Companion.PREF_SERVICE_RUNNING
import com.maary.liveinpeace.Constants.Companion.PREF_WATCHING_CONNECTING_TIME
import com.maary.liveinpeace.Constants.Companion.PREF_WELCOME_FINISHED
import com.maary.liveinpeace.Constants.Companion.SHARED_PREF
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
        val PREF_HIDE_IN_LAUNCHER = booleanPreferencesKey(Constants.PREF_HIDE_IN_LAUNCHER)
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

    fun isHideInLauncher() : Flow<Boolean> {
        return datastore.data.map { pref ->
            pref[PREF_HIDE_IN_LAUNCHER] ?: false
        }
    }

    suspend fun setHideInLauncher(state: Boolean) {
        datastore.edit { pref ->
            pref[PREF_HIDE_IN_LAUNCHER] = state
        }
    }
}