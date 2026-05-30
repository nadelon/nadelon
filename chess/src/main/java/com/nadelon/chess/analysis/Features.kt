package com.nadelon.chess.analysis

import com.nadelon.chess.core.Board
import com.nadelon.chess.core.fileOf
import com.nadelon.chess.core.rankOf
import com.nadelon.chess.core.sqOf

/** Positional features for one side, derived purely from a static position. */
class SideFeatures(
    val centerPawns: Int,        // own pawns on d4/e4/d5/e5
    val centerControl: Int,      // attacks into the four central squares
    val lightControl: Int,       // squares of light colour this side attacks
    val darkControl: Int,        // squares of dark colour this side attacks
    val space: Int,              // controlled squares in the opponent's half
    val developedMinors: Int,    // knights+bishops off the back rank
    val castled: Boolean,
    val kingSq: Int,
    val rooksOnOpenOrSemi: Int,
    val mobility: Int,
    val hasLightBishop: Boolean,
    val hasDarkBishop: Boolean,
    val queenSq: Int,
    val doubledPawns: Int,
    val isolatedPawns: Int
)

class PositionFeatures(val white: SideFeatures, val black: SideFeatures) {
    fun forSide(color: Int) = if (color == Board.WHITE) white else black
    fun opponentOf(color: Int) = if (color == Board.WHITE) black else white
}

object Features {

    private val CENTER = intArrayOf(sqOf(3, 3), sqOf(4, 3), sqOf(3, 4), sqOf(4, 4)) // d4 e4 d5 e5

    /** a1 (file0,rank0) is a dark square -> (file+rank) even == dark. */
    private fun isLight(sq: Int) = (fileOf(sq) + rankOf(sq)) % 2 == 1

    fun extract(b: Board): PositionFeatures =
        PositionFeatures(side(b, Board.WHITE), side(b, Board.BLACK))

    private fun side(b: Board, color: Int): SideFeatures {
        val white = color == Board.WHITE
        val pawn = if (white) 'P' else 'p'
        val backRank = if (white) 0 else 7

        var centerPawns = 0
        for (c in CENTER) if (b.sq[c] == pawn) centerPawns++

        // attack maps
        var light = 0; var dark = 0; var space = 0; var centerControl = 0
        for (sq in 0..63) {
            if (b.attackedBy(sq, color)) {
                if (isLight(sq)) light++ else dark++
                val r = rankOf(sq)
                val inOppHalf = if (white) r >= 4 else r <= 3
                if (inOppHalf) space++
                if (sq in CENTER) centerControl++
            }
        }

        var developedMinors = 0
        var hasLight = false; var hasDark = false
        var queenSq = -1
        val pawnFiles = IntArray(8)
        for (i in 0..63) {
            val p = b.sq[i]
            if (p == '.' || b.colorOf(p) != color) continue
            when (p.uppercaseChar()) {
                'N', 'B' -> if (rankOf(i) != backRank) developedMinors++
                'Q' -> queenSq = i
                'P' -> pawnFiles[fileOf(i)]++
            }
            if (p.uppercaseChar() == 'B') { if (isLight(i)) hasLight = true else hasDark = true }
        }

        val kingSq = b.findKing(color)
        val castled = kingSq >= 0 && (fileOf(kingSq) >= 6 || fileOf(kingSq) <= 2) &&
            rankOf(kingSq) == backRank

        // rooks on open / semi-open files
        var rooksGood = 0
        val rook = if (white) 'R' else 'r'
        for (i in 0..63) {
            if (b.sq[i] == rook) {
                val f = fileOf(i)
                var ownPawn = false; var anyPawn = false
                for (r in 0..7) {
                    val c = b.sq[sqOf(f, r)]
                    if (c == 'P' || c == 'p') {
                        anyPawn = true
                        if (c == pawn) ownPawn = true
                    }
                }
                if (!anyPawn || !ownPawn) rooksGood++ // open or semi-open
            }
        }

        var doubled = 0; var isolated = 0
        for (f in 0..7) {
            if (pawnFiles[f] > 1) doubled += pawnFiles[f] - 1
            val left = if (f > 0) pawnFiles[f - 1] else 0
            val right = if (f < 7) pawnFiles[f + 1] else 0
            if (pawnFiles[f] > 0 && left == 0 && right == 0) isolated += pawnFiles[f]
        }

        val mobility = mobilityFor(b, color)

        return SideFeatures(
            centerPawns, centerControl, light, dark, space, developedMinors, castled,
            kingSq, rooksGood, mobility, hasLight, hasDark, queenSq, doubled, isolated
        )
    }

    /** Count pseudo-legal moves available to [color] regardless of whose turn it is. */
    private fun mobilityFor(b: Board, color: Int): Int {
        val saved = b.turn
        b.turn = color
        val n = b.pseudoMoves().size
        b.turn = saved
        return n
    }
}
