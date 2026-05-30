package com.nadelon.chess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.nadelon.chess.ui.ChessViewModel
import com.nadelon.chess.ui.GamesScreen
import com.nadelon.chess.ui.LessonsScreen
import com.nadelon.chess.ui.MistakesScreen
import com.nadelon.chess.ui.OpeningsScreen
import com.nadelon.chess.ui.OverviewScreen
import com.nadelon.chess.ui.SetupScreen
import com.nadelon.chess.ui.UiState
import com.nadelon.chess.ui.theme.ChessCoachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessCoachTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

private class Tab(val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: ChessViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    val state = vm.uiState

    if (state !is UiState.Ready) {
        SetupScreen(
            settings = settings,
            state = state,
            onUsername = vm::setUsername,
            onMaxGames = vm::setMaxGames,
            onDepth = vm::setDepth,
            onUseEngine = vm::setUseEngine,
            onUseAi = vm::setUseAi,
            onApiKey = vm::setApiKey,
            onAnalyze = vm::analyze
        )
        return
    }

    val result = state.result
    val tabs = remember {
        listOf(
            Tab("Coach", Icons.Filled.Dashboard),
            Tab("Openings", Icons.Outlined.Star),
            Tab("Mistakes", Icons.Filled.Warning),
            Tab("Lessons", Icons.AutoMirrored.Filled.MenuBook),
            Tab("Games", Icons.Filled.SportsEsports)
        )
    }
    var selected by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chess Coach") },
                actions = {
                    IconButton(onClick = { vm.reset() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Re-analyse")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> OverviewScreen(result.aggregate)
                1 -> OpeningsScreen(result.aggregate)
                2 -> MistakesScreen(result.aggregate)
                3 -> LessonsScreen(result.lessons)
                4 -> GamesScreen(result.reports)
            }
        }
    }
}
