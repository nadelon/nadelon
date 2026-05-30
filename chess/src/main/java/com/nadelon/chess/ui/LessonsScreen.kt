package com.nadelon.chess.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nadelon.chess.lessons.Lesson

@Composable
fun LessonsScreen(lessons: List<Lesson>) {
    var selected by remember { mutableStateOf<Lesson?>(null) }
    val current = selected
    if (current != null) {
        BackHandler { selected = null }
        LessonDetail(current) { selected = null }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Your study plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                Text(
                    "Theoretical lessons ranked by how much they'll help you, each built from patterns in " +
                        "your own games. Tap to open.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            items(lessons) { lesson ->
                Card(Modifier.fillMaxWidth().clickable { selected = lesson }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(lesson.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (lesson.source == "AI coach") {
                                AssistChip(onClick = {}, label = { Text("AI") })
                            }
                        }
                        Text(lesson.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(lesson.summary, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonDetail(lesson: Lesson, onBack: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("‹ Back to lessons", color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBack() })
        }
        item {
            Text(lesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(lesson.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            Card(colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )) {
                Text(lesson.summary, Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        if (lesson.theory.isNotEmpty()) {
            item { SectionHeader("The idea") }
            items(lesson.theory) { para ->
                Text(para, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (lesson.principles.isNotEmpty()) {
            item { SectionHeader("Principles to apply") }
            item { BulletList(lesson.principles) }
        }

        if (lesson.examples.isNotEmpty()) {
            item { SectionHeader("From your games") }
            items(lesson.examples) { ex ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(ex.gameLabel, fontWeight = FontWeight.SemiBold)
                        Text(ex.detail, style = MaterialTheme.typography.bodySmall)
                        BoardView(ex.fen)
                    }
                }
            }
        }

        if (lesson.drills.isNotEmpty()) {
            item { SectionHeader("How to practise") }
            item { BulletList(lesson.drills) }
        }

        item { Divider() }
        item {
            Text("Source: ${lesson.source}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
