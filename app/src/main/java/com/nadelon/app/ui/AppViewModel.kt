package com.nadelon.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadelon.app.data.SubtitleParser
import com.nadelon.app.data.TranslationRepository
import com.nadelon.app.data.VocabularyStore
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

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    private val _cues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val cues: StateFlow<List<SubtitleCue>> = _cues.asStateFlow()

    private val _sourceLang = MutableStateFlow("auto")
    val sourceLang: StateFlow<String> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow("en")
    val targetLang: StateFlow<String> = _targetLang.asStateFlow()

    private val _pauseOnTap = MutableStateFlow(true)
    val pauseOnTap: StateFlow<Boolean> = _pauseOnTap.asStateFlow()

    private val _selectedWord = MutableStateFlow<SelectedWord?>(null)
    val selectedWord: StateFlow<SelectedWord?> = _selectedWord.asStateFlow()

    val vocabulary: StateFlow<List<VocabEntry>> = vocabStore.entries
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var translationJob: Job? = null

    fun setVideo(uri: Uri) { _videoUri.value = uri }

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

    fun selectWord(word: String, context: String) {
        val cleaned = word.cleanedWord()
        if (cleaned.isEmpty()) return
        _selectedWord.value = SelectedWord(
            term = cleaned,
            context = context,
            translation = null,
            state = TranslationState.Loading
        )
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

    fun removeVocab(key: String) {
        viewModelScope.launch { vocabStore.remove(key) }
    }

    fun clearVocab() {
        viewModelScope.launch { vocabStore.clear() }
    }
}

data class SelectedWord(
    val term: String,
    val context: String,
    val translation: String?,
    val state: TranslationState
)

enum class TranslationState { Loading, Success, Failed }

private val WORD_STRIP = Regex("""^[\p{P}\p{S}]+|[\p{P}\p{S}]+$""")

fun String.cleanedWord(): String = this.trim().replace(WORD_STRIP, "")
