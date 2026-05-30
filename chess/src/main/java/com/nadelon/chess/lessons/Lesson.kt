package com.nadelon.chess.lessons

import com.nadelon.chess.analysis.ThemeExample

/** A theoretical lesson, personalised with the user's own example positions. */
class Lesson(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,            // why this lesson is in *your* plan
    val theory: List<String>,       // explanatory paragraphs
    val principles: List<String>,   // concrete, actionable rules
    val drills: List<String>,       // ways to practise
    val examples: List<ThemeExample>,
    val priority: Int,
    val source: String = "Built-in"
)
