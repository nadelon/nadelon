package com.nadelon.chess.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ChessComException(message: String) : Exception(message)

/** One downloaded game: its PGN plus the chess.com URL. */
class RawGame(val pgn: String, val url: String, val timeClass: String, val rated: Boolean)

/**
 * Read-only client for the public chess.com API (no auth required). The API is
 * CORS-enabled and works from devices; it expects a descriptive User-Agent.
 */
class ChessComApi {

    private val ua = "ChessCoach/0.1 (Android; analysis app)"

    suspend fun profileExists(username: String): Boolean = withContext(Dispatchers.IO) {
        val u = username.trim().lowercase()
        if (u.isEmpty()) return@withContext false
        try {
            getJson("https://api.chess.com/pub/player/$u")
            true
        } catch (e: ChessComException) {
            false
        }
    }

    /** Monthly archive URLs, oldest first. */
    suspend fun archives(username: String): List<String> = withContext(Dispatchers.IO) {
        val u = username.trim().lowercase()
        val json = getJson("https://api.chess.com/pub/player/$u/games/archives")
        val arr = json.optJSONArray("archives") ?: return@withContext emptyList()
        (0 until arr.length()).map { arr.getString(it) }
    }

    /** Standard-chess games from a single monthly archive, in API order. */
    suspend fun gamesFrom(archiveUrl: String): List<RawGame> = withContext(Dispatchers.IO) {
        val json = getJson(archiveUrl)
        val arr = json.optJSONArray("games") ?: return@withContext emptyList()
        val out = ArrayList<RawGame>(arr.length())
        for (i in 0 until arr.length()) {
            val g = arr.getJSONObject(i)
            if (g.optString("rules", "chess") != "chess") continue // skip variants
            val pgn = g.optString("pgn")
            if (pgn.isNullOrBlank()) continue
            out.add(
                RawGame(
                    pgn = pgn,
                    url = g.optString("url"),
                    timeClass = g.optString("time_class"),
                    rated = g.optBoolean("rated", false)
                )
            )
        }
        out
    }

    private fun getJson(urlStr: String): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 20000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", ua)
        }
        try {
            val code = conn.responseCode
            if (code == 404) throw ChessComException("Player or resource not found.")
            if (code == 429) throw ChessComException("Rate limited by chess.com — try again shortly.")
            if (code !in 200..299) throw ChessComException("chess.com returned HTTP $code.")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } catch (e: ChessComException) {
            throw e
        } catch (t: Throwable) {
            throw ChessComException("Network error: ${t.message ?: t.javaClass.simpleName}")
        } finally {
            conn.disconnect()
        }
    }
}
