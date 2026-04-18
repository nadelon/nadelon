# Nadelon — Interface System

A reading room at dusk. Warm tungsten lamplight, foxed parchment, indigo ink, a notebook
in the margin. The user is a language learner watching a foreign-language film at home at
night; the verb is *annotate*; the feel is quiet study, not "clean and modern."

## Direction

- **World:** page, marginalia, highlighter wash, foxed parchment, ink stain, reading lamp,
  gutter, pull-quote, ruled line.
- **Primary verb:** annotate — scan the line, tap the unknown word, return to the film.
- **Depth strategy:** borders-only + whisper-quiet surface-lightness shifts. **No shadows.**
  Reading rooms are flat; shadows read as plastic.
- **Base unit:** 4dp. All spacing is a multiple (hair 2, tight 4, snug 8, reading 12,
  column 16, gutter 24).
- **Radius scale:** input 2, card 6, dialog 10. Small, printed-book square-ish corners —
  never bubble-rounded.

## Palette (evocative tokens, not generic slots)

### Dusk (dark — default)

| Token        | Hex / alpha    | Role                                                   |
|--------------|----------------|--------------------------------------------------------|
| `dusk`       | `#14121A`      | canvas — the room itself                              |
| `page`       | `#1B1923`      | level-1 surface — a page on the desk                  |
| `plate`      | `#221F2C`      | level-2 surface — index cards, popovers               |
| `ink`        | `#E8DCC5`      | primary text — handwriting in aged-paper off-white    |
| `inkFaint`   | `#B8A98F`      | secondary text                                        |
| `margin`     | `#8A7E6B`      | tertiary — metadata, margin notes, timestamps         |
| `muted`      | `#5C5444`      | disabled / placeholder                                |
| `oak`        | `rgba(ink, 8%)`| default border — barely-there rule                    |
| `oakStrong`  | `rgba(ink,18%)`| emphasis border / focus                               |
| `marked`     | `#D9A441`      | highlighter wash — tap / selected                     |
| `lamplight`  | `#E4A34A`      | brand / primary — warm tungsten                       |
| `redInk`     | `#B84848`      | destructive — dried red ink, not UI red               |
| `markedWash` | `rgba(marked,20%)` | translucent overlay for annotation               |

Surface jumps are ~3–4% lightness — whisper-quiet. Hue stays warm indigo-brown at every
elevation; never shift to a cold slate for "surface-2."

### Parchment (light)

| Token        | Hex             | Role                                |
|--------------|-----------------|-------------------------------------|
| `dusk`       | `#F4EEDD`       | canvas — aged off-white             |
| `page`       | `#FBF6E7`       | level-1                             |
| `plate`      | `#FFFDF4`       | level-2                             |
| `ink`        | `#2A221A`       | handwriting brown-black             |
| `inkFaint`   | `#5C4F3D`       |                                     |
| `margin`     | `#8A7B66`       |                                     |
| `oak`        | `rgba(ink, 8%)` |                                     |
| `oakStrong`  | `rgba(ink,16%)` |                                     |
| `lamplight`  | `#8C5A1F`       | richer leather/amber in light mode  |
| `redInk`     | `#8C2B2B`       |                                     |

## Typography

- **Serif** (`FontFamily.Serif` → Noto Serif) for: display, headlines, titles, subtitle
  cues, vocabulary headwords, translation glosses, and any reading-weight body. The
  subtitle is literally a line on a page; the vocab term is a dictionary headword.
- **Sans** (`FontFamily.SansSerif`) for: labels, shelf buttons, nav, form controls.
  Operations stay crisp.
- **Monospace** for: language codes, download counts, cue counts, anything columnar.
  Data signals "fact."

| Role           | Family  | Size | Weight     | Notes                           |
|----------------|---------|------|------------|---------------------------------|
| displayLarge   | Serif   | 28sp | SemiBold   | "Notebook", "Study"             |
| headlineMedium | Serif   | 22sp | SemiBold   | section / card titles           |
| titleLarge     | Serif   | 20sp | SemiBold   | dialog titles                   |
| titleMedium    | Sans    | 15sp | Medium     |                                 |
| bodyLarge      | Serif   | 17sp | Normal     | subtitle cues, dict glosses     |
| bodyMedium     | Sans    | 14sp | Normal     | prose, hints                    |
| labelLarge     | Sans    | 13sp | Medium     | shelf buttons, text links       |
| labelMedium    | Mono    | 11sp | Normal     | codes, counts, margin notes     |

## Components

### Subtitle overlay (the signature)

Page-bottom wash in `plate` at 0.86 alpha — a warm parchment over the film, not a black
caption bar. Half-dp `oakStrong` border. Serif cue text in `ink`. Tapped words ripple in
`lamplight`; each word is a `FlowRow` cell with `minimumInteractiveComponentSize` for
TalkBack. No button outlines. The tap is a highlighter stroke.

### Index card (translation result)

`plate` surface, 0.5dp `oakStrong` border, `Radius.card`. Headword: serif SemiBold,
`headlineMedium`, `ink`. Translation: serif italic `bodyLarge`, `inkFaint`. Actions are
underlined text-links at the foot — "close" in `inkFaint`, "save to notebook" in
`lamplight`. Never a Material Button bar.

### Shelf button

Small `page`-surface tile, 0.5dp `oakStrong` border, `Radius.input`. Sans label in
`labelLarge`. Used for film/subtitle pickers. Only present when the film is the focal
point; recedes once a video loads.

### Language pill

Two-level pill: `dusk` surface with the 2-letter code in monospace `labelMedium`
followed by the full display name in sans. Rendered as part of an "en → es" row with a
`margin`-colored `→` between. Dropdown opens below.

### Notebook entry

No card. The notebook is a column of entries separated by half-dp `oak` ruled lines.
Headword serif SemiBold, source→target code in monospace next to it. Gloss serif italic.
Original subtitle line as a pull-quote: 2dp `oakStrong` vertical rule + serif italic
`margin` text.

### Nav (bottom)

Three verbs, not three nouns: **watch / notebook / study**. Serif labels in lowercase.
`lamplight` tint for active; `margin` italic for inactive. No pill, no indicator bar —
the color shift is the entire state.

### Wordmark (top)

"Nadelon" in serif `headlineMedium`, followed inline by italic subtitle
"— a reading room for foreign films" in `labelMedium` `margin`. Single `oak` rule below.

## Rejected defaults

| Default                                        | Replacement                                                                 |
|------------------------------------------------|-----------------------------------------------------------------------------|
| Cold blue/teal dark-mode palette               | Warm indigo-brown `dusk` with ink-on-parchment foreground                   |
| Pure-black translucent subtitle bar            | Parchment-wash `plate` page-bottom with serif cue type                     |
| Generic `Player / Vocab / Settings` nav        | `Watch / Notebook / Study` — the verbs of a reading night                  |
| Material `Button` primary actions              | Underlined text-links in `lamplight`, close in `inkFaint`                  |
| `AssistChip` for pause-on-tap                  | Margin text — `pause on tap · on/off`, `lamplight` when on                 |
| Round `OutlinedButton` "Find subtitles"        | Small search icon in the language row; catalog dialog titled "Catalogue"   |

## Patterns worth remembering

- When reading-weight text sits next to operational text, the reading text is serif and
  the operational text is sans. Never mix.
- Data (codes, counts, timestamps) is always monospace `labelMedium` or `labelSmall`.
- Destructive actions use `redInk` underline text, never a red button.
- A "confirmation" of state (signed in, cue count, X headwords) belongs in the margin,
  not in a banner.
- When nothing is loaded, write a serif italic sentence in `inkFaint` — "An empty frame.",
  "The notebook is blank." — not a Material empty-state illustration.
