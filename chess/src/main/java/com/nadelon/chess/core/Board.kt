package com.nadelon.chess.core

/**
 * Mailbox chess board. Squares hold characters: uppercase = white, lowercase =
 * black, '.' = empty (same convention as FEN piece letters). The move generator
 * has been perft-validated against the five standard test positions
 * (startpos, Kiwipete, etc.) before being ported here.
 */
class Board {
    val sq = CharArray(64) { '.' }
    var turn = WHITE
    var castle = 0          // bitmask WK|WQ|BK|BQ
    var ep = -1             // en passant target square, or -1
    var half = 0
    var full = 1

    companion object {
        const val WHITE = 0
        const val BLACK = 1

        const val WK = 1
        const val WQ = 2
        const val BK = 4
        const val BQ = 8

        val KNIGHT_OFFS = arrayOf(
            intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
            intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)
        )
        val KING_OFFS = arrayOf(
            intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(-1, 1),
            intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1)
        )
        val BISHOP_DIRS = arrayOf(
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)
        )
        val ROOK_DIRS = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)
        )
        val QUEEN_DIRS = BISHOP_DIRS + ROOK_DIRS

        const val STARTPOS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun fromFen(fen: String): Board {
            val b = Board()
            val parts = fen.trim().split(Regex("\\s+"))
            val rows = parts[0].split("/")
            for (r in 0..7) {
                val row = rows[7 - r]
                var f = 0
                for (ch in row) {
                    if (ch.isDigit()) {
                        f += ch - '0'
                    } else {
                        b.sq[sqOf(f, r)] = ch
                        f++
                    }
                }
            }
            b.turn = if (parts.size > 1 && parts[1] == "b") BLACK else WHITE
            b.castle = 0
            if (parts.size > 2) {
                for (c in parts[2]) when (c) {
                    'K' -> b.castle = b.castle or WK
                    'Q' -> b.castle = b.castle or WQ
                    'k' -> b.castle = b.castle or BK
                    'q' -> b.castle = b.castle or BQ
                }
            }
            b.ep = if (parts.size > 3 && parts[3] != "-")
                sqOf(parts[3][0] - 'a', parts[3][1] - '1') else -1
            b.half = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
            b.full = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1
            return b
        }

        fun startingPosition(): Board = fromFen(STARTPOS)
    }

    fun copy(): Board {
        val b = Board()
        sq.copyInto(b.sq)
        b.turn = turn; b.castle = castle; b.ep = ep; b.half = half; b.full = full
        return b
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 7 downTo 0) {
            var empty = 0
            for (f in 0..7) {
                val p = sq[sqOf(f, r)]
                if (p == '.') empty++ else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(p)
                }
            }
            if (empty > 0) sb.append(empty)
            if (r > 0) sb.append('/')
        }
        sb.append(if (turn == WHITE) " w " else " b ")
        val c = StringBuilder()
        if (castle and WK != 0) c.append('K')
        if (castle and WQ != 0) c.append('Q')
        if (castle and BK != 0) c.append('k')
        if (castle and BQ != 0) c.append('q')
        sb.append(if (c.isEmpty()) "-" else c.toString())
        sb.append(' ')
        sb.append(if (ep < 0) "-" else squareName(ep))
        sb.append(' ').append(half).append(' ').append(full)
        return sb.toString()
    }

    fun colorOf(p: Char): Int = when {
        p == '.' -> -1
        p.isUpperCase() -> WHITE
        else -> BLACK
    }

    fun findKing(color: Int): Int {
        val k = if (color == WHITE) 'K' else 'k'
        for (i in 0..63) if (sq[i] == k) return i
        return -1
    }

    /** Is [target] attacked by any piece of side [by]? */
    fun attackedBy(target: Int, by: Int): Boolean {
        val f0 = fileOf(target); val r0 = rankOf(target)
        // pawns
        if (by == WHITE) {
            for (df in intArrayOf(-1, 1)) {
                val f = f0 + df; val r = r0 - 1
                if (onBoard(f, r) && sq[sqOf(f, r)] == 'P') return true
            }
        } else {
            for (df in intArrayOf(-1, 1)) {
                val f = f0 + df; val r = r0 + 1
                if (onBoard(f, r) && sq[sqOf(f, r)] == 'p') return true
            }
        }
        // knights
        val kn = if (by == WHITE) 'N' else 'n'
        for (o in KNIGHT_OFFS) {
            val f = f0 + o[0]; val r = r0 + o[1]
            if (onBoard(f, r) && sq[sqOf(f, r)] == kn) return true
        }
        // king
        val kg = if (by == WHITE) 'K' else 'k'
        for (o in KING_OFFS) {
            val f = f0 + o[0]; val r = r0 + o[1]
            if (onBoard(f, r) && sq[sqOf(f, r)] == kg) return true
        }
        // bishop / queen
        val bq1 = if (by == WHITE) 'B' else 'b'
        val bq2 = if (by == WHITE) 'Q' else 'q'
        for (o in BISHOP_DIRS) {
            var f = f0 + o[0]; var r = r0 + o[1]
            while (onBoard(f, r)) {
                val p = sq[sqOf(f, r)]
                if (p != '.') { if (p == bq1 || p == bq2) return true; break }
                f += o[0]; r += o[1]
            }
        }
        // rook / queen
        val rq1 = if (by == WHITE) 'R' else 'r'
        val rq2 = if (by == WHITE) 'Q' else 'q'
        for (o in ROOK_DIRS) {
            var f = f0 + o[0]; var r = r0 + o[1]
            while (onBoard(f, r)) {
                val p = sq[sqOf(f, r)]
                if (p != '.') { if (p == rq1 || p == rq2) return true; break }
                f += o[0]; r += o[1]
            }
        }
        return false
    }

    fun inCheck(color: Int): Boolean = attackedBy(findKing(color), 1 - color)

    fun pseudoMoves(): ArrayList<Move> {
        val moves = ArrayList<Move>(48)
        val me = turn
        for (frm in 0..63) {
            val p = sq[frm]
            if (p == '.' || colorOf(p) != me) continue
            val f0 = fileOf(frm); val r0 = rankOf(frm)
            when (p.uppercaseChar()) {
                'P' -> {
                    val fwd = if (me == WHITE) 1 else -1
                    val startRank = if (me == WHITE) 1 else 6
                    val promoRank = if (me == WHITE) 7 else 0
                    val r1 = r0 + fwd
                    if (onBoard(f0, r1) && sq[sqOf(f0, r1)] == '.') {
                        val to = sqOf(f0, r1)
                        if (r1 == promoRank) {
                            for (pr in "QRBN") moves.add(Move(frm, to, pr))
                        } else {
                            moves.add(Move(frm, to))
                            if (r0 == startRank) {
                                val r2 = r0 + 2 * fwd
                                if (sq[sqOf(f0, r2)] == '.')
                                    moves.add(Move(frm, sqOf(f0, r2), ' ', Move.FLAG_DOUBLE))
                            }
                        }
                    }
                    for (df in intArrayOf(-1, 1)) {
                        val f1 = f0 + df
                        if (!onBoard(f1, r1)) continue
                        val to = sqOf(f1, r1)
                        val tp = sq[to]
                        if (tp != '.' && colorOf(tp) != me) {
                            if (r1 == promoRank) for (pr in "QRBN") moves.add(Move(frm, to, pr))
                            else moves.add(Move(frm, to))
                        } else if (to == ep) {
                            moves.add(Move(frm, to, ' ', Move.FLAG_EP))
                        }
                    }
                }
                'N' -> for (o in KNIGHT_OFFS) {
                    val f = f0 + o[0]; val r = r0 + o[1]
                    if (onBoard(f, r)) {
                        val tp = sq[sqOf(f, r)]
                        if (tp == '.' || colorOf(tp) != me) moves.add(Move(frm, sqOf(f, r)))
                    }
                }
                'K' -> {
                    for (o in KING_OFFS) {
                        val f = f0 + o[0]; val r = r0 + o[1]
                        if (onBoard(f, r)) {
                            val tp = sq[sqOf(f, r)]
                            if (tp == '.' || colorOf(tp) != me) moves.add(Move(frm, sqOf(f, r)))
                        }
                    }
                    addCastles(frm, me, moves)
                }
                'B' -> slide(frm, me, BISHOP_DIRS, moves)
                'R' -> slide(frm, me, ROOK_DIRS, moves)
                'Q' -> slide(frm, me, QUEEN_DIRS, moves)
            }
        }
        return moves
    }

    private fun slide(frm: Int, me: Int, dirs: Array<IntArray>, moves: ArrayList<Move>) {
        val f0 = fileOf(frm); val r0 = rankOf(frm)
        for (o in dirs) {
            var f = f0 + o[0]; var r = r0 + o[1]
            while (onBoard(f, r)) {
                val to = sqOf(f, r)
                val tp = sq[to]
                if (tp == '.') moves.add(Move(frm, to))
                else { if (colorOf(tp) != me) moves.add(Move(frm, to)); break }
                f += o[0]; r += o[1]
            }
        }
    }

    private fun addCastles(frm: Int, me: Int, moves: ArrayList<Move>) {
        if (me == WHITE && frm == sqOf(4, 0)) {
            if (castle and WK != 0 && sq[sqOf(5, 0)] == '.' && sq[sqOf(6, 0)] == '.' &&
                sq[sqOf(7, 0)] == 'R' &&
                !attackedBy(sqOf(4, 0), BLACK) && !attackedBy(sqOf(5, 0), BLACK) &&
                !attackedBy(sqOf(6, 0), BLACK)
            ) moves.add(Move(frm, sqOf(6, 0), ' ', Move.FLAG_CASTLE_K))
            if (castle and WQ != 0 && sq[sqOf(3, 0)] == '.' && sq[sqOf(2, 0)] == '.' &&
                sq[sqOf(1, 0)] == '.' && sq[sqOf(0, 0)] == 'R' &&
                !attackedBy(sqOf(4, 0), BLACK) && !attackedBy(sqOf(3, 0), BLACK) &&
                !attackedBy(sqOf(2, 0), BLACK)
            ) moves.add(Move(frm, sqOf(2, 0), ' ', Move.FLAG_CASTLE_Q))
        }
        if (me == BLACK && frm == sqOf(4, 7)) {
            if (castle and BK != 0 && sq[sqOf(5, 7)] == '.' && sq[sqOf(6, 7)] == '.' &&
                sq[sqOf(7, 7)] == 'r' &&
                !attackedBy(sqOf(4, 7), WHITE) && !attackedBy(sqOf(5, 7), WHITE) &&
                !attackedBy(sqOf(6, 7), WHITE)
            ) moves.add(Move(frm, sqOf(6, 7), ' ', Move.FLAG_CASTLE_K))
            if (castle and BQ != 0 && sq[sqOf(3, 7)] == '.' && sq[sqOf(2, 7)] == '.' &&
                sq[sqOf(1, 7)] == '.' && sq[sqOf(0, 7)] == 'r' &&
                !attackedBy(sqOf(4, 7), WHITE) && !attackedBy(sqOf(3, 7), WHITE) &&
                !attackedBy(sqOf(2, 7), WHITE)
            ) moves.add(Move(frm, sqOf(2, 7), ' ', Move.FLAG_CASTLE_Q))
        }
    }

    fun make(mv: Move): Undo {
        val undo = Undo(mv, sq[mv.from], sq[mv.to], castle, ep, half, full, turn)
        val p = sq[mv.from]
        val me = turn
        ep = -1
        half++
        if (p.uppercaseChar() == 'P' || sq[mv.to] != '.') half = 0
        sq[mv.to] = p
        sq[mv.from] = '.'
        when (mv.flag) {
            Move.FLAG_DOUBLE -> ep = (mv.from + mv.to) / 2
            Move.FLAG_EP -> sq[sqOf(fileOf(mv.to), rankOf(mv.from))] = '.'
            Move.FLAG_CASTLE_K -> if (me == WHITE) {
                sq[sqOf(5, 0)] = 'R'; sq[sqOf(7, 0)] = '.'
            } else {
                sq[sqOf(5, 7)] = 'r'; sq[sqOf(7, 7)] = '.'
            }
            Move.FLAG_CASTLE_Q -> if (me == WHITE) {
                sq[sqOf(3, 0)] = 'R'; sq[sqOf(0, 0)] = '.'
            } else {
                sq[sqOf(3, 7)] = 'r'; sq[sqOf(0, 7)] = '.'
            }
        }
        if (mv.promo != ' ') sq[mv.to] = if (me == WHITE) mv.promo else mv.promo.lowercaseChar()
        // castling rights
        if (mv.from == sqOf(4, 0) || mv.to == sqOf(4, 0)) castle = castle and (WK or WQ).inv()
        if (mv.from == sqOf(0, 0) || mv.to == sqOf(0, 0)) castle = castle and WQ.inv()
        if (mv.from == sqOf(7, 0) || mv.to == sqOf(7, 0)) castle = castle and WK.inv()
        if (mv.from == sqOf(4, 7) || mv.to == sqOf(4, 7)) castle = castle and (BK or BQ).inv()
        if (mv.from == sqOf(0, 7) || mv.to == sqOf(0, 7)) castle = castle and BQ.inv()
        if (mv.from == sqOf(7, 7) || mv.to == sqOf(7, 7)) castle = castle and BK.inv()
        if (me == BLACK) full++
        turn = 1 - me
        return undo
    }

    fun unmake(u: Undo) {
        val mv = u.move
        val me = u.turn
        sq[mv.from] = u.pieceFrom
        sq[mv.to] = u.pieceTo
        when (mv.flag) {
            Move.FLAG_EP -> sq[sqOf(fileOf(mv.to), rankOf(mv.from))] = if (me == WHITE) 'p' else 'P'
            Move.FLAG_CASTLE_K -> if (me == WHITE) {
                sq[sqOf(7, 0)] = 'R'; sq[sqOf(5, 0)] = '.'
            } else {
                sq[sqOf(7, 7)] = 'r'; sq[sqOf(5, 7)] = '.'
            }
            Move.FLAG_CASTLE_Q -> if (me == WHITE) {
                sq[sqOf(0, 0)] = 'R'; sq[sqOf(3, 0)] = '.'
            } else {
                sq[sqOf(0, 7)] = 'r'; sq[sqOf(3, 7)] = '.'
            }
        }
        castle = u.castle; ep = u.ep; half = u.half; full = u.full; turn = u.turn
    }

    fun legalMoves(): ArrayList<Move> {
        val out = ArrayList<Move>(40)
        val me = turn
        for (mv in pseudoMoves()) {
            val u = make(mv)
            if (!attackedBy(findKing(me), 1 - me)) out.add(mv)
            unmake(u)
        }
        return out
    }

    /** Standard Algebraic Notation for [mv]; [legal] is the current legal move list. */
    fun san(mv: Move, legal: List<Move>): String {
        val base: String = when (mv.flag) {
            Move.FLAG_CASTLE_K -> "O-O"
            Move.FLAG_CASTLE_Q -> "O-O-O"
            else -> {
                val p = sq[mv.from]
                val pt = p.uppercaseChar()
                val capture = sq[mv.to] != '.' || mv.flag == Move.FLAG_EP
                val dest = squareName(mv.to)
                if (pt == 'P') {
                    val sb = StringBuilder()
                    if (capture) sb.append('a' + fileOf(mv.from)).append('x')
                    sb.append(dest)
                    if (mv.promo != ' ') sb.append('=').append(mv.promo)
                    sb.toString()
                } else {
                    val same = legal.filter {
                        it.to == mv.to && sq[it.from] == p && it.from != mv.from
                    }
                    var disamb = ""
                    if (same.isNotEmpty()) {
                        val sameFile = same.any { fileOf(it.from) == fileOf(mv.from) }
                        val sameRank = same.any { rankOf(it.from) == rankOf(mv.from) }
                        disamb = when {
                            !sameFile -> "${'a' + fileOf(mv.from)}"
                            !sameRank -> "${rankOf(mv.from) + 1}"
                            else -> "${'a' + fileOf(mv.from)}${rankOf(mv.from) + 1}"
                        }
                    }
                    "$pt$disamb${if (capture) "x" else ""}$dest"
                }
            }
        }
        val u = make(mv)
        val opp = turn
        val chk = attackedBy(findKing(opp), 1 - opp)
        val hasMoves = legalMoves().isNotEmpty()
        unmake(u)
        return base + if (chk) (if (hasMoves) "+" else "#") else ""
    }

    /** Resolve a SAN token to a legal [Move], or null if it matches none. */
    fun moveFromSan(token: String): Move? {
        val cleaned = token.trim().removeSuffix("!").removeSuffix("?")
            .trimEnd('!', '?')
        val legal = legalMoves()
        for (mv in legal) {
            val s = san(mv, legal)
            if (s == cleaned || s.trimEnd('+', '#') == cleaned.trimEnd('+', '#')) return mv
        }
        // tolerate notation variants like "0-0" and missing check marks
        val norm = cleaned.replace("0", "O")
        for (mv in legal) {
            val s = san(mv, legal).replace("0", "O")
            if (s.trimEnd('+', '#') == norm.trimEnd('+', '#')) return mv
        }
        return null
    }
}
