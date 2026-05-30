package com.nadelon.chess.analysis

import com.nadelon.chess.core.Board

class OpeningStat(
    val family: String,
    val games: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val avgAccuracy: Double?,   // null if not engine-analysed
    val asWhite: Int,
    val asBlack: Int
) {
    val scorePct: Double get() = if (games == 0) 0.0 else (wins + 0.5 * draws) / games * 100.0
}

class ThemeExample(
    val fen: String,
    val detail: String,
    val gameLabel: String,
    val plyIndex: Int
)

class ThemeStat(
    val theme: ThemeKey,
    val gamesAffected: Int,
    val totalGames: Int,
    val avgSeverity: Double,
    val examples: List<ThemeExample>
) {
    val frequencyPct: Double get() = if (totalGames == 0) 0.0 else gamesAffected * 100.0 / totalGames
}

class PhaseAccuracy(val opening: Double?, val middlegame: Double?, val endgame: Double?)

class Aggregate(
    val totalGames: Int,
    val analyzedWithEngine: Boolean,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val overallAccuracy: Double?,
    val byOpening: List<OpeningStat>,
    val themeStats: List<ThemeStat>,
    val phase: PhaseAccuracy,
    val totalInaccuracies: Int,
    val totalMistakes: Int,
    val totalBlunders: Int,
    val username: String
) {
    val winRate: Double get() = if (totalGames == 0) 0.0 else wins * 100.0 / totalGames

    fun bestOpenings(minGames: Int = 3): List<OpeningStat> =
        byOpening.filter { it.games >= minGames }.sortedByDescending { it.scorePct }.take(5)

    fun worstOpenings(minGames: Int = 3): List<OpeningStat> =
        byOpening.filter { it.games >= minGames }.sortedBy { it.scorePct }.take(5)
}

object AggregateBuilder {

    fun build(username: String, reports: List<GameReport>): Aggregate {
        val wins = reports.count { it.outcome == PlayerResult.WIN }
        val draws = reports.count { it.outcome == PlayerResult.DRAW }
        val losses = reports.count { it.outcome == PlayerResult.LOSS }
        val engineUsed = reports.any { !it.accuracy.isNaN() }

        // Opening grouping by family.
        val byFamily = reports.groupBy { it.opening.family }
        val openings = byFamily.map { (family, gs) ->
            val accs = gs.mapNotNull { if (it.accuracy.isNaN()) null else it.accuracy }
            OpeningStat(
                family = family,
                games = gs.size,
                wins = gs.count { it.outcome == PlayerResult.WIN },
                draws = gs.count { it.outcome == PlayerResult.DRAW },
                losses = gs.count { it.outcome == PlayerResult.LOSS },
                avgAccuracy = if (accs.isEmpty()) null else accs.average(),
                asWhite = gs.count { it.playerColor == Board.WHITE },
                asBlack = gs.count { it.playerColor == Board.BLACK }
            )
        }.sortedByDescending { it.games }

        // Theme aggregation.
        val themeStats = ThemeKey.entries.mapNotNull { theme ->
            val hitsByGame = reports.mapNotNull { r ->
                r.themeHits.filter { it.theme == theme }
                    .maxByOrNull { it.severity }?.let { r to it }
            }
            if (hitsByGame.isEmpty()) return@mapNotNull null
            val examples = hitsByGame.sortedByDescending { it.second.severity }.take(4).map { (r, h) ->
                ThemeExample(
                    fen = h.fen,
                    detail = h.detail,
                    gameLabel = "vs ${r.opponent}${if (r.date.isNotBlank()) " (${r.date})" else ""}",
                    plyIndex = h.plyIndex
                )
            }
            ThemeStat(
                theme = theme,
                gamesAffected = hitsByGame.size,
                totalGames = reports.size,
                avgSeverity = hitsByGame.map { it.second.severity }.average(),
                examples = examples
            )
        }.sortedWith(compareByDescending<ThemeStat> { it.gamesAffected }.thenByDescending { it.avgSeverity })

        // Phase accuracy from player plies with verdicts.
        val phase = phaseAccuracy(reports)
        val overall = reports.mapNotNull { if (it.accuracy.isNaN()) null else it.accuracy }
            .let { if (it.isEmpty()) null else it.average() }

        return Aggregate(
            totalGames = reports.size,
            analyzedWithEngine = engineUsed,
            wins = wins, draws = draws, losses = losses,
            overallAccuracy = overall,
            byOpening = openings,
            themeStats = themeStats,
            phase = phase,
            totalInaccuracies = reports.sumOf { it.inaccuracies },
            totalMistakes = reports.sumOf { it.mistakes },
            totalBlunders = reports.sumOf { it.blunders },
            username = username
        )
    }

    private fun phaseAccuracy(reports: List<GameReport>): PhaseAccuracy {
        val open = ArrayList<Double>(); val mid = ArrayList<Double>(); val end = ArrayList<Double>()
        for (r in reports) {
            for (p in r.plies) {
                val v = p.verdict ?: continue
                if (p.side != r.playerColor) continue
                val moveNo = p.ply / 2 + 1
                val acc = 103.1668 * Math.exp(-0.04354 * (v.winLoss * 100.0)) - 3.1669
                val a = acc.coerceIn(0.0, 100.0)
                when {
                    moveNo <= 12 -> open.add(a)
                    moveNo <= 30 -> mid.add(a)
                    else -> end.add(a)
                }
            }
        }
        fun avg(l: List<Double>) = if (l.isEmpty()) null else l.average()
        return PhaseAccuracy(avg(open), avg(mid), avg(end))
    }
}
