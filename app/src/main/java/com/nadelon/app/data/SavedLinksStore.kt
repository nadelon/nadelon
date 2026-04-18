package com.nadelon.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nadelon.app.model.SavedLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.linksDataStore by preferencesDataStore(name = "saved_links")
private val LINKS_KEY = stringPreferencesKey("links_json")

class SavedLinksStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(SavedLink.serializer())

    val links: Flow<List<SavedLink>> = context.linksDataStore.data.map { prefs ->
        val raw = prefs[LINKS_KEY] ?: return@map emptyList()
        runCatching { json.decodeFromString(serializer, raw) }.getOrElse { emptyList() }
    }

    suspend fun save(link: SavedLink) {
        context.linksDataStore.edit { prefs ->
            val current = prefs[LINKS_KEY]?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrElse { emptyList() }
            } ?: emptyList()
            val deduped = current.filterNot { it.url == link.url }
            prefs[LINKS_KEY] = json.encodeToString(serializer, listOf(link) + deduped)
        }
    }

    suspend fun remove(url: String) {
        context.linksDataStore.edit { prefs ->
            val current = prefs[LINKS_KEY]?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrElse { emptyList() }
            } ?: emptyList()
            prefs[LINKS_KEY] = json.encodeToString(serializer, current.filterNot { it.url == url })
        }
    }
}
