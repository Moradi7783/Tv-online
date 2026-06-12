package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = BrightAqua,
    tertiary = WarmAccent,
    background = MidnightBlue,
    surface = DeepCharcoal,
    surfaceVariant = SlateGrey,
    onPrimary = MidnightBlue,
    onSecondary = CoolWhite,
    onBackground = CoolWhite,
    onSurface = CoolWhite,
    error = CoralRed
)

// Default same dark look since media viewing is always best in Cinema Dark Mode
private val LightColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = BrightAqua,
    tertiary = WarmAccent,
    background = MidnightBlue,
    surface = DeepCharcoal,
    surfaceVariant = SlateGrey,
    onPrimary = MidnightBlue,
    onSecondary = CoolWhite,
    onBackground = CoolWhite,
    onSurface = CoolWhite,
    error = CoralRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep Cinema colors regardless for premium streaming aesthetics
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
