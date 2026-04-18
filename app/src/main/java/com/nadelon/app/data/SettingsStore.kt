package com.nadelon.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val KEY_API = stringPreferencesKey("opensubs_api_key")
private val KEY_USER = stringPreferencesKey("opensubs_username")
private val KEY_PASS = stringPreferencesKey("opensubs_password")
private val KEY_TOKEN = stringPreferencesKey("opensubs_token")

data class Settings(
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val token: String = ""
)

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            apiKey = prefs[KEY_API].orEmpty(),
            username = prefs[KEY_USER].orEmpty(),
            password = prefs[KEY_PASS].orEmpty(),
            token = prefs[KEY_TOKEN].orEmpty()
        )
    }

    suspend fun update(apiKey: String, username: String, password: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_API] = apiKey.trim()
            prefs[KEY_USER] = username.trim()
            prefs[KEY_PASS] = password
            prefs.remove(KEY_TOKEN)
        }
    }

    suspend fun saveToken(token: String) {
        context.settingsDataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun clearToken() {
        context.settingsDataStore.edit { it.remove(KEY_TOKEN) }
    }
}
