package com.nadelon.chess.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Move generator and SAN tests. Perft node counts are the standard reference
 * values; matching them proves legal move generation (incl. castling, en
 * passant, promotion, pins) is correct.
 */
class BoardTest {

    private fun perft(b: Board, depth: Int): Long {
        if (depth == 0) return 1
        var total = 0L
        for (mv in b.legalMoves()) {
            val u = b.make(mv)
            total += perft(b, depth - 1)
            b.unmake(u)
        }
        return total
    }

    @Test fun perftStartingPosition() {
        val b = Board.startingPosition()
        assertEquals(20L, perft(b, 1))
        assertEquals(400L, perft(b, 2))
        assertEquals(8902L, perft(b, 3))
        assertEquals(197281L, perft(b, 4))
    }

    @Test fun perftKiwipete() {
        val fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        val b = Board.fromFen(fen)
        assertEquals(48L, perft(b, 1))
        assertEquals(2039L, perft(b, 2))
        assertEquals(97862L, perft(b, 3))
    }

    @Test fun perftEnPassantAndPromotion() {
        val b = Board.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")
        assertEquals(14L, perft(b, 1))
        assertEquals(191L, perft(b, 2))
        assertEquals(2812L, perft(b, 3))
    }

    @Test fun fenRoundTrip() {
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
        assertEquals(fen, Board.fromFen(fen).toFen())
    }

    @Test fun operaGameSanRoundTrip() {
        // Morphy vs Duke Karl / Count Isouard, 1858 — captures, castling, checks, mate.
        val moves = ("e4 e5 Nf3 d6 d4 Bg4 dxe5 Bxf3 Qxf3 dxe5 Bc4 Nf6 Qb3 Qe7 Nc3 c6 Bg5 b5 " +
            "Nxb5 cxb5 Bxb5+ Nbd7 O-O-O Rd8 Rxd7 Rxd7 Rd1 Qe6 Bxd7+ Nxd7 Qb8+ Nxb8 Rd8#")
            .split(" ")
        val b = Board.startingPosition()
        for (tok in moves) {
            val legal = b.legalMoves()
            val mv = legal.firstOrNull { b.san(it, legal) == tok }
            assertTrue("SAN '$tok' should match a legal move", mv != null)
            b.make(mv!!)
        }
        // Final position: Black is checkmated.
        assertTrue(b.inCheck(Board.BLACK))
        assertEquals(0, b.legalMoves().size)
    }

    @Test fun parsesPgnMovetext() {
        val pgn = """
            [White "a"]
            [Black "b"]
            [Result "1-0"]

            1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 1-0
        """.trimIndent()
        val game = Pgn.parse(pgn)!!
        assertEquals(8, game.plies.size)
        assertEquals("Nf6", game.plies.last().san)
    }
}
