package com.nadelon.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Evocative tokens: a reading room at dusk. Indigo ink on parchment under tungsten lamplight.
// Names describe the world, not the hex. If these were "gray-700" / "surface-2", the product
// would have no voice. Borders-only depth + whisper-quiet surface shifts — no shadows.

@Immutable
data class NadelonPalette(
    val dusk: Color,          // canvas — the room itself
    val page: Color,          // level-1 — a page on the desk
    val plate: Color,         // level-2 — index cards / popovers lifted off the desk
    val ink: Color,           // primary text — handwriting
    val inkFaint: Color,      // secondary text
    val margin: Color,        // tertiary — metadata, timestamps, margin notes
    val muted: Color,         // disabled / placeholder
    val oak: Color,           // default border — barely-there rule
    val oakStrong: Color,     // emphasis border / focus ring
    val marked: Color,        // highlighter wash — the tap annotation
    val lamplight: Color,     // brand — warm tungsten glow
    val redInk: Color,        // destructive — dried red ink
    val markedWash: Color,    // translucent highlighter overlay
)

// Dusk reading room (dark). Base hue is warm indigo-brown, never slate.
private val DuskPalette = NadelonPalette(
    dusk       = Color(0xFF14121A),
    page       = Color(0xFF1B1923),
    plate      = Color(0xFF221F2C),
    ink        = Color(0xFFE8DCC5),
    inkFaint   = Color(0xFFB8A98F),
    margin     = Color(0xFF8A7E6B),
    muted      = Color(0xFF5C5444),
    oak        = Color(0x14E8DCC5),   // ~8% warm line
    oakStrong  = Color(0x2EE8DCC5),   // ~18% for emphasis / focus
    marked     = Color(0xFFD9A441),
    lamplight  = Color(0xFFE4A34A),
    redInk     = Color(0xFFB84848),
    markedWash = Color(0x33D9A441),
)

// Parchment day (light). Aged, warm, not bleached.
private val ParchmentPalette = NadelonPalette(
    dusk       = Color(0xFFF4EEDD),
    page       = Color(0xFFFBF6E7),
    plate      = Color(0xFFFFFDF4),
    ink        = Color(0xFF2A221A),
    inkFaint   = Color(0xFF5C4F3D),
    margin     = Color(0xFF8A7B66),
    muted      = Color(0xFFB0A591),
    oak        = Color(0x142A221A),
    oakStrong  = Color(0x292A221A),
    marked     = Color(0xFFC28A1F),
    lamplight  = Color(0xFF8C5A1F),
    redInk     = Color(0xFF8C2B2B),
    markedWash = Color(0x33C28A1F),
)

val LocalNadelonPalette = staticCompositionLocalOf { DuskPalette }

// Reading surfaces want serif — the subtitle cue is literally a line on a page, and the
// vocabulary term is a headword in a dictionary. Controls stay sans for crispness; data
// stays mono so counts and language codes align under the eye.
private val NadelonTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, letterSpacing = (-0.25).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, letterSpacing = 0.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
        fontSize = 17.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, letterSpacing = 0.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 10.sp, letterSpacing = 0.8.sp
    ),
)

private fun duskColorScheme(p: NadelonPalette) = darkColorScheme(
    primary          = p.lamplight,
    onPrimary        = p.dusk,
    secondary        = p.marked,
    onSecondary      = p.dusk,
    tertiary         = p.margin,
    onTertiary       = p.dusk,
    background       = p.dusk,
    onBackground     = p.ink,
    surface          = p.page,
    onSurface        = p.ink,
    surfaceVariant   = p.plate,
    onSurfaceVariant = p.inkFaint,
    outline          = p.oakStrong,
    outlineVariant   = p.oak,
    error            = p.redInk,
    onError          = p.ink,
)

private fun parchmentColorScheme(p: NadelonPalette) = lightColorScheme(
    primary          = p.lamplight,
    onPrimary        = Color.White,
    secondary        = p.marked,
    onSecondary      = Color.White,
    tertiary         = p.margin,
    onTertiary       = Color.White,
    background       = p.dusk,
    onBackground     = p.ink,
    surface          = p.page,
    onSurface        = p.ink,
    surfaceVariant   = p.plate,
    onSurfaceVariant = p.inkFaint,
    outline          = p.oakStrong,
    outlineVariant   = p.oak,
    error            = p.redInk,
    onError          = Color.White,
)

object Nadelon {
    val palette: NadelonPalette
        @Composable @ReadOnlyComposable
        get() = LocalNadelonPalette.current

    // 4dp base unit. Multiples only.
    object Space {
        val hair = 2.dp
        val tight = 4.dp
        val snug = 8.dp
        val reading = 12.dp
        val column = 16.dp
        val gutter = 24.dp
    }

    // Printed-book corners — small, square-ish. Not bubble-rounded.
    object Radius {
        val input = 2.dp
        val card = 6.dp
        val dialog = 10.dp
    }
}

@Composable
fun NadelonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DuskPalette else ParchmentPalette
    CompositionLocalProvider(LocalNadelonPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) duskColorScheme(palette) else parchmentColorScheme(palette),
            typography = NadelonTypography,
            content = content
        )
    }
}
