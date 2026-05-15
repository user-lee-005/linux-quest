package com.linuxquest.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val FONT_SIZE = intPreferencesKey("font_size")
        val SHOW_HINTS = booleanPreferencesKey("show_hints")
        val VIBRATION = booleanPreferencesKey("vibration")
        val CURRENT_LEVEL = intPreferencesKey("current_level")
    }

    val fontSize: Flow<Int> = context.dataStore.data.map { it[Keys.FONT_SIZE] ?: 14 }
    val showHints: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_HINTS] ?: true }
    val vibration: Flow<Boolean> = context.dataStore.data.map { it[Keys.VIBRATION] ?: true }
    val currentLevel: Flow<Int> = context.dataStore.data.map { it[Keys.CURRENT_LEVEL] ?: 0 }

    suspend fun setFontSize(size: Int) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size.coerceIn(10, 24) }
    }

    suspend fun setShowHints(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HINTS] = show }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setCurrentLevel(level: Int) {
        context.dataStore.edit { it[Keys.CURRENT_LEVEL] = level }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
