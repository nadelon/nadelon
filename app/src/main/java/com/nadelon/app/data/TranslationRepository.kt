package com.nadelon.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TranslationRepository {

    private val cache = LinkedHashMap<String, String>(64, 0.75f, true)
    private val maxCache = 256

    suspend fun translate(
        term: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleaned = term.trim()
        if (cleaned.isEmpty()) return@withContext Result.failure(IllegalArgumentException("empty"))
        val key = "$sourceLang|$targetLang|${cleaned.lowercase()}"
        cache[key]?.let { return@withContext Result.success(it) }

        val src = if (sourceLang.isBlank() || sourceLang == "auto") "auto" else sourceLang
        val pair = "$src|$targetLang"
        val q = URLEncoder.encode(cleaned, "UTF-8")
        val urlStr = "https://api.mymemory.translated.net/get?q=$q&langpair=$pair"

        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Nadelon/0.1 (Android)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(RuntimeException("HTTP $code"))
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val responseData = json.optJSONObject("responseData")
            val translated = responseData?.optString("translatedText")?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(RuntimeException("No translation"))
            cache[key] = translated
            if (cache.size > maxCache) {
                val iterator = cache.entries.iterator()
                if (iterator.hasNext()) { iterator.next(); iterator.remove() }
            }
            Result.success(translated)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            conn.disconnect()
        }
    }
}
