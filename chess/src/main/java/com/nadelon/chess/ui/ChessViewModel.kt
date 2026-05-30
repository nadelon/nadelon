package com.nadelon.chess.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadelon.chess.data.AnalysisResult
import com.nadelon.chess.data.AnalysisService
import com.nadelon.chess.data.Progress
import com.nadelon.chess.data.Settings
import com.nadelon.chess.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Idle : UiState
    data class Loading(val phase: String, val fraction: Float) : UiState
    data class Ready(val result: AnalysisResult) : UiState
    data class Error(val message: String) : UiState
}

class ChessViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val service = AnalysisService()

    val settings: StateFlow<Settings> = store.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        Settings("", "", 40, 3, useEngine = true, useAi = false)
    )

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    fun setUsername(v: String) = viewModelScope.launch { store.setUsername(v) }
    fun setApiKey(v: String) = viewModelScope.launch { store.setApiKey(v) }
    fun setMaxGames(v: Int) = viewModelScope.launch { store.setMaxGames(v) }
    fun setDepth(v: Int) = viewModelScope.launch { store.setDepth(v) }
    fun setUseEngine(v: Boolean) = viewModelScope.launch { store.setUseEngine(v) }
    fun setUseAi(v: Boolean) = viewModelScope.launch { store.setUseAi(v) }

    fun analyze() {
        val current = settings.value
        uiState = UiState.Loading("Starting…", 0f)
        viewModelScope.launch {
            try {
                val result = service.run(current) { p: Progress ->
                    uiState = UiState.Loading(p.phase, p.fraction)
                }
                uiState = UiState.Ready(result)
            } catch (t: Throwable) {
                uiState = UiState.Error(t.message ?: "Analysis failed.")
            }
        }
    }

    fun reset() { uiState = UiState.Idle }
}
