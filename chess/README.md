# Chess Coach

An Android app that scans your **chess.com** games and turns them into a
personalised improvement plan: your best and worst openings, the recurring
strategic and positional mistakes you keep making, and *theoretical lessons*
(not just puzzles) built from your own games.

It is a self-contained Gradle module (`:chess`) living alongside the Nadelon
media player in this repo. It shares no code with `:app`.

## What it does

1. **Downloads your games** from the public chess.com API (no login, no API key
   — just your username).
2. **Analyses every move** with a built-in chess engine:
   - a perft-validated legal move generator,
   - a hand-crafted evaluation (material, piece-square tables, bishop pair,
     pawn structure),
   - an alpha-beta + quiescence search that scores each of your moves and flags
     inaccuracies, mistakes and blunders (win-probability based, like a
     "Game Review").
3. **Detects positional & strategic patterns** beyond hanging pieces, e.g.:
   - neglecting the centre,
   - slow development / premature queen sorties,
   - leaving the king in the centre,
   - **weak colour complexes** (dark/light-square domination around your king),
   - passive rooks / ignoring open files,
   - conceding space without central counterplay,
   - lasting pawn weaknesses,
   - failing to convert winning positions,
   - late-game / endgame collapses.
4. **Aggregates** across all games: opening performance (score, accuracy, by
   colour), accuracy by phase (opening / middlegame / endgame), and how often
   each weakness shows up.
5. **Generates lessons** — ranked theoretical lessons that explain the
   underlying principle and attach example positions *from your own games*.
6. **Optional AI coach** — if you add a Claude API key in Settings, the app
   sends the aggregated analysis to the Claude API for richer, tailored
   lessons. Without a key it uses the built-in lesson library, so it is fully
   functional offline-of-AI.

## Screens

- **Coach** — dashboard: record, win rate, accuracy, accuracy by phase, top
  strengths and weaknesses.
- **Openings** — every opening family you play, scored and sorted.
- **Mistakes** — recurring patterns, most frequent first, each with an example
  board from your games.
- **Lessons** — your ranked study plan; tap a lesson for theory, principles,
  your example positions, and practice drills.
- **Games** — browse each analysed game and step through it move by move with
  engine annotations.

## Project layout

```
chess/src/main/java/com/nadelon/chess/
  core/        Board, Move, FEN, SAN, PGN parser (perft-validated)
  engine/      Evaluation, alpha-beta Search, MoveQuality classification
  analysis/    Positional Features, strategic Theme detectors, OpeningBook,
               per-game GameAnalyzer, cross-game Aggregate
  lessons/     Lesson model, LessonLibrary (theory), LessonEngine, AiCoach
  data/        ChessComApi, SettingsStore, AnalysisService (the pipeline)
  ui/          Compose screens, BoardView, ChessViewModel, theme
```

## Build & test

Open the project in Android Studio and run the `:chess` configuration on
API 24+, or:

```
./gradlew :chess:assembleDebug
./gradlew :chess:testDebugUnitTest   # perft, SAN round-trip, and pipeline tests
```

## Notes

- The chess.com API requires a descriptive `User-Agent` (the app sets one) and
  is rate-limited; very large game counts are fetched month by month.
- The engine is intentionally lightweight so it can analyse many games on-device
  without a native binary. Choose a higher "Engine strength" (search depth) or
  fewer games to trade speed for precision.
- The strategic detectors are heuristics: they surface *tendencies*, and the
  lessons teach the principle behind each one.
