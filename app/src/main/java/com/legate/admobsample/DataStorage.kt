package com.legate.admobsample

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

object DataStorage {
    private val APP_OPEN_COUNT = intPreferencesKey("app_open_count")

    fun getAppOpenCount(context: Context): Flow<Int> {
        return context.dataStore.data.map { prefs -> prefs[APP_OPEN_COUNT] ?: 0 }
    }

    suspend fun incrementAppOpenCount(context: Context) {
        context.dataStore.edit { prefs ->
            val currentCount = prefs[APP_OPEN_COUNT] ?: 0
            prefs[APP_OPEN_COUNT] = currentCount + 1
        }
    }
}