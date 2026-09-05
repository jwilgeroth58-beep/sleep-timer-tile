package com.jwilgeroth.sleeptimertile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// One DataStore file named "settings" for the whole app, hung off Context.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persisted user settings, backed by DataStore.
 *
 * DataStore is asynchronous (its natural API is a Flow / suspend functions).
 * The UI writes via the suspend setters. But the tile and the alarm receiver
 * are short-lived and need a value immediately, so for reads they use a brief
 * runBlocking to pull the latest snapshot. A tiny preferences read is fast; this
 * is a pragmatic, well-contained use of runBlocking, not a general pattern.
 */
object Settings {
    const val DEFAULT_DURATION_MINUTES = 30
    const val DEFAULT_PAUSE_MEDIA = true
    const val DEFAULT_LOCK_SCREEN = true

    private val KEY_DURATION = intPreferencesKey("duration_minutes")
    private val KEY_PAUSE = booleanPreferencesKey("pause_media")
    private val KEY_LOCK = booleanPreferencesKey("lock_screen")

    // --- Synchronous reads (tile + receiver) --------------------------------
    fun durationMinutes(context: Context): Int =
        read(context) { it[KEY_DURATION] ?: DEFAULT_DURATION_MINUTES }

    fun pauseMedia(context: Context): Boolean =
        read(context) { it[KEY_PAUSE] ?: DEFAULT_PAUSE_MEDIA }

    fun lockScreen(context: Context): Boolean =
        read(context) { it[KEY_LOCK] ?: DEFAULT_LOCK_SCREEN }

    // --- Async writes (settings UI) -----------------------------------------
    suspend fun setDurationMinutes(context: Context, value: Int) {
        context.dataStore.edit { it[KEY_DURATION] = value }
    }

    suspend fun setPauseMedia(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_PAUSE] = value }
    }

    suspend fun setLockScreen(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_LOCK] = value }
    }

    private fun <T> read(context: Context, block: (Preferences) -> T): T =
        runBlocking { block(context.dataStore.data.first()) }
}
