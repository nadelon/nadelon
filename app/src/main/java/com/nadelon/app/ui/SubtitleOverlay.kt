package com.nadelon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadelon.app.model.SubtitleCue

@Composable
fun BoxScope.SubtitleOverlay(
    cue: SubtitleCue?,
    onWordTap: (word: String, line: String) -> Unit,
) {
    if (cue == null) return
    val lines = remember(cue) { cue.text.lines().filter { it.isNotBlank() } }
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { line ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                WordFlow(line = line, onWordTap = { w -> onWordTap(w, line) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordFlow(line: String, onWordTap: (String) -> Unit) {
    val tokens = remember(line) { tokenize(line) }
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        tokens.forEach { token ->
            if (token.isWord) {
                Text(
                    text = token.text,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            onClickLabel = "Translate",
                            role = Role.Button,
                        ) { onWordTap(token.text) }
                        .semantics { contentDescription = "Translate ${token.text}" }
                        .padding(horizontal = 2.dp)
                )
            } else {
                Text(
                    text = token.text,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private data class Token(val text: String, val isWord: Boolean)

private val TOKEN_REGEX = Regex("""[\p{L}\p{M}\p{N}][\p{L}\p{M}\p{N}'’\-]*|[^\p{L}\p{M}\p{N}]+""")

private fun tokenize(line: String): List<Token> {
    val result = mutableListOf<Token>()
    for (match in TOKEN_REGEX.findAll(line)) {
        val text = match.value
        val isWord = text.firstOrNull()?.let { it.isLetterOrDigit() } == true
        result += Token(text, isWord)
    }
    return result
}
