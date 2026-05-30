package com.nadelon.chess

import com.nadelon.chess.analysis.AggregateBuilder
import com.nadelon.chess.analysis.AnalyzerConfig
import com.nadelon.chess.analysis.GameAnalyzer
import com.nadelon.chess.core.Board
import com.nadelon.chess.core.Pgn
import com.nadelon.chess.lessons.LessonEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipelineTest {

    private val operaPgn = """
        [Event "Paris"]
        [White "Morphy"]
        [Black "Allies"]
        [Result "1-0"]
        [ECO "C41"]
        [Date "1858.10.31"]

        1. e4 e5 2. Nf3 d6 3. d4 Bg4 4. dxe5 Bxf3 5. Qxf3 dxe5 6. Bc4 Nf6 7. Qb3 Qe7
        8. Nc3 c6 9. Bg5 b5 10. Nxb5 cxb5 11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7
        14. Rd1 Qe6 15. Bxd7+ Nxd7 16. Qb8+ Nxb8 17. Rd8# 1-0
    """.trimIndent()

    @Test fun parsesAndIdentifiesOpening() {
        val game = Pgn.parse(operaPgn)!!
        assertEquals(33, game.plies.size)
        val report = GameAnalyzer(AnalyzerConfig(useEngine = false))
            .analyze(game, "Allies")!!
        assertEquals(Board.BLACK, report.playerColor)
        assertEquals("Philidor Defense", report.opening.family)
        assertEquals(com.nadelon.chess.analysis.PlayerResult.LOSS, report.outcome)
    }

    @Test fun returnsNullWhenUserNotInGame() {
        val game = Pgn.parse(operaPgn)!!
        assertEquals(null, GameAnalyzer(AnalyzerConfig(useEngine = false)).analyze(game, "nobody"))
    }

    @Test fun engineProducesAccuracyAndFlagsErrors() {
        val game = Pgn.parse(operaPgn)!!
        // Shallow depth keeps the unit test fast but still exercises the full path.
        val report = GameAnalyzer(AnalyzerConfig(useEngine = true, depth = 2))
            .analyze(game, "Allies")!!
        assertFalse("accuracy should be a number", report.accuracy.isNaN())
        assertTrue(report.accuracy in 0.0..100.0)
        // The losing side made at least one serious error in this game.
        assertTrue(report.inaccuracies + report.mistakes + report.blunders >= 1)
    }

    @Test fun aggregatesAcrossGamesAndBuildsLessons() {
        val game = Pgn.parse(operaPgn)!!
        val analyzer = GameAnalyzer(AnalyzerConfig(useEngine = false))
        val reports = listOf(
            analyzer.analyze(game, "Allies")!!,
            analyzer.analyze(game, "Allies")!!
        )
        val agg = AggregateBuilder.build("Allies", reports)
        assertEquals(2, agg.totalGames)
        assertEquals(2, agg.losses)
        assertEquals(1, agg.byOpening.size)
        assertEquals("Philidor Defense", agg.byOpening.first().family)

        val plan = LessonEngine.studyPlanSummary(agg)
        assertTrue(plan.contains("Philidor"))

        // Lessons are generated from detected themes (this lost game has several).
        val lessons = LessonEngine.build(agg)
        assertNotNull(lessons)
    }
}
