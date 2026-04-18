# Nadelon

Android media player for language learners. Tap any word in the on-screen subtitles to see a translation, then save it to your vocabulary list.

## Features

- Play any local video (ExoPlayer / Media3).
- Load `.srt` or `.vtt` subtitle files. Cues are rendered as a tappable overlay on top of the video.
- Tap a word → inline translation card via the free [MyMemory](https://mymemory.translated.net/doc/spec.php) API.
- Save words (with original line as context) to a persistent vocabulary list (DataStore).
- Search / remove / clear saved vocabulary.
- "Pause on tap" toggle to freeze playback while you study a line.
- Selectable source and target languages.

## Project layout

```
app/src/main/java/com/nadelon/app/
  MainActivity.kt                  # Compose entry + bottom-nav (Player / Vocabulary)
  model/Models.kt                  # SubtitleCue, VocabEntry
  data/SubtitleParser.kt           # SRT + WebVTT parsing, binary-search cue lookup
  data/TranslationRepository.kt    # HTTPS call to MyMemory, in-memory LRU cache
  data/VocabularyStore.kt          # DataStore-backed JSON persistence
  ui/AppViewModel.kt               # State holder for player, subtitles, translations, vocab
  ui/PlayerScreen.kt               # Player UI + language pickers + file pickers
  ui/SubtitleOverlay.kt            # Per-word tappable subtitle overlay (ClickableText)
  ui/VocabularyScreen.kt           # Search / remove / clear saved words
  ui/theme/Theme.kt                # Material3 color scheme
```

## Build

Open in Android Studio (Hedgehog or newer) and run on API 24+. A Gradle wrapper JAR is not committed — regenerate it with `gradle wrapper --gradle-version 8.7` once, or let Android Studio sync.

## Notes

- MyMemory is free and keyless but rate-limited. Translations are cached in memory for the session.
- Subtitle parsing is intentionally forgiving about HTML tags and timing punctuation.
