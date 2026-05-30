package com.nadelon.chess.engine

import com.nadelon.chess.core.Board
import com.nadelon.chess.core.Move

enum class Quality { BEST, GOOD, INACCURACY, MISTAKE, BLUNDER }

/**
 * The engine's verdict on a single played move.
 * [cpBefore]/[cpAfter] are centipawns from the moving side's perspective.
 * [winLoss] is the drop in win probability (0..1) the move conceded.
 */
class MoveVerdict(
    val quality: Quality,
    val cpBefore: Int,
    val cpAfter: Int,
    val winLoss: Double,
    val bestMove: Move?,
    val bestSan: String?
)

object MoveQuality {

    /**
     * Classify [played] in [board] (side to move is the mover) using a search of
     * the given [depth]. The board is left unchanged.
     */
    fun classify(board: Board, played: Move, depth: Int): MoveVerdict {
        val search = Search(depth)
        val me = board.turn
        val sign = if (me == Board.WHITE) 1 else -1

        val before = search.search(board)            // White-POV score + best move
        val cpBeforeMover = before.score * sign      // mover-POV
        val bestSan = before.best?.let { board.san(it, board.legalMoves()) }

        val u = board.make(played)
        // search() already returns a White-POV score; convert to the mover's POV
        // with the mover's sign (the side to move is now the opponent).
        val after = Search(depth).search(board)
        val cpAfterMover = after.score * sign         // mover-POV
        board.unmake(u)

        val wBefore = Evaluation.winProbability(cpBeforeMover.coerceIn(-1500, 1500))
        val wAfter = Evaluation.winProbability(cpAfterMover.coerceIn(-1500, 1500))
        val loss = (wBefore - wAfter).coerceAtLeast(0.0)

        val isBest = before.best != null &&
            before.best.from == played.from && before.best.to == played.to &&
            before.best.promo == played.promo

        val quality = when {
            isBest || loss < 0.02 -> Quality.BEST
            loss < 0.07 -> Quality.GOOD
            loss < 0.14 -> Quality.INACCURACY
            loss < 0.25 -> Quality.MISTAKE
            else -> Quality.BLUNDER
        }
        return MoveVerdict(quality, cpBeforeMover, cpAfterMover, loss, before.best, bestSan)
    }
}
