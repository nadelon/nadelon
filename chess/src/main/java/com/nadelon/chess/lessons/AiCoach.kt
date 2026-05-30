package com.nadelon.chess.lessons

import com.nadelon.chess.analysis.Aggregate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional enrichment layer: if the user supplies a Claude API key, send the
 * aggregated analysis to the Claude API and ask for richer, personalised
 * theoretical lessons. Any failure (no key, network, parse) falls back to the
 * built-in [LessonEngine] lessons, so the app is fully functional without a key.
 */
object AiCoach {

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    const val DEFAULT_MODEL = "claude-sonnet-4-6"

    @Serializable private class AiLesson(
        val title: String = "",
        val category: String = "",
        val summary: String = "",
        val theory: List<String> = emptyList(),
        val principles: List<String> = emptyList(),
        val drills: List<String> = emptyList()
    )

    @Serializable private class AiResponse(val lessons: List<AiLesson> = emptyList())

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Returns AI-generated lessons, or [fallback] if the key is blank or anything
     * goes wrong. Examples from the analysis are attached so the boards still show.
     */
    suspend fun coach(
        apiKey: String,
        agg: Aggregate,
        fallback: List<Lesson>,
        model: String = DEFAULT_MODEL
    ): List<Lesson> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext fallback
        try {
            val text = request(apiKey, model, buildPrompt(agg)) ?: return@withContext fallback
            val parsed = json.decodeFromString(AiResponse.serializer(), extractJson(text))
            if (parsed.lessons.isEmpty()) return@withContext fallback
            val exampleSource = agg.themeStats
            parsed.lessons.mapIndexed { i, l ->
                Lesson(
                    id = "ai_$i",
                    title = l.title.ifBlank { "Coaching focus ${i + 1}" },
                    category = l.category.ifBlank { "Coaching" },
                    summary = l.summary,
                    theory = l.theory,
                    principles = l.principles,
                    drills = l.drills,
                    examples = exampleSource.getOrNull(i)?.examples ?: emptyList(),
                    priority = 1000 - i,
                    source = "AI coach"
                )
            }
        } catch (t: Throwable) {
            fallback
        }
    }

    private fun buildPrompt(agg: Aggregate): String = buildString {
        appendLine("Here is an automated analysis of a chess player's games from chess.com.")
        appendLine("Act as a strong, encouraging chess coach. Based ONLY on this data, write a")
        appendLine("personalised improvement plan as a set of theoretical lessons (not puzzles).")
        appendLine("Each lesson must teach the underlying principle clearly and tie it to the player's")
        appendLine("specific tendencies. Cover positional and strategic ideas, not just tactics.")
        appendLine()
        appendLine(LessonEngine.studyPlanSummary(agg))
        appendLine()
        appendLine("Example positions illustrating the issues (FEN — description):")
        agg.themeStats.take(5).forEach { st ->
            st.examples.firstOrNull()?.let { ex ->
                appendLine("- ${st.theme.title}: ${ex.fen}  — ${ex.detail}")
            }
        }
        appendLine()
        appendLine("Respond with ONLY valid JSON in exactly this shape, no prose, no code fences:")
        appendLine("""{"lessons":[{"title":"...","category":"...","summary":"...",""")
        appendLine(""""theory":["paragraph",...],"principles":["...",...],"drills":["...",...]}]}""")
        appendLine("Write 4-6 lessons, ordered by importance. Theory entries are full paragraphs.")
    }

    private fun request(apiKey: String, model: String, prompt: String): String? {
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 3000)
            put("system", "You are a world-class chess coach who explains strategy clearly.")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }.toString()

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("content-type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.errorStream?.bufferedReader()?.use { it.readText() }
                return null
            }
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONObject(resp).optJSONArray("content") ?: return null
            val sb = StringBuilder()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                if (item.optString("type") == "text") sb.append(item.optString("text"))
            }
            return sb.toString().ifBlank { null }
        } finally {
            conn.disconnect()
        }
    }

    /** Pull the JSON object out of a possibly fenced / chatty response. */
    private fun extractJson(text: String): String {
        val t = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        return if (start >= 0 && end > start) t.substring(start, end + 1) else t
    }
}
