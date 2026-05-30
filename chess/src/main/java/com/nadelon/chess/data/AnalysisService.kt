package com.nadelon.chess.data

import com.nadelon.chess.analysis.Aggregate
import com.nadelon.chess.analysis.AggregateBuilder
import com.nadelon.chess.analysis.AnalyzerConfig
import com.nadelon.chess.analysis.GameAnalyzer
import com.nadelon.chess.analysis.GameReport
import com.nadelon.chess.core.Pgn
import com.nadelon.chess.lessons.AiCoach
import com.nadelon.chess.lessons.Lesson
import com.nadelon.chess.lessons.LessonEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Progress callback payload for the long-running analysis pipeline. */
class Progress(val phase: String, val fraction: Float)

/** Everything the UI needs once analysis completes. */
class AnalysisResult(
    val aggregate: Aggregate,
    val reports: List<GameReport>,
    val lessons: List<Lesson>
)

/**
 * Drives the full pipeline: download games from chess.com, analyse each one,
 * aggregate, and produce lessons (optionally enriched by the AI coach).
 */
class AnalysisService(
    private val api: ChessComApi = ChessComApi()
) {

    suspend fun run(
        settings: Settings,
        onProgress: suspend (Progress) -> Unit
    ): AnalysisResult = withContext(Dispatchers.Default) {
        val username = settings.username.trim()
        require(username.isNotEmpty()) { "Enter your chess.com username." }

        onProgress(Progress("Checking chess.com profile…", 0f))
        if (!api.profileExists(username)) {
            throw ChessComException("No chess.com player named \"$username\".")
        }

        onProgress(Progress("Finding your games…", 0.03f))
        val archives = api.archives(username).reversed() // newest month first
        val raw = ArrayList<RawGame>()
        for (archive in archives) {
            coroutineContext.ensureActive()
            val games = api.gamesFrom(archive).reversed() // newest game first
            raw.addAll(games)
            onProgress(
                Progress("Downloading games (${raw.size.coerceAtMost(settings.maxGames)})…", 0.08f)
            )
            if (raw.size >= settings.maxGames) break
        }
        val selected = raw.take(settings.maxGames)
        if (selected.isEmpty()) {
            throw ChessComException("No standard games found for \"$username\".")
        }

        val config = AnalyzerConfig(useEngine = settings.useEngine, depth = settings.depth)
        val analyzer = GameAnalyzer(config)
        val reports = ArrayList<GameReport>(selected.size)
        selected.forEachIndexed { i, rg ->
            coroutineContext.ensureActive()
            val parsed = Pgn.splitGames(rg.pgn).firstOrNull()?.let { Pgn.parse(it) }
            if (parsed != null) {
                analyzer.analyze(parsed, username)?.let { reports.add(it) }
            }
            val frac = 0.1f + 0.75f * ((i + 1).toFloat() / selected.size)
            onProgress(Progress("Analysing game ${i + 1} of ${selected.size}…", frac))
        }
        if (reports.isEmpty()) {
            throw ChessComException("Could not analyse any games for \"$username\".")
        }

        onProgress(Progress("Building your report…", 0.9f))
        val aggregate = AggregateBuilder.build(username, reports)
        var lessons = LessonEngine.build(aggregate)

        if (settings.useAi && settings.apiKey.isNotBlank()) {
            onProgress(Progress("Asking the AI coach for tailored lessons…", 0.95f))
            lessons = AiCoach.coach(settings.apiKey, aggregate, lessons)
        }

        onProgress(Progress("Done", 1f))
        AnalysisResult(aggregate, reports, lessons)
    }
}
