package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CinematicColorScheme = darkColorScheme(
    primary = PrimaryCoral,
    secondary = PrimaryGold,
    tertiary = LightGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = SurfaceVariant,
    onPrimary = OnPrimary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSecondary = DarkBackground,
    outline = BorderColor
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CinematicColorScheme,
        typography = Typography,
        content = content
    )
}
