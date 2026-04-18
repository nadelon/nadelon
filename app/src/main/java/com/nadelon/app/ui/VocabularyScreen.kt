package com.nadelon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nadelon.app.ui.theme.Nadelon

// The notebook. A column of entries separated by thin oak rules, the way a ruled page
// works. The headword is serif bold; the translation is the dictionary gloss below it;
// the original subtitle line sits as a pull-quote, italic, recessed.
@Composable
fun VocabularyScreen(vm: AppViewModel = viewModel()) {
    val entries by vm.vocabulary.collectAsState()
    var query by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    val filtered = remember(entries, query) {
        if (query.isBlank()) entries else entries.filter {
            it.term.contains(query, ignoreCase = true) ||
                it.translation.contains(query, ignoreCase = true) ||
                it.context.contains(query, ignoreCase = true)
        }
    }

    val palette = Nadelon.palette

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = Nadelon.Space.column,
                vertical = Nadelon.Space.reading
            ),
        verticalArrangement = Arrangement.spacedBy(Nadelon.Space.reading)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Notebook",
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.ink,
                )
                Text(
                    text = if (entries.isEmpty()) "nothing annotated yet"
                           else "${entries.size} headwords",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.margin,
                )
            }
            if (entries.isNotEmpty()) {
                Text(
                    "clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.redInk,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { confirmClear = true }
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(Nadelon.Radius.input),
            placeholder = {
                Text(
                    "search headwords, glosses, or lines",
                    fontStyle = FontStyle.Italic,
                    color = palette.margin,
                )
            }
        )

        if (entries.isEmpty()) {
            EmptyNotebook()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Nadelon.Space.hair)
            ) {
                items(filtered, key = { it.key }) { entry ->
                    NotebookEntry(
                        term = entry.term,
                        translation = entry.translation,
                        context = entry.context,
                        sourceLang = entry.sourceLang,
                        targetLang = entry.targetLang,
                        onRemove = { vm.removeVocab(entry.key) }
                    )
                    RuledLine()
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(Nadelon.Radius.dialog),
            title = {
                Text(
                    "Tear out every page?",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    "Every headword will be removed. Nothing can be recovered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkFaint,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearVocab()
                    confirmClear = false
                }) {
                    Text(
                        "tear out",
                        color = palette.redInk,
                        style = MaterialTheme.typography.labelLarge,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(
                        "keep",
                        color = palette.inkFaint,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        )
    }
}

@Composable
private fun NotebookEntry(
    term: String,
    translation: String,
    context: String,
    sourceLang: String,
    targetLang: String,
    onRemove: () -> Unit,
) {
    val palette = Nadelon.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Nadelon.Space.reading),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = term,
                    color = palette.ink,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.width(Nadelon.Space.snug))
                Text(
                    text = "${sourceLang.uppercase()}→${targetLang.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.margin,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(Nadelon.Space.tight))
            Text(
                text = translation,
                color = palette.inkFaint,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (context.isNotBlank()) {
                Spacer(Modifier.height(Nadelon.Space.snug))
                // Pull-quote — indented, with a vertical rule whose height matches the text.
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(palette.oakStrong)
                    )
                    Spacer(Modifier.width(Nadelon.Space.snug))
                    Text(
                        text = context,
                        color = palette.margin,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove $term",
                tint = palette.margin,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RuledLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Nadelon.palette.oak)
    )
}

@Composable
private fun EmptyNotebook() {
    val palette = Nadelon.palette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = Nadelon.Space.gutter),
        verticalArrangement = Arrangement.spacedBy(Nadelon.Space.snug),
    ) {
        Text(
            "The notebook is blank.",
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = palette.inkFaint,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Tap a word in the subtitle, then save it. It will be filed here as a headword with the line it came from.",
            fontFamily = FontFamily.Serif,
            color = palette.margin,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
