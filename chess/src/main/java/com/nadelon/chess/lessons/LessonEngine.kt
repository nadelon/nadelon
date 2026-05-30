package com.nadelon.chess.lessons

import com.nadelon.chess.analysis.Aggregate
import com.nadelon.chess.analysis.ThemeExample

/**
 * Turns the aggregate analysis into a ranked, personalised study plan, drawing
 * theory from [LessonLibrary] and attaching the user's own example positions.
 */
object LessonEngine {

    fun build(agg: Aggregate): List<Lesson> {
        val lessons = ArrayList<Lesson>()

        // Theme-based lessons, prioritised by how often and how severely they occur.
        for (stat in agg.themeStats) {
            val content = LessonLibrary.contentFor(stat.theme)
            val priority = (stat.gamesAffected * 10 + (stat.avgSeverity * 5).toInt())
            val summary = buildString {
                append("This came up in ${stat.gamesAffected} of your ${stat.totalGames} games")
                append(" (${stat.frequencyPct.toInt()}%). ")
                append("Addressing it is one of the highest-value things you can do right now.")
            }
            lessons.add(
                Lesson(
                    id = "theme_${stat.theme.name}",
                    title = stat.theme.title,
                    category = content.category,
                    summary = summary,
                    theory = content.theory,
                    principles = content.principles,
                    drills = content.drills,
                    examples = stat.examples,
                    priority = priority
                )
            )
        }

        // An opening-focused lesson if there's a clear weak opening.
        val worst = agg.worstOpenings(minGames = 3).firstOrNull()
        val best = agg.bestOpenings(minGames = 3).firstOrNull()
        if (worst != null && worst.scorePct < 45.0) {
            val accNote = worst.avgAccuracy?.let {
                " Your average accuracy in it is ${"%.1f".format(it)}%."
            } ?: ""
            lessons.add(
                Lesson(
                    id = "opening_${worst.family}",
                    title = "Repair your results in the ${worst.family}",
                    category = "Opening repertoire",
                    summary = "You score only ${worst.scorePct.toInt()}% across ${worst.games} games in the " +
                        "${worst.family}.$accNote This opening is dragging your results down.",
                    theory = listOf(
                        "Your statistics show the ${worst.family} is a recurring problem. Poor results in one " +
                            "opening usually mean one of three things: you don't know the key plans (not just the " +
                            "moves), you keep walking into the same tactical or strategic idea, or the opening " +
                            "simply doesn't suit your style.",
                        "Fixing an opening is not about memorising twenty moves of theory. It is about " +
                            "understanding the typical pawn structures, where each piece belongs, and the standard " +
                            "plans and pawn breaks for both sides. Once you know the ideas, you'll find good moves " +
                            "even when the opponent leaves theory.",
                        best?.let {
                            "By contrast you score ${it.scorePct.toInt()}% in the ${it.family} — study what makes " +
                                "you comfortable there and look for the same kind of positions."
                        } ?: "Pick structures that match the positions you already handle well."
                    ),
                    principles = listOf(
                        "Learn the typical middlegame plans and pawn breaks of the ${worst.family}, not just move orders.",
                        "After each loss in it, note the exact moment your position went wrong.",
                        "Consider whether a different, more comfortable system would suit you better.",
                        "Build a short, principled repertoire you actually understand."
                    ),
                    drills = listOf(
                        "Study 3–5 annotated master games in the ${worst.family} and write down the main plans.",
                        "Replay your own losses in it side by side to spot the recurring mistake.",
                        "Play 10 training games focusing only on reaching a good middlegame from this opening."
                    ),
                    examples = emptyList(),
                    priority = 35
                )
            )
        }

        return lessons.sortedByDescending { it.priority }
    }

    /** A compact textual study plan, handy for sharing or feeding to the AI coach. */
    fun studyPlanSummary(agg: Aggregate): String = buildString {
        appendLine("Player: ${agg.username}")
        appendLine("Games: ${agg.totalGames}  Record: ${agg.wins}W/${agg.draws}D/${agg.losses}L " +
            "(${agg.winRate.toInt()}% wins)")
        agg.overallAccuracy?.let { appendLine("Overall accuracy: ${"%.1f".format(it)}%") }
        agg.phase.let { p ->
            val parts = buildList {
                p.opening?.let { add("opening ${"%.0f".format(it)}%") }
                p.middlegame?.let { add("middlegame ${"%.0f".format(it)}%") }
                p.endgame?.let { add("endgame ${"%.0f".format(it)}%") }
            }
            if (parts.isNotEmpty()) appendLine("Accuracy by phase: ${parts.joinToString(", ")}")
        }
        appendLine("Errors: ${agg.totalInaccuracies} inaccuracies, ${agg.totalMistakes} mistakes, " +
            "${agg.totalBlunders} blunders")
        appendLine()
        appendLine("Best openings:")
        agg.bestOpenings().forEach { appendLine("  - ${it.family}: ${it.scorePct.toInt()}% over ${it.games} games") }
        appendLine("Weakest openings:")
        agg.worstOpenings().forEach { appendLine("  - ${it.family}: ${it.scorePct.toInt()}% over ${it.games} games") }
        appendLine()
        appendLine("Most frequent strategic problems:")
        agg.themeStats.take(6).forEach {
            appendLine("  - ${it.theme.title}: in ${it.gamesAffected}/${it.totalGames} games. " +
                (it.examples.firstOrNull()?.detail ?: ""))
        }
    }
}
