package com.nadelon.chess.analysis

import com.nadelon.chess.core.Board
import com.nadelon.chess.core.ParsedGame
import com.nadelon.chess.engine.MoveQuality
import com.nadelon.chess.engine.MoveVerdict
import com.nadelon.chess.engine.Quality

/** One analysed half-move, with its position, engine verdict and features. */
class AnalyzedPly(
    val ply: Int,
    val side: Int,
    val san: String,
    val fenBefore: String,
    val fenAfter: String,
    val fromSq: Int,
    val toSq: Int,
    val verdict: MoveVerdict?,
    val features: PositionFeatures
)

enum class PlayerResult { WIN, LOSS, DRAW, UNKNOWN }

class GameReport(
    val white: String,
    val black: String,
    val result: String,
    val date: String,
    val timeControl: String,
    val playerColor: Int,
    val opponent: String,
    val playerElo: Int?,
    val opponentElo: Int?,
    val opening: OpeningId,
    val plies: List<AnalyzedPly>,
    val themeHits: List<ThemeHit>,
    val accuracy: Double,
    val inaccuracies: Int,
    val mistakes: Int,
    val blunders: Int,
    val outcome: PlayerResult
) {
    val moveCount: Int get() = (plies.maxOfOrNull { it.ply } ?: -1) / 2 + 1
}

class AnalyzerConfig(
    val useEngine: Boolean = true,
    val depth: Int = 3,
    /** Skip engine work on the first N full moves (book territory). */
    val skipOpeningMoves: Int = 4
)

class GameAnalyzer(private val config: AnalyzerConfig = AnalyzerConfig()) {

    /** Returns null if the username does not appear in this game. */
    fun analyze(game: ParsedGame, username: String): GameReport? {
        val user = username.trim().lowercase()
        val playerColor = when (user) {
            game.white.lowercase() -> Board.WHITE
            game.black.lowercase() -> Board.BLACK
            else -> return null
        }
        val outcome = outcomeFor(game.result, playerColor)

        val analyzed = ArrayList<AnalyzedPly>(game.plies.size)
        var lossSum = 0.0
        var lossCount = 0
        var inacc = 0; var mist = 0; var blun = 0

        for (p in game.plies) {
            val board = Board.fromFen(p.fenBefore)
            val features = Features.extract(board)
            var verdict: MoveVerdict? = null
            val isPlayer = p.sideToMove == playerColor
            val pastBook = (p.ply / 2) + 1 > config.skipOpeningMoves
            if (config.useEngine && isPlayer && pastBook) {
                verdict = MoveQuality.classify(board, p.move, config.depth)
                lossSum += accuracyForLoss(verdict.winLoss)
                lossCount++
                when (verdict.quality) {
                    Quality.INACCURACY -> inacc++
                    Quality.MISTAKE -> mist++
                    Quality.BLUNDER -> blun++
                    else -> {}
                }
            }
            analyzed.add(
                AnalyzedPly(
                    p.ply, p.sideToMove, p.san, p.fenBefore, p.fenAfter,
                    p.move.from, p.move.to, verdict, features
                )
            )
        }

        val hits = Themes.detect(
            playerColor, analyzed,
            playerWon = outcome == PlayerResult.WIN,
            playerDrew = outcome == PlayerResult.DRAW
        )
        val accuracy = if (lossCount > 0) lossSum / lossCount else Double.NaN

        return GameReport(
            white = game.white,
            black = game.black,
            result = game.result,
            date = game.date,
            timeControl = game.timeControl,
            playerColor = playerColor,
            opponent = if (playerColor == Board.WHITE) game.black else game.white,
            playerElo = if (playerColor == Board.WHITE) game.whiteElo else game.blackElo,
            opponentElo = if (playerColor == Board.WHITE) game.blackElo else game.whiteElo,
            opening = OpeningBook.identify(game),
            plies = analyzed,
            themeHits = hits,
            accuracy = accuracy,
            inaccuracies = inacc,
            mistakes = mist,
            blunders = blun,
            outcome = outcome
        )
    }

    private fun outcomeFor(result: String, color: Int): PlayerResult = when (result) {
        "1-0" -> if (color == Board.WHITE) PlayerResult.WIN else PlayerResult.LOSS
        "0-1" -> if (color == Board.BLACK) PlayerResult.WIN else PlayerResult.LOSS
        "1/2-1/2" -> PlayerResult.DRAW
        else -> PlayerResult.UNKNOWN
    }

    /** Per-move accuracy% from win-probability loss, per the lichess curve. */
    private fun accuracyForLoss(winLoss: Double): Double {
        val dropPct = (winLoss * 100.0).coerceIn(0.0, 100.0)
        val acc = 103.1668 * Math.exp(-0.04354 * dropPct) - 3.1669
        return acc.coerceIn(0.0, 100.0)
    }
}
