package com.nadelon.chess.analysis

import com.nadelon.chess.core.Board
import com.nadelon.chess.core.fileOf
import com.nadelon.chess.core.rankOf
import com.nadelon.chess.engine.Quality
import kotlin.math.abs

/** A recurring positional / strategic motif the coach can teach. */
enum class ThemeKey(val title: String, val category: String) {
    CENTER_NEGLECT("Neglecting the centre", "Strategy"),
    SLOW_DEVELOPMENT("Slow development", "Opening"),
    EARLY_QUEEN("Premature queen sorties", "Opening"),
    KING_IN_CENTER("Leaving the king in the centre", "King safety"),
    WEAK_DARK_SQUARES("Weak dark squares", "Pawn structure"),
    WEAK_LIGHT_SQUARES("Weak light squares", "Pawn structure"),
    PASSIVE_ROOKS("Passive rooks / ignoring open files", "Piece activity"),
    NO_COUNTERPLAY("Conceding space without counterplay", "Strategy"),
    PAWN_WEAKNESSES("Creating lasting pawn weaknesses", "Pawn structure"),
    CONVERSION_FAIL("Failing to convert winning positions", "Technique"),
    LATE_GAME_COLLAPSE("Collapsing in the late game", "Endgame"),
    TACTICAL_BLUNDERS("Tactical oversights", "Tactics")
}

/** A single occurrence of a theme inside one game. */
class ThemeHit(
    val theme: ThemeKey,
    val plyIndex: Int,        // index into the game's ply list (or -1)
    val fen: String,
    val detail: String,
    val severity: Int         // 1 (minor) .. 3 (severe)
)

/**
 * Strategic-pattern detectors. Each emits at most one hit per game so the
 * aggregate "how often does this happen" count is meaningful. Detectors are
 * deliberately conservative heuristics — they describe tendencies, and the
 * lessons explain the underlying principle.
 */
object Themes {

    private fun isLight(sq: Int) = (fileOf(sq) + rankOf(sq)) % 2 == 1

    /** A serious error (mistake or blunder), null-safe. */
    private fun Quality?.isSerious(): Boolean =
        this == Quality.MISTAKE || this == Quality.BLUNDER

    fun detect(
        playerColor: Int,
        plies: List<AnalyzedPly>,
        playerWon: Boolean,
        playerDrew: Boolean
    ): List<ThemeHit> {
        val hits = ArrayList<ThemeHit>()
        val playerPlies = plies.filter { it.side == playerColor }
        if (playerPlies.isEmpty()) return hits

        fun atFullmove(target: Int): AnalyzedPly? =
            playerPlies.minByOrNull { abs(((it.ply / 2) + 1) - target) }

        // 1. Centre neglect (around move 8)
        atFullmove(8)?.let { p ->
            val me = p.features.forSide(playerColor)
            val opp = p.features.opponentOf(playerColor)
            val gap = (opp.centerControl + opp.centerPawns) - (me.centerControl + me.centerPawns)
            if (gap >= 3 && me.centerPawns <= opp.centerPawns) {
                hits.add(
                    ThemeHit(
                        ThemeKey.CENTER_NEGLECT, p.ply, p.fenBefore,
                        "By move ${p.ply / 2 + 1} your opponent controlled the centre more firmly " +
                            "(${opp.centerControl} central attacks and ${opp.centerPawns} central pawns " +
                            "vs your ${me.centerControl}/${me.centerPawns}).",
                        if (gap >= 5) 3 else 2
                    )
                )
            }
        }

        // 2. Slow development (around move 10)
        atFullmove(10)?.let { p ->
            val me = p.features.forSide(playerColor)
            if (!me.castled && me.developedMinors <= 2) {
                hits.add(
                    ThemeHit(
                        ThemeKey.SLOW_DEVELOPMENT, p.ply, p.fenBefore,
                        "Around move ${p.ply / 2 + 1} only ${me.developedMinors} of your minor pieces were " +
                            "developed and your king had not castled.",
                        if (me.developedMinors <= 1) 3 else 2
                    )
                )
            }
        }

        // 3. Premature queen sortie (first early queen move)
        playerPlies.firstOrNull { it.san.startsWith("Q") && it.ply <= 7 }?.let { p ->
            val me = p.features.forSide(playerColor)
            if (me.developedMinors < 2) {
                hits.add(
                    ThemeHit(
                        ThemeKey.EARLY_QUEEN, p.ply, p.fenBefore,
                        "You moved your queen (${p.san}) on move ${p.ply / 2 + 1} before developing your " +
                            "minor pieces, inviting your opponent to gain tempi by attacking it.",
                        2
                    )
                )
            }
        }

        // 4. King left in the centre (by move 14)
        atFullmove(14)?.let { p ->
            val me = p.features.forSide(playerColor)
            val kf = fileOf(me.kingSq)
            if (!me.castled && kf in 3..5) {
                hits.add(
                    ThemeHit(
                        ThemeKey.KING_IN_CENTER, p.ply, p.fenBefore,
                        "Your king was still in the centre on move ${p.ply / 2 + 1}, exposed to attacks " +
                            "down the central files.",
                        3
                    )
                )
            }
        }

        // 5. Weak colour complex (middlegame, missing a bishop + opponent dominates that colour near your king)
        val mg = playerPlies.filter { it.ply in 20..60 }
        run {
            var bestHit: ThemeHit? = null
            for (p in mg) {
                val me = p.features.forSide(playerColor)
                val b = Board.fromFen(p.fenBefore)
                val opp = 1 - playerColor
                if (!me.hasDarkBishop) {
                    val dom = zoneColorControl(b, me.kingSq, opp, light = false)
                    if (dom >= 4) {
                        bestHit = ThemeHit(
                            ThemeKey.WEAK_DARK_SQUARES, p.ply, p.fenBefore,
                            "Without your dark-squared bishop, your opponent's pieces dominated the dark " +
                                "squares around your king (${dom} dark-square attacks in the king zone).",
                            if (dom >= 6) 3 else 2
                        )
                        break
                    }
                }
                if (!me.hasLightBishop) {
                    val dom = zoneColorControl(b, me.kingSq, opp, light = true)
                    if (dom >= 4) {
                        bestHit = ThemeHit(
                            ThemeKey.WEAK_LIGHT_SQUARES, p.ply, p.fenBefore,
                            "Without your light-squared bishop, your opponent's pieces dominated the light " +
                                "squares around your king (${dom} light-square attacks in the king zone).",
                            if (dom >= 6) 3 else 2
                        )
                        break
                    }
                }
            }
            bestHit?.let { hits.add(it) }
        }

        // 6. Passive rooks while open files exist (middlegame)
        atFullmove(18)?.let { p ->
            val b = Board.fromFen(p.fenBefore)
            val me = p.features.forSide(playerColor)
            if (me.rooksOnOpenOrSemi == 0 && hasOpenFile(b) && hasRook(b, playerColor)) {
                hits.add(
                    ThemeHit(
                        ThemeKey.PASSIVE_ROOKS, p.ply, p.fenBefore,
                        "Open files were available on move ${p.ply / 2 + 1} but none of your rooks were " +
                            "placed to use them.",
                        2
                    )
                )
            }
        }

        // 7. Conceding space without counterplay (middlegame)
        atFullmove(16)?.let { p ->
            val me = p.features.forSide(playerColor)
            val opp = p.features.opponentOf(playerColor)
            if (opp.space - me.space >= 6 && me.centerControl <= opp.centerControl) {
                hits.add(
                    ThemeHit(
                        ThemeKey.NO_COUNTERPLAY, p.ply, p.fenBefore,
                        "Your opponent had a large space advantage (${opp.space} vs ${me.space} controlled " +
                            "squares in your half) and you had no central counterplay to push back.",
                        2
                    )
                )
            }
        }

        // 8. Lasting pawn weaknesses (middlegame)
        mg.firstOrNull { it.features.forSide(playerColor).isolatedPawns +
            it.features.forSide(playerColor).doubledPawns >= 2 }?.let { p ->
            val me = p.features.forSide(playerColor)
            hits.add(
                ThemeHit(
                    ThemeKey.PAWN_WEAKNESSES, p.ply, p.fenBefore,
                    "Your structure carried lasting weaknesses (${me.isolatedPawns} isolated, " +
                        "${me.doubledPawns} doubled pawns) that the opponent could target.",
                    2
                )
            )
        }

        // 9. Failing to convert a winning position
        run {
            val analyzed = playerPlies.filter { it.verdict != null }
            val peak = analyzed.maxByOrNull { it.verdict!!.cpAfter }
            if (peak != null && peak.verdict!!.cpAfter >= 250 && !playerWon) {
                hits.add(
                    ThemeHit(
                        ThemeKey.CONVERSION_FAIL, peak.ply, peak.fenAfter,
                        "You reached a clearly winning position (about +${peak.verdict.cpAfter / 100.0} " +
                            "for you) but the game ended " + (if (playerDrew) "in a draw." else "in a loss."),
                        3
                    )
                )
            }
        }

        // 10. Late-game collapse
        run {
            val maxPly = plies.maxOfOrNull { it.ply } ?: 0
            if (maxPly >= 30) {
                val cutoff = (maxPly * 2) / 3
                val late = playerPlies.count {
                    it.ply >= cutoff && it.verdict?.quality.isSerious()
                }
                val early = playerPlies.count {
                    it.ply < cutoff && it.verdict?.quality.isSerious()
                }
                if (late >= 2 && late > early) {
                    val ex = playerPlies.lastOrNull {
                        it.ply >= cutoff && it.verdict?.quality == Quality.BLUNDER
                    } ?: playerPlies.last { it.ply >= cutoff }
                    hits.add(
                        ThemeHit(
                            ThemeKey.LATE_GAME_COLLAPSE, ex.ply, ex.fenBefore,
                            "Most of your serious errors ($late of them) came in the final third of the game, " +
                                "a sign of fatigue, time pressure, or shaky endgame technique.",
                            2
                        )
                    )
                }
            }
        }

        // 11. Tactical oversights (worst engine-flagged blunder)
        playerPlies.filter { it.verdict?.quality == Quality.BLUNDER }
            .maxByOrNull { it.verdict!!.winLoss }?.let { p ->
                val best = p.verdict!!.bestSan
                hits.add(
                    ThemeHit(
                        ThemeKey.TACTICAL_BLUNDERS, p.ply, p.fenBefore,
                        "On move ${p.ply / 2 + 1} you played ${p.san}" +
                            (if (best != null) ", but ${best} was much stronger." else ".") +
                            " The engine evaluates this as a serious tactical error.",
                        3
                    )
                )
            }

        return hits
    }

    /** Count squares of a given colour within king distance 2 that [byColor] attacks. */
    private fun zoneColorControl(b: Board, kingSq: Int, byColor: Int, light: Boolean): Int {
        if (kingSq < 0) return 0
        val kf = fileOf(kingSq); val kr = rankOf(kingSq)
        var n = 0
        for (f in (kf - 2)..(kf + 2)) for (r in (kr - 2)..(kr + 2)) {
            if (f in 0..7 && r in 0..7) {
                val sq = r * 8 + f
                if (isLight(sq) == light && b.attackedBy(sq, byColor)) n++
            }
        }
        return n
    }

    private fun hasOpenFile(b: Board): Boolean {
        for (f in 0..7) {
            var pawn = false
            for (r in 0..7) {
                val c = b.sq[r * 8 + f]
                if (c == 'P' || c == 'p') { pawn = true; break }
            }
            if (!pawn) return true
        }
        return false
    }

    private fun hasRook(b: Board, color: Int): Boolean {
        val rook = if (color == Board.WHITE) 'R' else 'r'
        return b.sq.any { it == rook }
    }
}
