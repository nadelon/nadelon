package com.nadelon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadelon.app.model.SubtitleCue

@Composable
fun BoxScope.SubtitleOverlay(
    cue: SubtitleCue?,
    onWordTap: (word: String, line: String) -> Unit,
) {
    if (cue == null) return
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cue.text.lines().filter { it.isNotBlank() }.forEach { line ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                ClickableWordsText(line = line, onWordTap = { word -> onWordTap(word, line) })
            }
        }
    }
}

@Composable
private fun ClickableWordsText(line: String, onWordTap: (String) -> Unit) {
    val annotated: AnnotatedString = buildAnnotatedString {
        val tokens = tokenize(line)
        tokens.forEach { token ->
            if (token.isWord) {
                pushStringAnnotation(tag = "WORD", annotation = token.text)
                withStyle(
                    SpanStyle(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                ) { append(token.text) }
                pop()
            } else {
                withStyle(SpanStyle(color = Color.White)) { append(token.text) }
            }
        }
    }
    androidx.compose.foundation.text.ClickableText(
        text = annotated,
        style = LocalTextStyle.current.copy(
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 26.sp
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "WORD", start = offset, end = offset)
                .firstOrNull()
                ?.let { onWordTap(it.item) }
        }
    )
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
