package com.nadelon.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadelon.app.data.OpenSubResult
import com.nadelon.app.data.OpenSubtitlesRepository
import com.nadelon.app.data.ResumeStore
import com.nadelon.app.data.Settings
import com.nadelon.app.data.SettingsStore
import com.nadelon.app.data.SubtitleParser
import com.nadelon.app.data.TranslationRepository
import com.nadelon.app.data.VocabularyStore
import com.nadelon.app.data.queryDisplayName
import com.nadelon.app.model.SubtitleCue
import com.nadelon.app.model.VocabEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val translator = TranslationRepository()
    private val vocabStore = VocabularyStore(app)
    private val settingsStore = SettingsStore(app)
    private val openSubs = OpenSubtitlesRepository(settingsStore)
    private val resumeStore = ResumeStore(app)

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    private val _videoDisplayName = MutableStateFlow<String?>(null)
    val videoDisplayName: StateFlow<String?> = _videoDisplayName.asStateFlow()

    private val _cues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val cues: StateFlow<List<SubtitleCue>> = _cues.asStateFlow()

    private val _sourceLang = MutableStateFlow("auto")
    val sourceLang: StateFlow<String> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow("en")
    val targetLang: StateFlow<String> = _targetLang.asStateFlow()

    private val _pauseOnTap = MutableStateFlow(true)
    val pauseOnTap: StateFlow<Boolean> = _pauseOnTap.asStateFlow()

    private val _fullscreen = MutableStateFlow(false)
    val fullscreen: StateFlow<Boolean> = _fullscreen.asStateFlow()

    private val _selectedWord = MutableStateFlow<SelectedWord?>(null)
    val selectedWord: StateFlow<SelectedWord?> = _selectedWord.asStateFlow()

    private val _openSubsState = MutableStateFlow(OpenSubsState())
    val openSubsState: StateFlow<OpenSubsState> = _openSubsState.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _autoPauseCueEnd = MutableStateFlow(false)
    val autoPauseCueEnd: StateFlow<Boolean> = _autoPauseCueEnd.asStateFlow()

    private val _subtitleOffsetMs = MutableStateFlow(0L)
    val subtitleOffsetMs: StateFlow<Long> = _subtitleOffsetMs.asStateFlow()

    val vocabulary: StateFlow<List<VocabEntry>> = vocabStore.entries
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<Settings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private var translationJob: Job? = null
    private var searchJob: Job? = null

    fun setVideo(uri: Uri) {
        _videoUri.value = uri
        _videoDisplayName.value = getApplication<Application>().queryDisplayName(uri)
    }

    fun setVideoFromUrl(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        _videoUri.value = uri
        _videoDisplayName.value = uri.lastPathSegment ?: url
    }

    fun setFullscreen(value: Boolean) { _fullscreen.value = value }

    fun loadSubtitles(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            val cues = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { SubtitleParser.parse(it, displayName) }
                        ?: emptyList()
                }.getOrElse { emptyList() }
            }
            _cues.value = cues
        }
    }

    fun setSourceLang(lang: String) { _sourceLang.value = lang }
    fun setTargetLang(lang: String) { _targetLang.value = lang }
    fun togglePauseOnTap() { _pauseOnTap.value = !_pauseOnTap.value }
    fun setPlaybackSpeed(speed: Float) { _playbackSpeed.value = speed }
    fun toggleAutoPauseCueEnd() { _autoPauseCueEnd.value = !_autoPauseCueEnd.value }
    fun adjustSubtitleOffset(deltaMs: Long) { _subtitleOffsetMs.value += deltaMs }

    fun saveResumePosition(uri: Uri, positionMs: Long) {
        if (positionMs < 5_000L) return
        viewModelScope.launch { resumeStore.save(uri.toString(), positionMs) }
    }

    suspend fun getResumePosition(uri: Uri): Long = resumeStore.get(uri.toString())

    fun translateLine(line: String) {
        if (line.isBlank()) return
        _selectedWord.value = SelectedWord(line, line, null, TranslationState.Loading)
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            val result = translator.translate(line, _sourceLang.value, _targetLang.value)
            val current = _selectedWord.value ?: return@launch
            if (current.term != line) return@launch
            _selectedWord.value = result.fold(
                onSuccess = { current.copy(translation = it, state = TranslationState.Success) },
                onFailure = { current.copy(translation = null, state = TranslationState.Failed) }
            )
        }
    }

    fun selectWord(word: String, context: String) {
        val cleaned = word.cleanedWord()
        if (cleaned.isEmpty()) return
        _selectedWord.value = SelectedWord(cleaned, context, null, TranslationState.Loading)
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            val result = translator.translate(cleaned, _sourceLang.value, _targetLang.value)
            val current = _selectedWord.value ?: return@launch
            if (current.term != cleaned) return@launch
            _selectedWord.value = result.fold(
                onSuccess = { current.copy(translation = it, state = TranslationState.Success) },
                onFailure = { current.copy(translation = null, state = TranslationState.Failed) }
            )
        }
    }

    fun dismissWord() { _selectedWord.value = null }

    fun saveSelected() {
        val current = _selectedWord.value ?: return
        val translation = current.translation ?: return
        viewModelScope.launch {
            vocabStore.add(
                VocabEntry(
                    term = current.term,
                    translation = translation,
                    sourceLang = _sourceLang.value,
                    targetLang = _targetLang.value,
                    context = current.context
                )
            )
            _selectedWord.value = null
        }
    }

    fun removeVocab(key: String) { viewModelScope.launch { vocabStore.remove(key) } }
    fun clearVocab() { viewModelScope.launch { vocabStore.clear() } }

    fun saveCredentials(apiKey: String, username: String) {
        viewModelScope.launch { settingsStore.update(apiKey, username) }
    }

    fun loginOpenSubs(password: String) {
        viewModelScope.launch {
            val s = settings.value
            if (s.apiKey.isBlank() || s.username.isBlank() || password.isBlank()) {
                _openSubsState.value = _openSubsState.value.copy(
                    message = "Add API key, username, and password first."
                )
                return@launch
            }
            _openSubsState.value = _openSubsState.value.copy(busy = true, message = null)
            val result = openSubs.login(s.apiKey, s.username, password)
            _openSubsState.value = _openSubsState.value.copy(
                busy = false,
                message = result.fold({ "Logged in." }, { it.localizedMessage ?: "Login failed." })
            )
        }
    }

    fun searchOpenSubs(query: String? = null) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val s = settings.value
            val q = query?.takeIf { it.isNotBlank() } ?: guessQuery()
            val lang = _sourceLang.value.takeIf { it != "auto" }
            _openSubsState.value = _openSubsState.value.copy(
                busy = true,
                message = null,
                results = emptyList(),
                showResults = true,
                lastQuery = q.orEmpty()
            )
            val result = openSubs.search(q, lang, s.apiKey)
            _openSubsState.value = result.fold(
                onSuccess = { list ->
                    _openSubsState.value.copy(
                        busy = false,
                        results = list,
                        message = if (list.isEmpty()) "No matches. Try a different query." else null
                    )
                },
                onFailure = { err ->
                    _openSubsState.value.copy(
                        busy = false,
                        message = err.localizedMessage ?: "Search failed."
                    )
                }
            )
        }
    }

    fun setOpenSubsQuery(q: String) {
        _openSubsState.value = _openSubsState.value.copy(lastQuery = q)
    }

    fun dismissOpenSubsResults() {
        _openSubsState.value = _openSubsState.value.copy(showResults = false, results = emptyList())
    }

    fun downloadSubtitle(fileId: Long, fileName: String) {
        viewModelScope.launch {
            val s = settings.value
            _openSubsState.value = _openSubsState.value.copy(busy = true, message = "Downloading…")
            val result = openSubs.downloadSubtitleText(fileId, s.apiKey, s.token)
            _openSubsState.value = result.fold(
                onSuccess = { text ->
                    val parsed = withContext(Dispatchers.Default) {
                        SubtitleParser.parse(text.reader().buffered(), fileName)
                    }
                    _cues.value = parsed
                    _openSubsState.value.copy(
                        busy = false,
                        showResults = false,
                        message = "Loaded ${parsed.size} cues."
                    )
                },
                onFailure = { err ->
                    _openSubsState.value.copy(
                        busy = false,
                        message = err.localizedMessage ?: "Download failed."
                    )
                }
            )
        }
    }

    private fun guessQuery(): String? {
        val name = _videoDisplayName.value ?: return null
        return name.substringBeforeLast('.', name)
            .replace(Regex("""[._]+"""), " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .takeIf { it.isNotBlank() }
    }

}

data class SelectedWord(
    val term: String,
    val context: String,
    val translation: String?,
    val state: TranslationState
)

enum class TranslationState { Loading, Success, Failed }

data class OpenSubsState(
    val busy: Boolean = false,
    val results: List<OpenSubResult> = emptyList(),
    val showResults: Boolean = false,
    val lastQuery: String = "",
    val message: String? = null
)

private val WORD_STRIP = Regex("""^[\p{P}\p{S}]+|[\p{P}\p{S}]+$""")

fun String.cleanedWord(): String = this.trim().replace(WORD_STRIP, "")
