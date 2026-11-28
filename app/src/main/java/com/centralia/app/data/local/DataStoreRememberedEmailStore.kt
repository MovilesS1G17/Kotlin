package com.centralia.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.centralia.app.domain.auth.RememberedEmailStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** One DataStore file per process: `files/datastore/centralia_login.preferences_pb`. */
private val Context.loginPreferences: DataStore<Preferences> by preferencesDataStore(name = "centralia_login")

/**
 * Local storage (Sprint 3, Alex): Jetpack DataStore (Preferences).
 *
 * Why DataStore here and SharedPreferences for the profile copy:
 * - DataStore is the newer key-value store. Reads and writes are suspend
 *   functions that run on Dispatchers.IO by themselves, so it can never block
 *   the main thread (SharedPreferences' first read can).
 * - `edit { }` is a transaction: the value is written completely or not at all,
 *   and errors come back as exceptions instead of being lost (as with `apply()`).
 * - Its data is a Flow, so a screen could also observe the value.
 * The profile copy stayed on SharedPreferences because it was already there in
 * the offline-first layer; this is a new piece of state, so it uses DataStore.
 */
class DataStoreRememberedEmailStore(private val context: Context) : RememberedEmailStore {

    override suspend fun email(): String? =
        context.loginPreferences.data.map { it[KEY_EMAIL] }.first()

    override suspend fun remember(email: String) {
        context.loginPreferences.edit { it[KEY_EMAIL] = email }
    }

    override suspend fun forget() {
        context.loginPreferences.edit { it.remove(KEY_EMAIL) }
    }

    private companion object {
        val KEY_EMAIL = stringPreferencesKey("remembered_email")
    }
}
