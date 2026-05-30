package com.nadelon.chess.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nadelon.chess.analysis.GameReport
import com.nadelon.chess.analysis.PlayerResult
import com.nadelon.chess.core.Board
import com.nadelon.chess.engine.Quality

@Composable
fun GamesScreen(reports: List<GameReport>) {
    var selected by remember { mutableStateOf<GameReport?>(null) }
    val current = selected
    if (current != null) {
        BackHandler { selected = null }
        GameDetail(current) { selected = null }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Your games", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(reports) { r -> GameRow(r) { selected = r } }
        }
    }
}

@Composable
private fun GameRow(r: GameReport, onClick: () -> Unit) {
    val (label, color) = when (r.outcome) {
        PlayerResult.WIN -> "Win" to Color(0xFF2E7D32)
        PlayerResult.LOSS -> "Loss" to Color(0xFFC62828)
        PlayerResult.DRAW -> "Draw" to Color(0xFF9E9D24)
        else -> "—" to MaterialTheme.colorScheme.onSurface
    }
    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "vs ${r.opponent}" + (r.opponentElo?.let { " ($it)" } ?: ""),
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                )
                Text(label, color = color, fontWeight = FontWeight.Bold)
            }
            Text(
                "${if (r.playerColor == Board.WHITE) "White" else "Black"} · ${r.opening.family}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                (if (!r.accuracy.isNaN()) "Accuracy ${r.accuracy.toInt()}% · " else "") +
                    "${r.blunders} blunders, ${r.mistakes} mistakes · ${r.moveCount} moves",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GameDetail(r: GameReport, onBack: () -> Unit) {
    var idx by remember { mutableIntStateOf(-1) } // -1 = starting position
    val plies = r.plies
    val fen = if (idx < 0) Board.STARTPOS else plies[idx].fenAfter
    val highlight = if (idx < 0) emptySet() else setOf(plies[idx].fromSq, plies[idx].toSq)
    val ply = plies.getOrNull(idx)

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("‹ Back to games", color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBack() })
        }
        item {
            Text(
                "${r.white} vs ${r.black}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
            )
            Text(
                "${r.opening.eco} ${r.opening.name} · ${r.result}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            BoardView(fen, highlight = highlight, flipped = r.playerColor == Board.BLACK)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { idx = -1 }, modifier = Modifier.weight(1f)) { Text("⏮") }
                OutlinedButton(
                    onClick = { if (idx >= 0) idx-- },
                    modifier = Modifier.weight(1f)
                ) { Text("‹ Prev") }
                Button(
                    onClick = { if (idx < plies.size - 1) idx++ },
                    modifier = Modifier.weight(1f)
                ) { Text("Next ›") }
            }
        }
        item {
            if (ply != null) {
                val moveNo = ply.ply / 2 + 1
                val dots = if (ply.side == Board.WHITE) "." else "..."
                MoveAnnotation(moveNo, dots, ply.san, ply.side == r.playerColor, ply.verdict?.let {
                    QualityNote(it.quality, it.bestSan)
                })
            } else {
                Text("Starting position. Use Next to step through the game.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { SectionHeader("Moves") }
        item { MoveTable(r) { tappedIdx -> idx = tappedIdx } }
    }
}

private class QualityNote(val quality: Quality, val bestSan: String?)

@Composable
private fun MoveAnnotation(moveNo: Int, dots: String, san: String, byPlayer: Boolean, note: QualityNote?) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = note?.let { qualityColor(it.quality).copy(alpha = 0.18f) }
                ?: MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$moveNo$dots $san", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall)
            if (note != null && byPlayer) {
                Text(qualityLabel(note.quality), color = qualityColor(note.quality), fontWeight = FontWeight.SemiBold)
                if (note.quality in setOf(Quality.INACCURACY, Quality.MISTAKE, Quality.BLUNDER) &&
                    note.bestSan != null
                ) {
                    Text("Better was ${note.bestSan}.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MoveTable(r: GameReport, onTap: (Int) -> Unit) {
    Column {
        var i = 0
        val plies = r.plies
        while (i < plies.size) {
            val moveNo = plies[i].ply / 2 + 1
            val white = if (plies[i].side == Board.WHITE) plies[i] else null
            val black = when {
                white != null && i + 1 < plies.size && plies[i + 1].side == Board.BLACK -> plies[i + 1]
                white == null -> plies[i]
                else -> null
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text("$moveNo.", Modifier.padding(end = 6.dp), style = MaterialTheme.typography.bodySmall)
                if (white != null) {
                    val wIdx = i
                    MoveCell(white.san, white.verdict?.quality.takeIf { white.side == r.playerColor },
                        Modifier.weight(1f)) { onTap(wIdx) }
                } else {
                    Box(Modifier.weight(1f))
                }
                if (black != null) {
                    val bIdx = if (white != null) i + 1 else i
                    MoveCell(black.san, black.verdict?.quality.takeIf { black.side == r.playerColor },
                        Modifier.weight(1f)) { onTap(bIdx) }
                } else {
                    Box(Modifier.weight(1f))
                }
            }
            i += if (white != null && black != null) 2 else 1
        }
    }
}

@Composable
private fun MoveCell(san: String, quality: Quality?, modifier: Modifier, onTap: () -> Unit) {
    val mark = when (quality) {
        Quality.BLUNDER -> " ??"
        Quality.MISTAKE -> " ?"
        Quality.INACCURACY -> " ?!"
        else -> ""
    }
    Text(
        text = san + mark,
        modifier = modifier.clickable { onTap() }.padding(2.dp),
        color = quality?.let { qualityColor(it) } ?: MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (quality == Quality.MISTAKE || quality == Quality.BLUNDER) FontWeight.Bold else FontWeight.Normal
    )
}

private fun qualityColor(q: Quality): Color = when (q) {
    Quality.BEST -> Color(0xFF2E7D32)
    Quality.GOOD -> Color(0xFF558B2F)
    Quality.INACCURACY -> Color(0xFFF9A825)
    Quality.MISTAKE -> Color(0xFFEF6C00)
    Quality.BLUNDER -> Color(0xFFC62828)
}

private fun qualityLabel(q: Quality): String = when (q) {
    Quality.BEST -> "Best move"
    Quality.GOOD -> "Good move"
    Quality.INACCURACY -> "Inaccuracy"
    Quality.MISTAKE -> "Mistake"
    Quality.BLUNDER -> "Blunder"
}
