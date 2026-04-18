package com.nadelon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayCircle
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
import com.nadelon.app.ui.VocabularyScreen
import com.nadelon.app.ui.theme.NadelonTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NadelonTheme { NadelonApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NadelonApp() {
    var tab by remember { mutableStateOf(Tab.Player) }
    val vm: AppViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nadelon") })
        },
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
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Player -> PlayerScreen(vm = vm)
                Tab.Vocab -> VocabularyScreen(vm = vm)
            }
        }
    }
}

private enum class Tab { Player, Vocab }
