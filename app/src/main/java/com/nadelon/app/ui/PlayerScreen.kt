package com.nadelon.app.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nadelon.app.data.SubtitleParser
import kotlinx.coroutines.delay

private val LANG_OPTIONS = listOf(
    "auto" to "Auto-detect",
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "it" to "Italian",
    "pt" to "Portuguese",
    "nl" to "Dutch",
    "sv" to "Swedish",
    "ru" to "Russian",
    "ja" to "Japanese",
    "ko" to "Korean",
    "zh" to "Chinese",
    "ar" to "Arabic",
    "tr" to "Turkish",
    "hi" to "Hindi",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val videoUri by vm.videoUri.collectAsState()
    val cues by vm.cues.collectAsState()
    val sourceLang by vm.sourceLang.collectAsState()
    val targetLang by vm.targetLang.collectAsState()
    val pauseOnTap by vm.pauseOnTap.collectAsState()
    val selected by vm.selectedWord.collectAsState()
    val openSubs by vm.openSubsState.collectAsState()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    LaunchedEffect(videoUri) {
        val uri = videoUri ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    var positionMs by remember { mutableStateOf(0L) }
    LaunchedEffect(exoPlayer) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            delay(100)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.setVideo(it)
        }
    }
    val subsPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val name = queryDisplayName(context, it)
            vm.loadSubtitles(it, name)
        }
    }

    val currentCue = remember(cues, positionMs) {
        SubtitleParser.cueAt(cues, positionMs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { videoPicker.launch(arrayOf("video/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.VideoLibrary, contentDescription = null)
                Text("  Video", maxLines = 1)
            }
            OutlinedButton(
                onClick = {
                    subsPicker.launch(
                        arrayOf(
                            "application/x-subrip",
                            "text/vtt",
                            "text/plain",
                            "*/*"
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Subtitles, contentDescription = null)
                Text("  Subtitles", maxLines = 1)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            LangDropdown(
                label = "From",
                selected = sourceLang,
                onSelected = vm::setSourceLang,
                modifier = Modifier.weight(1f)
            )
            LangDropdown(
                label = "To",
                selected = targetLang,
                onSelected = vm::setTargetLang,
                options = LANG_OPTIONS.filter { it.first != "auto" },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { vm.searchOpenSubs() },
                enabled = !openSubs.busy,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Text("  Find subtitles", maxLines = 1)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black, RoundedCornerShape(10.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        player = exoPlayer
                        setShowSubtitleButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            SubtitleOverlay(
                cue = currentCue,
                onWordTap = { word, line ->
                    if (pauseOnTap && exoPlayer.isPlaying) exoPlayer.pause()
                    vm.selectWord(word, line)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = vm::togglePauseOnTap,
                label = {
                    Text(if (pauseOnTap) "Pause on tap: on" else "Pause on tap: off")
                }
            )
            Text(
                text = if (cues.isEmpty()) "No subtitles loaded." else "${cues.size} cues loaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        openSubs.message?.takeIf { !openSubs.showResults }?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        selected?.let { sel ->
            TranslationCard(
                term = sel.term,
                translation = sel.translation,
                state = sel.state,
                onSave = vm::saveSelected,
                onDismiss = vm::dismissWord
            )
        }
    }

    if (openSubs.showResults) {
        OpenSubsDialog(
            query = openSubs.lastQuery,
            busy = openSubs.busy,
            results = openSubs.results,
            message = openSubs.message,
            onQueryChange = vm::setOpenSubsQuery,
            onSearch = { vm.searchOpenSubs(openSubs.lastQuery) },
            onPick = { r -> vm.downloadSubtitle(r.fileId, r.fileName) },
            onDismiss = vm::dismissOpenSubsResults
        )
    }
}

@Composable
private fun TranslationCard(
    term: String,
    translation: String?,
    state: TranslationState,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(term, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            when (state) {
                TranslationState.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        "  Translating…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TranslationState.Success -> Text(
                    translation.orEmpty(),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                TranslationState.Failed -> Text(
                    "Translation unavailable.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Close") }
                Button(
                    onClick = onSave,
                    enabled = state == TranslationState.Success && !translation.isNullOrBlank()
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun LangDropdown(
    label: String,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<Pair<String, String>> = LANG_OPTIONS
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $selectedLabel", maxLines = 1)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenSubsDialog(
    query: String,
    busy: Boolean,
    results: List<com.nadelon.app.data.OpenSubResult>,
    message: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (com.nadelon.app.data.OpenSubResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Find subtitles") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Movie / show title") },
                        singleLine = true
                    )
                    Button(onClick = onSearch, enabled = !busy) { Text("Search") }
                }
                if (busy) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Working…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                message?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (results.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(results, key = { it.fileId }) { r ->
                            Surface(
                                onClick = { onPick(r) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = r.featureTitle.ifBlank { r.fileName },
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2
                                        )
                                        Icon(
                                            Icons.Filled.CloudDownload,
                                            contentDescription = "Download"
                                        )
                                    }
                                    val meta = buildString {
                                        append(r.language.uppercase())
                                        if (r.release.isNotBlank()) append(" · ${r.release}")
                                        append(" · ${r.downloads} dl")
                                        if (r.fromTrusted) append(" · trusted")
                                    }
                                    Text(
                                        meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.surface)
                        }
                    }
                }
            }
        }
    )
}
