package com.nadelon.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class OpenSubResult(
    val fileId: Long,
    val language: String,
    val release: String,
    val fileName: String,
    val downloads: Int,
    val fromTrusted: Boolean,
    val featureTitle: String
)

sealed class OpenSubError(message: String) : RuntimeException(message) {
    object MissingApiKey : OpenSubError("Set your OpenSubtitles API key in Settings.")
    object NotLoggedIn : OpenSubError("Log in with your OpenSubtitles account in Settings to download.")
    class Http(val code: Int, val body: String) : OpenSubError("HTTP $code: $body")
    class Unexpected(message: String) : OpenSubError(message)
}

class OpenSubtitlesRepository(
    private val settings: SettingsStore,
    private val userAgent: String = "Nadelon v0.1"
) {

    private val base = "https://api.opensubtitles.com/api/v1"

    suspend fun search(
        query: String?,
        language: String?,
        apiKey: String
    ): Result<List<OpenSubResult>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(OpenSubError.MissingApiKey)
        val params = mutableListOf<String>()
        if (!query.isNullOrBlank()) params += "query=" + URLEncoder.encode(query, "UTF-8")
        if (!language.isNullOrBlank() && language != "auto") {
            params += "languages=" + URLEncoder.encode(language, "UTF-8")
        }
        params += "order_by=download_count"
        val url = "$base/subtitles?" + params.joinToString("&")
        runCatching {
            val body = getJson(url, apiKey, token = null)
            parseResults(body)
        }
    }

    suspend fun login(apiKey: String, username: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext Result.failure(OpenSubError.MissingApiKey)
            val payload = JSONObject().apply {
                put("username", username)
                put("password", password)
            }
            runCatching {
                val response = postJson("$base/login", apiKey, token = null, body = payload)
                val token = response.optString("token")
                if (token.isBlank()) throw OpenSubError.Unexpected("No token in login response")
                settings.saveToken(token)
                token
            }
        }

    suspend fun downloadSubtitleText(
        fileId: Long,
        apiKey: String,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(OpenSubError.MissingApiKey)
        if (token.isBlank()) return@withContext Result.failure(OpenSubError.NotLoggedIn)
        val payload = JSONObject().put("file_id", fileId)
        runCatching {
            val response = postJson("$base/download", apiKey, token = token, body = payload)
            val link = response.optString("link")
            if (link.isBlank()) throw OpenSubError.Unexpected("No download link returned")
            fetchText(link)
        }
    }

    private fun parseResults(body: JSONObject): List<OpenSubResult> {
        val data = body.optJSONArray("data") ?: return emptyList()
        val out = ArrayList<OpenSubResult>(data.length())
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val attrs = item.optJSONObject("attributes") ?: continue
            val files = attrs.optJSONArray("files") ?: continue
            if (files.length() == 0) continue
            val file = files.optJSONObject(0) ?: continue
            val feature = attrs.optJSONObject("feature_details")
            out += OpenSubResult(
                fileId = file.optLong("file_id"),
                language = attrs.optString("language", ""),
                release = attrs.optString("release", ""),
                fileName = file.optString("file_name", ""),
                downloads = attrs.optInt("download_count", 0),
                fromTrusted = attrs.optBoolean("from_trusted", false),
                featureTitle = feature?.optString("movie_name", "")
                    ?: feature?.optString("title", "").orEmpty()
            )
        }
        return out
    }

    private fun getJson(url: String, apiKey: String, token: String?): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Api-Key", apiKey)
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        return readJson(conn)
    }

    private fun postJson(url: String, apiKey: String, token: String?, body: JSONObject): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Api-Key", apiKey)
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        return readJson(conn)
    }

    private fun readJson(conn: HttpURLConnection): JSONObject {
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw OpenSubError.Http(code, text.take(400))
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchText(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 20000
            setRequestProperty("User-Agent", userAgent)
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw OpenSubError.Http(code, err.take(400))
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
