package com.nadelon.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5AA7FF),
    onPrimary = Color(0xFF0B0D12),
    secondary = Color(0xFF7CF0C2),
    onSecondary = Color(0xFF0B0D12),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE9ECF1),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFE9ECF1),
    surfaceVariant = Color(0xFF1F232C),
    onSurfaceVariant = Color(0xFF9AA4B2),
    error = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F66C7),
    onPrimary = Color.White,
    secondary = Color(0xFF1F8F6E),
    onSecondary = Color.White,
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF12151B),
    surface = Color.White,
    onSurface = Color(0xFF12151B),
    surfaceVariant = Color(0xFFE8ECF2),
    onSurfaceVariant = Color(0xFF55606E)
)

@Composable
fun NadelonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
