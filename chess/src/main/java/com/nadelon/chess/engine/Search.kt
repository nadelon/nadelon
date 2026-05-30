package com.nadelon.chess.engine

import com.nadelon.chess.core.Board
import com.nadelon.chess.core.Move

class SearchResult(val score: Int, val best: Move?)

/**
 * Negamax alpha-beta search with quiescence and MVV-LVA ordering. Scores are
 * returned from the side-to-move's perspective. Depth is kept modest because
 * this runs purely on the CPU while batch-analysing many games.
 */
class Search(private val maxDepth: Int = 4) {

    private fun pieceValue(p: Char): Int = Evaluation.VALUE[p.uppercaseChar()] ?: 0

    /** Order captures first (MVV-LVA), then the rest. */
    private fun order(b: Board, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { mv ->
            val victim = b.sq[mv.to]
            if (victim != '.') 1000 + pieceValue(victim) * 10 - pieceValue(b.sq[mv.from])
            else if (mv.promo != ' ') 800
            else 0
        }
    }

    fun search(board: Board): SearchResult {
        val me = board.turn
        val sign = if (me == Board.WHITE) 1 else -1
        var alpha = -INF
        val beta = INF
        var best: Move? = null
        val legal = order(board, board.legalMoves())
        if (legal.isEmpty()) {
            // Convert to White-POV: a side with no moves in check is mated.
            return if (board.inCheck(me)) SearchResult(-Evaluation.MATE * sign, null)
            else SearchResult(0, null)
        }
        for (mv in legal) {
            val u = board.make(mv)
            val score = -negamax(board, maxDepth - 1, -beta, -alpha)
            board.unmake(u)
            if (score > alpha) { alpha = score; best = mv }
        }
        // alpha is from mover's perspective; convert to White POV for storage callers
        return SearchResult(alpha * sign, best)
    }

    private fun negamax(board: Board, depth: Int, alphaIn: Int, beta: Int): Int {
        var alpha = alphaIn
        val me = board.turn
        if (depth <= 0) return quiesce(board, alpha, beta)
        val legal = order(board, board.legalMoves())
        if (legal.isEmpty()) {
            return if (board.inCheck(me)) -Evaluation.MATE - depth else 0
        }
        for (mv in legal) {
            val u = board.make(mv)
            val score = -negamax(board, depth - 1, -beta, -alpha)
            board.unmake(u)
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun quiesce(board: Board, alphaIn: Int, beta: Int): Int {
        var alpha = alphaIn
        val me = board.turn
        val standSign = if (me == Board.WHITE) 1 else -1
        val stand = Evaluation.evaluate(board) * standSign
        if (stand >= beta) return beta
        if (stand > alpha) alpha = stand
        // only search captures / promotions
        val caps = order(board, board.legalMoves().filter {
            board.sq[it.to] != '.' || it.flag == Move.FLAG_EP || it.promo != ' '
        })
        for (mv in caps) {
            val u = board.make(mv)
            val score = -quiesce(board, -beta, -alpha)
            board.unmake(u)
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    companion object {
        const val INF = 1_000_000
    }
}
