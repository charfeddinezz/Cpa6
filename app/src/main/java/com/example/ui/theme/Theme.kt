package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CpaPrimary,
    onPrimary = Color(0xFF001A22),
    primaryContainer = CpaPrimaryDim,
    onPrimaryContainer = CpaPrimary,
    secondary = CpaAccent,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = CpaAccentDim,
    onSecondaryContainer = CpaAccent,
    background = CpaBg,
    onBackground = CpaText,
    surface = CpaSurface,
    onSurface = CpaText,
    surfaceVariant = CpaCardElevated,
    onSurfaceVariant = CpaTextMuted,
    outline = CpaBorder,
    error = CpaError,
    onError = Color(0xFF450A0A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
