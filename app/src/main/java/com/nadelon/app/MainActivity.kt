package com.nadelon.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nadelon.app.ui.AppViewModel
import com.nadelon.app.ui.PlayerScreen
import com.nadelon.app.ui.SettingsScreen
import com.nadelon.app.ui.VocabularyScreen
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
        runCatching {
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NadelonApp(vm: AppViewModel) {
    var tab by remember { mutableStateOf(Tab.Player) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nadelon") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Player,
                    onClick = { tab = Tab.Player },
                    icon = { Icon(Icons.Filled.PlayCircle, contentDescription = null) },
                    label = { Text("Player") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Vocab,
                    onClick = { tab = Tab.Vocab },
                    icon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
                    label = { Text("Vocabulary") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Settings,
                    onClick = { tab = Tab.Settings },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Player -> PlayerScreen(vm = vm)
                Tab.Vocab -> VocabularyScreen(vm = vm)
                Tab.Settings -> SettingsScreen(vm = vm)
            }
        }
    }
}

private enum class Tab { Player, Vocab, Settings }
