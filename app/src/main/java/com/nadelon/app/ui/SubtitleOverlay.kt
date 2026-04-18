package com.nadelon.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadelon.app.model.SubtitleCue
import com.nadelon.app.ui.theme.Nadelon

// The subtitle is a line on a page, not a caption. A warm parchment-wash lies across the
// bottom of the frame like the bottom of a book; a single hair-line rule separates it from
// the film above. Tapped words ripple in lamplight — a highlighter stroke, not a button.
// Long-press anywhere on the bar to translate the full line.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.SubtitleOverlay(
    cue: SubtitleCue?,
    onWordTap: (word: String, line: String) -> Unit,
    onLineLongPress: (line: String) -> Unit,
) {
    if (cue == null) return
    val lines = remember(cue) { cue.text.lines().filter { it.isNotBlank() } }
    val palette = Nadelon.palette

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(
                horizontal = Nadelon.Space.snug,
                vertical = Nadelon.Space.column
            )
            .clip(RoundedCornerShape(Nadelon.Radius.card))
            // plate at 0.86 alpha — a wash, not a bar. The film shows through faintly.
            .background(palette.plate.copy(alpha = 0.86f))
            .border(
                width = 0.5.dp,
                color = palette.oakStrong,
                shape = RoundedCornerShape(Nadelon.Radius.card)
            )
            .combinedClickable(
                onClick = {},
                onLongClick = { onLineLongPress(cue.text) },
                onLongClickLabel = "Translate line",
            )
            .padding(
                horizontal = Nadelon.Space.column,
                vertical = Nadelon.Space.reading
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Nadelon.Space.tight)
        ) {
            lines.forEach { line ->
                WordFlow(
                    line = line,
                    inkColor = palette.ink,
                    onWordTap = { w -> onWordTap(w, line) },
                    onLineLongPress = onLineLongPress,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun WordFlow(
    line: String,
    inkColor: Color,
    onWordTap: (String) -> Unit,
    onLineLongPress: (String) -> Unit,
) {
    val tokens = remember(line) { tokenize(line) }
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        tokens.forEach { token ->
            if (token.isWord) {
                Text(
                    text = token.text,
                    color = inkColor,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(Nadelon.Radius.input))
                        .combinedClickable(
                            onClickLabel = "Translate",
                            role = Role.Button,
                            onClick = { onWordTap(token.text) },
                            onLongClick = { onLineLongPress(line) },
                            onLongClickLabel = "Translate line",
                        )
                        .semantics { contentDescription = "Translate ${token.text}" }
                        .padding(horizontal = 2.dp)
                )
            } else {
                Text(
                    text = token.text,
                    color = inkColor,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private data class Token(val text: String, val isWord: Boolean)

private val TOKEN_REGEX = Regex("""[\p{L}\p{M}\p{N}][\p{L}\p{M}\p{N}''\-]*|[^\p{L}\p{M}\p{N}]+""")

private fun tokenize(line: String): List<Token> {
    val result = mutableListOf<Token>()
    for (match in TOKEN_REGEX.findAll(line)) {
        val text = match.value
        val isWord = text.firstOrNull()?.let { it.isLetterOrDigit() } == true
        result += Token(text, isWord)
    }
    return result
}
