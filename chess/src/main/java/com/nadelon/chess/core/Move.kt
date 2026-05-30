package com.nadelon.chess.core

/**
 * A chess move. Squares are 0..63 with a1=0, h8=63 (index = rank*8 + file).
 * [promo] is one of 'Q','R','B','N' or ' ' for no promotion.
 * [flag] uses the FLAG_* constants below.
 */
data class Move(
    val from: Int,
    val to: Int,
    val promo: Char = ' ',
    val flag: Int = FLAG_NORMAL
) {
    companion object {
        const val FLAG_NORMAL = 0
        const val FLAG_DOUBLE = 1      // pawn two-square advance
        const val FLAG_EP = 2          // en passant capture
        const val FLAG_CASTLE_K = 3    // king-side castle
        const val FLAG_CASTLE_Q = 4    // queen-side castle
    }
}

/** Undo information produced by [Board.make] and consumed by [Board.unmake]. */
class Undo(
    val move: Move,
    val pieceFrom: Char,
    val pieceTo: Char,
    val castle: Int,
    val ep: Int,
    val half: Int,
    val full: Int,
    val turn: Int
)

fun fileOf(sq: Int): Int = sq and 7
fun rankOf(sq: Int): Int = sq shr 3
fun sqOf(file: Int, rank: Int): Int = rank * 8 + file
fun onBoard(file: Int, rank: Int): Boolean = file in 0..7 && rank in 0..7

/** Algebraic name of a square, e.g. 27 -> "d4". */
fun squareName(sq: Int): String = "${'a' + fileOf(sq)}${rankOf(sq) + 1}"
