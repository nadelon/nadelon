package com.nadelon.chess.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chessSettings by preferencesDataStore(name = "chess_coach_settings")

class Settings(
    val username: String,
    val apiKey: String,
    val maxGames: Int,
    val depth: Int,
    val useEngine: Boolean,
    val useAi: Boolean
)

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.chessSettings.data.map { p ->
        Settings(
            username = p[USERNAME] ?: "",
            apiKey = p[API_KEY] ?: "",
            maxGames = p[MAX_GAMES] ?: 40,
            depth = p[DEPTH] ?: 3,
            useEngine = p[USE_ENGINE] ?: true,
            useAi = p[USE_AI] ?: false
        )
    }

    suspend fun setUsername(v: String) = edit { it[USERNAME] = v.trim() }
    suspend fun setApiKey(v: String) = edit { it[API_KEY] = v.trim() }
    suspend fun setMaxGames(v: Int) = edit { it[MAX_GAMES] = v }
    suspend fun setDepth(v: Int) = edit { it[DEPTH] = v }
    suspend fun setUseEngine(v: Boolean) = edit { it[USE_ENGINE] = v }
    suspend fun setUseAi(v: Boolean) = edit { it[USE_AI] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.chessSettings.edit(block)
    }

    companion object {
        private val USERNAME = stringPreferencesKey("username")
        private val API_KEY = stringPreferencesKey("api_key")
        private val MAX_GAMES = intPreferencesKey("max_games")
        private val DEPTH = intPreferencesKey("depth")
        private val USE_ENGINE = booleanPreferencesKey("use_engine")
        private val USE_AI = booleanPreferencesKey("use_ai")
    }
}
