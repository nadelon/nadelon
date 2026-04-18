package com.nadelon.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.resumeDataStore by preferencesDataStore(name = "resume")

class ResumeStore(private val context: Context) {

    suspend fun save(uriString: String, positionMs: Long) {
        context.resumeDataStore.edit { prefs ->
            prefs[key(uriString)] = positionMs.toString()
        }
    }

    suspend fun get(uriString: String): Long =
        context.resumeDataStore.data
            .map { prefs -> prefs[key(uriString)]?.toLongOrNull() ?: 0L }
            .firstOrNull() ?: 0L

    // Hash the URI so DataStore key is always short and safe
    private fun key(uri: String) = stringPreferencesKey("r_${uri.hashCode().toUInt()}")
}
