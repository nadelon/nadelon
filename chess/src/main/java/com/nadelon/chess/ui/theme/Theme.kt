package com.nadelon.chess.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7E4B9),
    onPrimaryContainer = Color(0xFF052100),
    secondary = Color(0xFF52634F),
    tertiary = Color(0xFF38656A),
    background = Color(0xFFFCFDF7),
    surface = Color(0xFFFCFDF7),
    surfaceVariant = Color(0xFFDEE5D9),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD49E),
    onPrimary = Color(0xFF003908),
    primaryContainer = Color(0xFF155214),
    onPrimaryContainer = Color(0xFFB7E4B9),
    secondary = Color(0xFFB9CCB4),
    tertiary = Color(0xFFA0CFD3),
    background = Color(0xFF1A1C18),
    surface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFF424940),
    error = Color(0xFFFFB4AB)
)

@Composable
fun ChessCoachTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
