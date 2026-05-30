package com.nadelon.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

private val LIGHT = Color(0xFFEEEED2)
private val DARK = Color(0xFF769656)
private val HILITE = Color(0x80F6F669)

private val GLYPHS = mapOf(
    'K' to "♔", 'Q' to "♕", 'R' to "♖", 'B' to "♗", 'N' to "♘", 'P' to "♙",
    'k' to "♚", 'q' to "♛", 'r' to "♜", 'b' to "♝", 'n' to "♞", 'p' to "♟"
)

/** Parse the placement field of a FEN into a 64-char board (index = rank*8+file). */
private fun fenToBoard(fen: String): CharArray {
    val board = CharArray(64) { ' ' }
    val placement = fen.trim().substringBefore(' ')
    val rows = placement.split("/")
    for (r in 0..7) {
        if (7 - r >= rows.size) continue
        val row = rows[7 - r]
        var f = 0
        for (ch in row) {
            if (ch.isDigit()) f += ch - '0' else { if (f < 8) board[r * 8 + f] = ch; f++ }
        }
    }
    return board
}

/**
 * Renders a chess position from a FEN string. [highlight] is a set of square
 * indices (0..63, a1=0) to shade — typically the from/to of the key move.
 */
@Composable
fun BoardView(
    fen: String,
    modifier: Modifier = Modifier,
    highlight: Set<Int> = emptySet(),
    flipped: Boolean = false
) {
    val board = remember(fen) { fenToBoard(fen) }
    Column(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val ranks = if (flipped) (0..7) else (7 downTo 0)
        for (r in ranks) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                val files = if (flipped) (7 downTo 0) else (0..7)
                for (f in files) {
                    val idx = r * 8 + f
                    val base = if ((f + r) % 2 == 1) LIGHT else DARK
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(base)
                            .background(if (idx in highlight) HILITE else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        val piece = board[idx]
                        val glyph = GLYPHS[piece]
                        if (glyph != null) {
                            Text(
                                text = glyph,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = if (piece.isUpperCase()) Color(0xFFFAFAFA) else Color(0xFF111111)
                            )
                        }
                    }
                }
            }
        }
    }
}
