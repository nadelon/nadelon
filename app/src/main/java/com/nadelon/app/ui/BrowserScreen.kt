package com.nadelon.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nadelon.app.model.SavedLink
import com.nadelon.app.ui.theme.Nadelon

// Matches direct video file and playlist URLs; deliberately excludes segments (.ts)
// and subtitles (.vtt) to avoid noise from HLS chunk requests.
private fun isVideoUrl(url: String): Boolean {
    val lower = url.lowercase()
    return (
        lower.endsWith(".mp4") || lower.contains(".mp4?") ||
        lower.endsWith(".webm") || lower.contains(".webm?") ||
        lower.endsWith(".ogg") || lower.endsWith(".ogv") ||
        lower.endsWith(".m3u8") || lower.contains(".m3u8?") ||
        lower.endsWith(".mpd") || lower.contains(".mpd?")
    ) && !lower.contains(".vtt") && !lower.contains("thumbnail") && !lower.contains("poster")
}

private fun resolveUrl(input: String): String = when {
    input.startsWith("http://") || input.startsWith("https://") -> input
    input.contains(".") && !input.contains(" ") -> "https://$input"
    else -> "https://duckduckgo.com/?q=${Uri.encode(input)}"
}

// JS injected on capture: scrapes all <video> sources currently in the DOM.
private const val CAPTURE_JS = """
(function() {
    var urls = new Set();
    document.querySelectorAll('video').forEach(function(v) {
        if (v.currentSrc && v.currentSrc.startsWith('http')) urls.add(v.currentSrc);
        if (v.src && v.src.startsWith('http')) urls.add(v.src);
        v.querySelectorAll('source').forEach(function(s) {
            if (s.src && s.src.startsWith('http')) urls.add(s.src);
        });
    });
    return JSON.stringify(Array.from(urls));
})()
"""

private val mainHandler = Handler(Looper.getMainLooper())

private enum class BrowsePane { Web, Saved }

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(vm: AppViewModel, onPlayVideo: () -> Unit) {
    val context = LocalContext.current
    val savedLinks by vm.savedLinks.collectAsState()
    val initialUrl by vm.browserUrl.collectAsState()

    var pane by remember { mutableStateOf(BrowsePane.Web) }
    var addressInput by remember { mutableStateOf(initialUrl) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    val capturedUrls = remember { mutableStateListOf<String>() }
    var showCapture by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
            }
            webViewClient = object : WebViewClient() {
                // Intercept every network request to catch video streams as they load.
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    val url = request.url.toString()
                    if (isVideoUrl(url)) {
                        mainHandler.post { if (!capturedUrls.contains(url)) capturedUrls.add(url) }
                    }
                    return null
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    isLoading = true
                    currentUrl = url
                    addressInput = url
                    canGoBack = view.canGoBack()
                    canGoForward = view.canGoForward()
                    vm.setBrowserUrl(url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    isLoading = false
                    canGoBack = view.canGoBack()
                    canGoForward = view.canGoForward()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView, title: String) {
                    pageTitle = title
                }
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            webView.destroy()
        }
    }

    LaunchedEffect(Unit) { webView.loadUrl(resolveUrl(initialUrl)) }

    BackHandler(enabled = pane == BrowsePane.Web && canGoBack) { webView.goBack() }

    val palette = Nadelon.palette

    // Runs JS on the page to find <video> elements, then combines with any URLs
    // already caught by request interception. Opens the capture dialog when done.
    fun capture() {
        webView.evaluateJavascript(CAPTURE_JS) { result ->
            if (!result.isNullOrBlank() && result != "null") {
                val urlRegex = Regex(""""(https?://[^"]+)"""")
                urlRegex.findAll(result).forEach { m ->
                    val u = m.groupValues[1]
                    if (!capturedUrls.contains(u)) capturedUrls.add(u)
                }
            }
            showCapture = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.dusk)
    ) {
        // Pane toggle: Browser | Saved links
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Nadelon.Space.column, vertical = Nadelon.Space.snug),
            horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.snug),
        ) {
            PaneTab(
                label = "Browser",
                selected = pane == BrowsePane.Web,
                onClick = { pane = BrowsePane.Web },
                modifier = Modifier.weight(1f),
            )
            PaneTab(
                label = "Saved links",
                selected = pane == BrowsePane.Saved,
                onClick = { pane = BrowsePane.Saved },
                modifier = Modifier.weight(1f),
            )
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(palette.oak))

        when (pane) {
            BrowsePane.Web -> {
                // Address bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Nadelon.Space.column, vertical = Nadelon.Space.snug),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.snug),
                ) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Search or enter URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(Nadelon.Radius.input),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { webView.loadUrl(resolveUrl(addressInput)) }
                        ),
                    )
                }

                // Loading bar — fixed height to avoid layout shift
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.lamplight,
                        trackColor = palette.oak,
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(4.dp))
                }

                AndroidView(
                    factory = { webView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                // Bottom navigation shelf
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(palette.oak))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Nadelon.Space.snug, vertical = Nadelon.Space.tight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { webView.goBack() }, enabled = canGoBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) palette.inkFaint else palette.muted,
                        )
                    }
                    IconButton(onClick = { webView.goForward() }, enabled = canGoForward) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) palette.inkFaint else palette.muted,
                        )
                    }
                    IconButton(onClick = { webView.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = palette.inkFaint)
                    }
                    Spacer(Modifier.weight(1f))
                    // Bookmark current page URL
                    val isBookmarked = savedLinks.any { it.url == currentUrl }
                    IconButton(
                        onClick = {
                            if (isBookmarked) vm.removeLink(currentUrl)
                            else vm.saveLink(currentUrl, pageTitle, isVideo = false)
                        }
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark page",
                            tint = if (isBookmarked) palette.lamplight else palette.inkFaint,
                        )
                    }
                    // Capture video button
                    Surface(
                        onClick = { capture() },
                        shape = RoundedCornerShape(Nadelon.Radius.input),
                        color = palette.lamplight,
                        modifier = Modifier.padding(end = Nadelon.Space.snug),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = Nadelon.Space.reading,
                                vertical = Nadelon.Space.snug,
                            ),
                        ) {
                            Icon(
                                Icons.Filled.Videocam,
                                contentDescription = null,
                                tint = palette.dusk,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(Nadelon.Space.tight))
                            Text("Capture", color = palette.dusk, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            BrowsePane.Saved -> {
                if (savedLinks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Nadelon.Space.snug),
                        ) {
                            Icon(
                                Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                tint = palette.margin,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                "Nothing saved yet.",
                                color = palette.inkFaint,
                                fontStyle = FontStyle.Italic,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Serif,
                            )
                            Text(
                                "Tap 🔖 to bookmark a page, or save a video from Capture.",
                                color = palette.margin,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Nadelon.Space.column),
                        verticalArrangement = Arrangement.spacedBy(Nadelon.Space.hair),
                    ) {
                        items(savedLinks, key = { it.url }) { link ->
                            SavedLinkRow(
                                link = link,
                                onPlay = {
                                    vm.setVideoFromUrl(link.url)
                                    onPlayVideo()
                                },
                                onOpen = {
                                    pane = BrowsePane.Web
                                    webView.loadUrl(link.url)
                                },
                                onDelete = { vm.removeLink(link.url) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCapture) {
        CaptureDialog(
            urls = capturedUrls.toList(),
            onPlay = { url ->
                vm.setVideoFromUrl(url)
                showCapture = false
                onPlayVideo()
            },
            onSave = { url ->
                val title = pageTitle.ifBlank { url.substringAfterLast('/').take(60) }
                vm.saveLink(url, title, isVideo = true)
            },
            onDismiss = { showCapture = false },
        )
    }
}

@Composable
private fun PaneTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Nadelon.palette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Nadelon.Radius.input),
        color = if (selected) palette.page else palette.dusk,
        border = BorderStroke(0.5.dp, if (selected) palette.oakStrong else palette.oak),
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = if (selected) palette.ink else palette.margin,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(
                horizontal = Nadelon.Space.reading,
                vertical = Nadelon.Space.snug + Nadelon.Space.tight,
            ),
        )
    }
}

@Composable
private fun SavedLinkRow(
    link: SavedLink,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = Nadelon.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Nadelon.Space.snug)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (link.isVideo) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = palette.lamplight,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(Nadelon.Space.tight))
            }
            Text(
                text = link.title,
                color = palette.ink,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Serif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = palette.muted, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = link.url,
            color = palette.margin,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Nadelon.Space.tight))
        Row(horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.gutter)) {
            if (link.isVideo) {
                Text(
                    "play",
                    color = palette.lamplight,
                    style = MaterialTheme.typography.labelLarge,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onPlay() },
                )
            }
            Text(
                "open",
                color = palette.inkFaint,
                style = MaterialTheme.typography.labelLarge,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onOpen() },
            )
        }
        Spacer(Modifier.height(Nadelon.Space.snug))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(palette.oak))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureDialog(
    urls: List<String>,
    onPlay: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = Nadelon.palette
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Nadelon.Radius.dialog),
        confirmButton = { TextButton(onClick = onDismiss) { Text("close") } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = palette.lamplight,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Nadelon.Space.snug))
                Text("Videos found", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            if (urls.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Nadelon.Space.snug)) {
                    Text(
                        "No video sources detected on this page.",
                        color = palette.inkFaint,
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Start the video playing in the page first, then tap Capture again.",
                        color = palette.margin,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Nadelon.Space.snug)) {
                    items(urls) { url ->
                        CapturedUrlRow(url = url, onPlay = { onPlay(url) }, onSave = { onSave(url) })
                    }
                }
            }
        }
    )
}

@Composable
private fun CapturedUrlRow(url: String, onPlay: () -> Unit, onSave: () -> Unit) {
    val palette = Nadelon.palette
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = url.substringAfterLast('/').substringBefore('?').take(80).ifBlank { url.take(80) },
            color = palette.ink,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Nadelon.Space.tight))
        Row(horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.gutter)) {
            Text(
                "play",
                color = palette.lamplight,
                style = MaterialTheme.typography.labelLarge,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onPlay() },
            )
            Text(
                "save link",
                color = palette.inkFaint,
                style = MaterialTheme.typography.labelLarge,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onSave() },
            )
        }
        Spacer(Modifier.height(Nadelon.Space.snug))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(palette.oak))
    }
}
