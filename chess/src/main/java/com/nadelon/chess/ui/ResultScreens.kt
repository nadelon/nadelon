package com.nadelon.chess.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import com.nadelon.chess.analysis.Aggregate
import com.nadelon.chess.analysis.OpeningStat
import com.nadelon.chess.analysis.ThemeStat

@Composable
fun OverviewScreen(agg: Aggregate) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Hi ${agg.username} — here's your report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Games", "${agg.totalGames}", Modifier.weight(1f))
                StatTile("Win rate", "${agg.winRate.toInt()}%", Modifier.weight(1f))
                StatTile(
                    "Accuracy",
                    agg.overallAccuracy?.let { "${it.toInt()}%" } ?: "—",
                    Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Wins", "${agg.wins}", Modifier.weight(1f))
                StatTile("Draws", "${agg.draws}", Modifier.weight(1f))
                StatTile("Losses", "${agg.losses}", Modifier.weight(1f))
            }
        }

        if (agg.analyzedWithEngine) {
            item { SectionHeader("Accuracy by phase") }
            item {
                Card {
                    Column(Modifier.padding(14.dp)) {
                        agg.phase.opening?.let { LabeledBar("Opening", "${it.toInt()}%", it.toFloat() / 100f) }
                        agg.phase.middlegame?.let { LabeledBar("Middlegame", "${it.toInt()}%", it.toFloat() / 100f) }
                        agg.phase.endgame?.let { LabeledBar("Endgame", "${it.toInt()}%", it.toFloat() / 100f) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Inaccuracies", "${agg.totalInaccuracies}", Modifier.weight(1f))
                    StatTile("Mistakes", "${agg.totalMistakes}", Modifier.weight(1f))
                    StatTile("Blunders", "${agg.totalBlunders}", Modifier.weight(1f))
                }
            }
        }

        val best = agg.bestOpenings().firstOrNull()
        val worst = agg.worstOpenings().firstOrNull()
        if (best != null || worst != null) {
            item { SectionHeader("Openings at a glance") }
            best?.let {
                item {
                    InfoCard(
                        "Strongest: ${it.family}",
                        "You score ${it.scorePct.toInt()}% across ${it.games} games here" +
                            (it.avgAccuracy?.let { a -> " with ${a.toInt()}% accuracy." } ?: ".")
                    )
                }
            }
            worst?.let {
                item {
                    InfoCard(
                        "Toughest: ${it.family}",
                        "You score only ${it.scorePct.toInt()}% across ${it.games} games here" +
                            (it.avgAccuracy?.let { a -> " with ${a.toInt()}% accuracy." } ?: ".")
                    )
                }
            }
        }

        if (agg.themeStats.isNotEmpty()) {
            item { SectionHeader("Top recurring weaknesses") }
            items(agg.themeStats.take(3)) { st ->
                InfoCard(
                    st.theme.title,
                    "Appears in ${st.gamesAffected}/${st.totalGames} games. " +
                        (st.examples.firstOrNull()?.detail ?: "")
                )
            }
            item {
                Text(
                    "See the Lessons tab for a full study plan addressing these.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun OpeningsScreen(agg: Aggregate) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Openings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Text(
                "Grouped by opening family, sorted by how often you play them. The bar shows your " +
                    "score (wins + ½ draws).",
                style = MaterialTheme.typography.bodySmall
            )
        }
        items(agg.byOpening) { op -> OpeningRow(op) }
    }
}

@Composable
private fun OpeningRow(op: OpeningStat) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(op.family, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("${op.scorePct.toInt()}%", fontWeight = FontWeight.Bold)
            }
            PercentBar(op.scorePct.toFloat() / 100f, Modifier.padding(vertical = 6.dp))
            Text(
                "${op.games} games · ${op.wins}W/${op.draws}D/${op.losses}L · " +
                    "${op.asWhite} as White, ${op.asBlack} as Black" +
                    (op.avgAccuracy?.let { " · ${it.toInt()}% accuracy" } ?: ""),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun MistakesScreen(agg: Aggregate) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("What's going wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Text(
                "Recurring strategic and tactical patterns detected across your games, most frequent first. " +
                    "Tap a card to see an example position from your own games.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (agg.themeStats.isEmpty()) {
            item { InfoCard("Nothing major detected", "No strong recurring weakness stood out — analyse more games for a sharper picture.") }
        }
        items(agg.themeStats) { st -> ThemeCard(st) }
    }
}

@Composable
private fun ThemeCard(st: ThemeStat) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp).clickable { expanded = !expanded },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(st.theme.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(st.theme.category, style = MaterialTheme.typography.bodySmall)
                }
                Text("${st.frequencyPct.toInt()}%", fontWeight = FontWeight.Bold)
            }
            PercentBar(st.frequencyPct.toFloat() / 100f)
            Text(
                "Seen in ${st.gamesAffected} of ${st.totalGames} games.",
                style = MaterialTheme.typography.bodySmall
            )
            if (expanded) {
                val ex = st.examples.firstOrNull()
                if (ex != null) {
                    Text(ex.detail, style = MaterialTheme.typography.bodyMedium)
                    Text(ex.gameLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    BoardView(ex.fen, Modifier.padding(top = 4.dp))
                }
            } else {
                Text("Tap for an example →", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
