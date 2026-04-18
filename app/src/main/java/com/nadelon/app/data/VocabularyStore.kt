package com.nadelon.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nadelon.app.model.VocabEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.vocabDataStore by preferencesDataStore(name = "vocabulary")
private val VOCAB_KEY = stringPreferencesKey("entries_json")

class VocabularyStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(VocabEntry.serializer())

    val entries: Flow<List<VocabEntry>> = context.vocabDataStore.data.map { prefs ->
        val raw = prefs[VOCAB_KEY] ?: return@map emptyList()
        runCatching { json.decodeFromString(serializer, raw) }.getOrElse { emptyList() }
    }

    suspend fun add(entry: VocabEntry) {
        context.vocabDataStore.edit { prefs ->
            val current = prefs[VOCAB_KEY]?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrElse { emptyList() }
            } ?: emptyList()
            val deduped = current.filterNot { it.key == entry.key }
            val next = listOf(entry) + deduped
            prefs[VOCAB_KEY] = json.encodeToString(serializer, next)
        }
    }

    suspend fun remove(key: String) {
        context.vocabDataStore.edit { prefs ->
            val current = prefs[VOCAB_KEY]?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrElse { emptyList() }
            } ?: emptyList()
            val next = current.filterNot { it.key == key }
            prefs[VOCAB_KEY] = json.encodeToString(serializer, next)
        }
    }

    suspend fun clear() {
        context.vocabDataStore.edit { prefs ->
            prefs[VOCAB_KEY] = json.encodeToString(serializer, emptyList())
        }
    }
}
