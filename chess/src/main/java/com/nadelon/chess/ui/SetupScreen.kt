package com.nadelon.chess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nadelon.chess.data.Settings

@Composable
fun SetupScreen(
    settings: Settings,
    state: UiState,
    onUsername: (String) -> Unit,
    onMaxGames: (Int) -> Unit,
    onDepth: (Int) -> Unit,
    onUseEngine: (Boolean) -> Unit,
    onUseAi: (Boolean) -> Unit,
    onApiKey: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Chess Coach", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Scan your chess.com games and get a personalised improvement plan — your best and " +
                "worst openings, the recurring strategic mistakes you make, and theoretical lessons " +
                "built from your own games.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = settings.username,
            onValueChange = onUsername,
            label = { Text("chess.com username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("How many recent games?")
                val options = listOf(20, 40, 80, 150)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { n ->
                        FilterPill(text = "$n", selected = settings.maxGames == n) { onMaxGames(n) }
                    }
                }
                Text(
                    "More games give better statistics but take longer to analyse.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingSwitch(
                    "Engine analysis",
                    "Score every move for accuracy and detect tactical blunders. Slower but far more insightful.",
                    settings.useEngine, onUseEngine
                )
                if (settings.useEngine) {
                    Text("Engine strength", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2 to "Fast", 3 to "Balanced", 4 to "Deep").forEach { (d, label) ->
                            FilterPill(text = label, selected = settings.depth == d) { onDepth(d) }
                        }
                    }
                }
                Divider()
                SettingSwitch(
                    "AI coach (optional)",
                    "Use your Claude API key to generate richer, tailored lessons. Falls back to the " +
                        "built-in lesson library if left off.",
                    settings.useAi, onUseAi
                )
                if (settings.useAi) {
                    OutlinedTextField(
                        value = settings.apiKey,
                        onValueChange = onApiKey,
                        label = { Text("Claude API key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            }
        }

        when (state) {
            is UiState.Loading -> {
                Spacer(Modifier.height(4.dp))
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.height(0.dp))
                            Text("  ${state.phase}", style = MaterialTheme.typography.bodyMedium)
                        }
                        LinearProgressIndicator(
                            progress = { state.fraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            is UiState.Error -> {
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        state.message,
                        Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            else -> {}
        }

        Button(
            onClick = onAnalyze,
            enabled = settings.username.isNotBlank() && state !is UiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state is UiState.Loading) "Analysing…" else "Analyse my games")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingSwitch(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}
