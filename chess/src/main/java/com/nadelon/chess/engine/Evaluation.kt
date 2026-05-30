package com.nadelon.chess.engine

import com.nadelon.chess.core.Board
import com.nadelon.chess.core.fileOf
import com.nadelon.chess.core.rankOf
import com.nadelon.chess.core.sqOf

/**
 * Hand-crafted static evaluation in centipawns, from White's perspective
 * (positive = better for White). Intentionally lightweight: material,
 * piece-square tables, bishop pair, and basic pawn-structure terms. Good
 * enough to rank candidate moves and flag eval swings for move-quality.
 */
object Evaluation {

    const val MATE = 30000

    val VALUE = mapOf('P' to 100, 'N' to 320, 'B' to 330, 'R' to 500, 'Q' to 900, 'K' to 0)

    // Piece-square tables from White's point of view, a1..h8 (index 0..63).
    private val PAWN = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        5, 10, 10, -20, -20, 10, 10, 5,
        5, -5, -10, 0, 0, -10, -5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, 5, 10, 25, 25, 10, 5, 5,
        10, 10, 20, 30, 30, 20, 10, 10,
        50, 50, 50, 50, 50, 50, 50, 50,
        0, 0, 0, 0, 0, 0, 0, 0
    )
    private val KNIGHT = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    )
    private val BISHOP = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10, 5, 0, 0, 0, 0, 5, -10,
        -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 0, 10, 10, 10, 10, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10,
        -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    )
    private val ROOK = intArrayOf(
        0, 0, 0, 5, 5, 0, 0, 0,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        5, 10, 10, 10, 10, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0
    )
    private val QUEEN = intArrayOf(
        -20, -10, -10, -5, -5, -10, -10, -20,
        -10, 0, 5, 0, 0, 0, 0, -10,
        -10, 5, 5, 5, 5, 5, 0, -10,
        0, 0, 5, 5, 5, 5, 0, -5,
        -5, 0, 5, 5, 5, 5, 0, -5,
        -10, 0, 5, 5, 5, 5, 0, -10,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -20, -10, -10, -5, -5, -10, -10, -20
    )
    private val KING_MID = intArrayOf(
        20, 30, 10, 0, 0, 10, 30, 20,
        20, 20, 0, 0, 0, 0, 20, 20,
        -10, -20, -20, -20, -20, -20, -20, -10,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30
    )

    private fun pst(pt: Char, sq: Int, white: Boolean): Int {
        val i = if (white) sq else sqOf(fileOf(sq), 7 - rankOf(sq))
        return when (pt) {
            'P' -> PAWN[i]; 'N' -> KNIGHT[i]; 'B' -> BISHOP[i]
            'R' -> ROOK[i]; 'Q' -> QUEEN[i]; 'K' -> KING_MID[i]
            else -> 0
        }
    }

    fun evaluate(b: Board): Int {
        var score = 0
        var whiteBishops = 0
        var blackBishops = 0
        val whitePawnFiles = IntArray(8)
        val blackPawnFiles = IntArray(8)
        for (i in 0..63) {
            val p = b.sq[i]
            if (p == '.') continue
            val white = p.isUpperCase()
            val pt = p.uppercaseChar()
            val v = VALUE[pt] ?: 0
            val s = v + pst(pt, i, white)
            score += if (white) s else -s
            if (pt == 'B') { if (white) whiteBishops++ else blackBishops++ }
            if (pt == 'P') { if (white) whitePawnFiles[fileOf(i)]++ else blackPawnFiles[fileOf(i)]++ }
        }
        if (whiteBishops >= 2) score += 30
        if (blackBishops >= 2) score -= 30
        // doubled & isolated pawns
        for (f in 0..7) {
            if (whitePawnFiles[f] > 1) score -= 12 * (whitePawnFiles[f] - 1)
            if (blackPawnFiles[f] > 1) score += 12 * (blackPawnFiles[f] - 1)
            val wLeft = if (f > 0) whitePawnFiles[f - 1] else 0
            val wRight = if (f < 7) whitePawnFiles[f + 1] else 0
            if (whitePawnFiles[f] > 0 && wLeft == 0 && wRight == 0) score -= 15
            val bLeft = if (f > 0) blackPawnFiles[f - 1] else 0
            val bRight = if (f < 7) blackPawnFiles[f + 1] else 0
            if (blackPawnFiles[f] > 0 && bLeft == 0 && bRight == 0) score += 15
        }
        return score
    }

    /** Convert a centipawn score (White POV) to a win probability for White, 0..1. */
    fun winProbability(cp: Int): Double {
        val c = cp.coerceIn(-1500, 1500)
        return 1.0 / (1.0 + Math.exp(-0.00368208 * c))
    }
}
