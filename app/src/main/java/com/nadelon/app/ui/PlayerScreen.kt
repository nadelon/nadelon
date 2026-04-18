package com.nadelon.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nadelon.app.data.SubtitleParser
import com.nadelon.app.data.queryDisplayName
import com.nadelon.app.model.SubtitleCue
import com.nadelon.app.ui.theme.Nadelon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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

private val SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f)

private fun formatSpeed(f: Float): String {
    val s = if (f == f.toLong().toFloat()) "${f.toLong()}" else "$f"
    return "${s}×"
}

private fun formatOffsetLabel(ms: Long): String {
    val s = ms / 1000.0
    return if (s >= 0) "+%.1f s".format(s) else "%.1f s".format(s)
}

// The film dominates. Controls are a thin shelf — a bookshelf above the page, not a UI.
// Language selection reads left-to-right: "en → es", the arrow is literal ("from, to").
// "Find subtitles" lives in the margin as a small text link, not as a chunky button.
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
    val playbackSpeed by vm.playbackSpeed.collectAsState()
    val autoPauseCueEnd by vm.autoPauseCueEnd.collectAsState()
    val subtitleOffsetMs by vm.subtitleOffsetMs.collectAsState()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.playWhenReady = false
                    vm.videoUri.value?.let { uri ->
                        vm.saveResumePosition(uri, exoPlayer.currentPosition)
                    }
                }
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoUri) {
        val uri = videoUri ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        val resumePos = vm.getResumePosition(uri)
        if (resumePos > 0L) exoPlayer.seekTo(resumePos)
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    var positionMs by remember { mutableStateOf(0L) }
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) positionMs = exoPlayer.currentPosition
            delay(150)
        }
    }

    val adjustedPositionMs = positionMs - subtitleOffsetMs
    val currentCue = remember(cues, adjustedPositionMs) {
        SubtitleParser.cueAt(cues, adjustedPositionMs)
    }

    // Auto-pause at end of every subtitle line
    val prevCueHolder = remember { object { var cue: SubtitleCue? = null } }
    LaunchedEffect(currentCue) {
        val shouldPause = autoPauseCueEnd
            && prevCueHolder.cue != null
            && currentCue != prevCueHolder.cue
        prevCueHolder.cue = currentCue
        if (shouldPause) exoPlayer.pause()
    }

    // Cue navigation — seek is offset-aware
    val onReplayCue = {
        currentCue?.let { exoPlayer.seekTo(it.startMs + subtitleOffsetMs) }
        Unit
    }
    val onPrevCue = {
        val adj = positionMs - subtitleOffsetMs
        val target = if (currentCue != null && adj - currentCue.startMs > 1000L) {
            currentCue
        } else {
            cues.lastOrNull { it.endMs < (currentCue?.startMs ?: adj) }
        }
        target?.let { exoPlayer.seekTo(it.startMs + subtitleOffsetMs) }
        Unit
    }
    val onNextCue = {
        val adj = positionMs - subtitleOffsetMs
        cues.firstOrNull { it.startMs > adj }
            ?.let { exoPlayer.seekTo(it.startMs + subtitleOffsetMs) }
        Unit
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
        uri?.let { vm.loadSubtitles(it, context.queryDisplayName(it)) }
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
        // The frame. When empty, the room is empty — a large invitation to open a film.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Nadelon.Radius.card))
                .background(Color.Black)
                .border(
                    width = 0.5.dp,
                    color = palette.oakStrong,
                    shape = RoundedCornerShape(Nadelon.Radius.card)
                )
        ) {
            if (videoUri != null) {
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
                    },
                    onLineLongPress = { line ->
                        if (pauseOnTap && exoPlayer.isPlaying) exoPlayer.pause()
                        vm.translateLine(line)
                    }
                )
            } else {
                EmptyFrame(
                    onPickVideo = { videoPicker.launch(arrayOf("video/*")) }
                )
            }
        }

        // Cue controls: « replay » + speed toggle + line-end auto-pause
        if (videoUri != null) {
            CueControlsRow(
                cues = cues,
                currentCue = currentCue,
                onPrevCue = onPrevCue,
                onReplayCue = onReplayCue,
                onNextCue = onNextCue,
                playbackSpeed = playbackSpeed,
                onSpeedChange = vm::setPlaybackSpeed,
                autoPauseCueEnd = autoPauseCueEnd,
                onToggleAutoPause = vm::toggleAutoPauseCueEnd,
            )
        }

        // Shelf — a thin strip of controls. Becomes even quieter once a video is loaded.
        Shelf(
            hasVideo = videoUri != null,
            sourceLang = sourceLang,
            targetLang = targetLang,
            onPickVideo = { videoPicker.launch(arrayOf("video/*")) },
            onPickSubs = {
                subsPicker.launch(
                    arrayOf(
                        "application/x-subrip",
                        "text/vtt",
                        "text/plain",
                        "*/*"
                    )
                )
            },
            onSourceLang = vm::setSourceLang,
            onTargetLang = vm::setTargetLang,
            onFindSubtitles = { vm.searchOpenSubs() },
            findBusy = openSubs.busy,
        )

        // Margin note: cue count + pause-on-tap as a subtle toggle, never a chip.
        MarginRow(
            cueCount = cues.size,
            pauseOnTap = pauseOnTap,
            onTogglePause = vm::togglePauseOnTap,
        )

        // Subtitle timing offset — only shown when subtitles are loaded
        if (cues.isNotEmpty()) {
            OffsetRow(
                offsetMs = subtitleOffsetMs,
                onAdjust = vm::adjustSubtitleOffset,
            )
        }

        openSubs.message?.takeIf { !openSubs.showResults }?.let { msg ->
            MarginNote(msg)
        }

        selected?.let { sel ->
            IndexCard(
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
private fun EmptyFrame(onPickVideo: () -> Unit) {
    val palette = Nadelon.palette
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = palette.margin,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(Nadelon.Space.snug))
        Text(
            "An empty frame.",
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = palette.inkFaint,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(Nadelon.Space.reading))
        TextLink(text = "Open a film", onClick = onPickVideo)
    }
}

@Composable
private fun CueControlsRow(
    cues: List<SubtitleCue>,
    currentCue: SubtitleCue?,
    onPrevCue: () -> Unit,
    onReplayCue: () -> Unit,
    onNextCue: () -> Unit,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    autoPauseCueEnd: Boolean,
    onToggleAutoPause: () -> Unit,
) {
    val palette = Nadelon.palette
    val hasCues = cues.isNotEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevCue, enabled = hasCues) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "Previous cue",
                tint = if (hasCues) palette.inkFaint else palette.muted,
            )
        }
        IconButton(onClick = onReplayCue, enabled = currentCue != null) {
            Icon(
                Icons.Filled.Replay,
                contentDescription = "Replay cue",
                tint = if (currentCue != null) palette.inkFaint else palette.muted,
            )
        }
        IconButton(onClick = onNextCue, enabled = hasCues) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "Next cue",
                tint = if (hasCues) palette.inkFaint else palette.muted,
            )
        }
        Spacer(Modifier.weight(1f))
        SPEEDS.forEach { speed ->
            val selected = speed == playbackSpeed
            Text(
                text = formatSpeed(speed),
                color = if (selected) palette.lamplight else palette.margin,
                style = MaterialTheme.typography.labelMedium,
                textDecoration = if (selected) TextDecoration.Underline else TextDecoration.None,
                modifier = Modifier
                    .clickable { onSpeedChange(speed) }
                    .padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.width(Nadelon.Space.snug))
        Text(
            text = if (autoPauseCueEnd) "line end · on" else "line end · off",
            style = MaterialTheme.typography.labelMedium,
            color = if (autoPauseCueEnd) palette.lamplight else palette.margin,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onToggleAutoPause() }
        )
    }
}

@Composable
private fun OffsetRow(offsetMs: Long, onAdjust: (Long) -> Unit) {
    val palette = Nadelon.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.tight),
    ) {
        Text(
            "sub offset",
            style = MaterialTheme.typography.labelMedium,
            color = palette.margin,
        )
        Text(
            "−",
            color = palette.inkFaint,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable { onAdjust(-500L) },
        )
        Text(
            formatOffsetLabel(offsetMs),
            style = MaterialTheme.typography.labelMedium,
            color = palette.ink,
        )
        Text(
            "+",
            color = palette.inkFaint,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable { onAdjust(+500L) },
        )
    }
}

@Composable
private fun Shelf(
    hasVideo: Boolean,
    sourceLang: String,
    targetLang: String,
    onPickVideo: () -> Unit,
    onPickSubs: () -> Unit,
    onSourceLang: (String) -> Unit,
    onTargetLang: (String) -> Unit,
    onFindSubtitles: () -> Unit,
    findBusy: Boolean,
) {
    val palette = Nadelon.palette
    Column(verticalArrangement = Arrangement.spacedBy(Nadelon.Space.snug)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.snug),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShelfButton(
                label = if (hasVideo) "Film" else "Open film",
                onClick = onPickVideo,
                modifier = Modifier.weight(1f),
            )
            ShelfButton(
                label = "Subtitle",
                onClick = onPickSubs,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.snug),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LangPill(
                selected = sourceLang,
                onSelected = onSourceLang,
                modifier = Modifier.weight(1f),
            )
            Text(
                "→",
                color = palette.margin,
                style = MaterialTheme.typography.titleLarge,
            )
            LangPill(
                selected = targetLang,
                onSelected = onTargetLang,
                options = LANG_OPTIONS.filter { it.first != "auto" },
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onFindSubtitles,
                enabled = !findBusy,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Find subtitles",
                    tint = if (findBusy) palette.muted else palette.inkFaint,
                )
            }
        }
    }
}

@Composable
private fun ShelfButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Nadelon.palette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Nadelon.Radius.input),
        color = palette.page,
        border = BorderStroke(0.5.dp, palette.oakStrong),
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = palette.ink,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(
                horizontal = Nadelon.Space.reading,
                vertical = Nadelon.Space.snug + Nadelon.Space.tight,
            )
        )
    }
}

@Composable
private fun LangPill(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<Pair<String, String>> = LANG_OPTIONS,
) {
    val palette = Nadelon.palette
    var expanded by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selected }?.second ?: selected
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(Nadelon.Radius.input),
            color = palette.dusk,
            border = BorderStroke(0.5.dp, palette.oak),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = Nadelon.Space.reading,
                    vertical = Nadelon.Space.snug,
                )
            ) {
                Text(
                    text = selected.uppercase(),
                    color = palette.ink,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "  $label",
                    color = palette.inkFaint,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, display) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                code.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                color = palette.margin,
                                modifier = Modifier.width(36.dp),
                            )
                            Text(display, color = palette.ink)
                        }
                    },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MarginRow(
    cueCount: Int,
    pauseOnTap: Boolean,
    onTogglePause: () -> Unit,
) {
    val palette = Nadelon.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (cueCount == 0) "—  no subtitles yet"
                   else "$cueCount cues · tap a word to annotate",
            style = MaterialTheme.typography.labelMedium,
            color = palette.margin,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (pauseOnTap) "pause on tap · on" else "pause on tap · off",
            style = MaterialTheme.typography.labelMedium,
            color = if (pauseOnTap) palette.lamplight else palette.margin,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onTogglePause() }
        )
    }
}

@Composable
private fun MarginNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        color = Nadelon.palette.margin,
    )
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Nadelon.palette.lamplight,
        style = MaterialTheme.typography.labelLarge,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable { onClick() }
    )
}

// A slipped index card — warmer surface, serif headword, the translation underlined like
// a dictionary definition. Actions sit as text at the foot of the card, not chunky buttons.
@Composable
private fun IndexCard(
    term: String,
    translation: String?,
    state: TranslationState,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = Nadelon.palette
    Surface(
        shape = RoundedCornerShape(Nadelon.Radius.card),
        color = palette.plate,
        border = BorderStroke(0.5.dp, palette.oakStrong),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Nadelon.Space.column,
                vertical = Nadelon.Space.reading,
            )
        ) {
            Text(
                text = term,
                color = palette.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(Nadelon.Space.tight))
            when (state) {
                TranslationState.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = palette.margin,
                    )
                    Text(
                        "  looking it up…",
                        color = palette.margin,
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TranslationState.Success -> Text(
                    text = translation.orEmpty(),
                    color = palette.inkFaint,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyLarge,
                )
                TranslationState.Failed -> Text(
                    "translation unavailable",
                    color = palette.redInk,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(Nadelon.Space.reading))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextLink(text = "close", onClick = onDismiss)
                Spacer(Modifier.width(Nadelon.Space.gutter))
                val canSave = state == TranslationState.Success && !translation.isNullOrBlank()
                Text(
                    text = "save to notebook",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (canSave) palette.lamplight else palette.muted,
                    textDecoration = TextDecoration.Underline,
                    modifier = if (canSave) Modifier.clickable { onSave() } else Modifier,
                )
            }
        }
    }
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
    val palette = Nadelon.palette
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Nadelon.Radius.dialog),
        confirmButton = { TextButton(onClick = onDismiss) { Text("close") } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = palette.lamplight,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Nadelon.Space.snug))
                Text(
                    "Catalogue — subtitles",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Nadelon.Space.reading)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.snug),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("title") },
                        singleLine = true,
                        shape = RoundedCornerShape(Nadelon.Radius.input),
                    )
                    ShelfButton(
                        label = if (busy) "…" else "Search",
                        onClick = onSearch,
                    )
                }
                if (busy) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.snug),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = palette.margin,
                        )
                        Text(
                            "working…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = palette.margin,
                        )
                    }
                }
                message?.let { MarginNote(it) }
                if (results.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Nadelon.Space.hair),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(results, key = { it.fileId }) { r ->
                            CatalogueEntry(result = r, onPick = { onPick(r) })
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun CatalogueEntry(
    result: com.nadelon.app.data.OpenSubResult,
    onPick: () -> Unit,
) {
    val palette = Nadelon.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick() }
            .padding(vertical = Nadelon.Space.snug + Nadelon.Space.tight)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = result.featureTitle.ifBlank { result.fileName },
                color = palette.ink,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = "Download",
                tint = palette.margin,
                modifier = Modifier.size(18.dp),
            )
        }
        val meta = buildString {
            append(result.language.uppercase())
            if (result.release.isNotBlank()) append("  ·  ${result.release}")
            append("  ·  ${result.downloads} dl")
            if (result.fromTrusted) append("  ·  trusted")
        }
        Text(
            text = meta,
            style = MaterialTheme.typography.labelMedium,
            color = palette.margin,
        )
        Spacer(Modifier.height(Nadelon.Space.snug))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(palette.oak)
        )
    }
}
