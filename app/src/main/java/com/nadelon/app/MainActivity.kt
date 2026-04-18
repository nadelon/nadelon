package com.nadelon.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.nadelon.app.R
import com.nadelon.app.ui.AppViewModel
import com.nadelon.app.ui.PlayerScreen
import com.nadelon.app.ui.SettingsScreen
import com.nadelon.app.ui.VocabularyScreen
import com.nadelon.app.ui.theme.Nadelon
import com.nadelon.app.ui.theme.NadelonTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncoming(intent)
        setContent {
            NadelonTheme { NadelonApp(vm = vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> extraStream(intent)
            else -> null
        }
        uri ?: return
        if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        vm.setVideo(uri)
    }

    @Suppress("DEPRECATION")
    private fun extraStream(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
}

// The frame is quiet. Serif wordmark with a subtitle that names the night's three verbs.
// A single oak rule separates chrome from content. No elevation, no shadow.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NadelonApp(vm: AppViewModel) {
    var tab by remember { mutableStateOf(Tab.Watch) }
    val palette = Nadelon.palette

    Scaffold(
        containerColor = palette.dusk,
        contentColor = palette.ink,
        topBar = { Wordmark() },
        bottomBar = { ReadingRoomNav(selected = tab, onSelect = { tab = it }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(palette.dusk)
        ) {
            when (tab) {
                Tab.Watch -> PlayerScreen(vm = vm)
                Tab.Notebook -> VocabularyScreen(vm = vm)
                Tab.Study -> SettingsScreen(vm = vm)
            }
        }
    }
}

@Composable
private fun Wordmark() {
    val palette = Nadelon.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.dusk)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                horizontal = Nadelon.Space.column,
                vertical = Nadelon.Space.reading,
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = Color.Unspecified,
            )
            Spacer(Modifier.width(Nadelon.Space.snug))
            Text(
                text = "Nadelon",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
            )
        }
        Spacer(Modifier.height(Nadelon.Space.snug))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(palette.oak)
        )
    }
}

@Composable
private fun ReadingRoomNav(selected: Tab, onSelect: (Tab) -> Unit) {
    val palette = Nadelon.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.dusk)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(palette.oak)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Nadelon.Space.snug,
                    vertical = Nadelon.Space.snug,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavVerb(
                label = "Watch",
                icon = Icons.Filled.Movie,
                selected = selected == Tab.Watch,
                onClick = { onSelect(Tab.Watch) },
                modifier = Modifier.weight(1f),
            )
            NavVerb(
                label = "Notebook",
                icon = Icons.Filled.AutoStories,
                selected = selected == Tab.Notebook,
                onClick = { onSelect(Tab.Notebook) },
                modifier = Modifier.weight(1f),
            )
            NavVerb(
                label = "Study",
                icon = Icons.Filled.Lightbulb,
                selected = selected == Tab.Study,
                onClick = { onSelect(Tab.Study) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavVerb(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Nadelon.palette
    val tint = if (selected) palette.lamplight else palette.margin
    Column(
        modifier = modifier
            .clickable(
                role = Role.Tab,
                onClickLabel = "Switch to $label",
                onClick = onClick,
            )
            .semantics { contentDescription = "$label tab" }
            .padding(vertical = Nadelon.Space.snug),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .height(22.dp)
                .width(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.lowercase(),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontFamily = FontFamily.Serif,
            fontStyle = if (selected) FontStyle.Normal else FontStyle.Italic,
        )
    }
}

private enum class Tab { Watch, Notebook, Study }
