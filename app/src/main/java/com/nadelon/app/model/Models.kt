package com.nadelon.app.model

import kotlinx.serialization.Serializable

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

@Serializable
data class VocabEntry(
    val term: String,
    val translation: String,
    val sourceLang: String,
    val targetLang: String,
    val context: String = "",
    val savedAtMs: Long = System.currentTimeMillis()
) {
    val key: String get() = "${sourceLang}:${term.lowercase()}"
}
