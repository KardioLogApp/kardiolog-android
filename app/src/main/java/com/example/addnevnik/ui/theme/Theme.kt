package com.example.addnevnik.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = MdThemeLightPrimary,
    onPrimary = MdThemeLightOnPrimary,
    primaryContainer = MdThemeLightPrimaryContainer,
    onPrimaryContainer = MdThemeLightOnPrimaryContainer,
    secondary = MdThemeLightSecondary,
    background = MdThemeLightBackground,
    surface = MdThemeLightSurface
)

private val DarkColors = darkColorScheme(
    primary = MdThemeDarkPrimary,
    onPrimary = MdThemeDarkOnPrimary,
    primaryContainer = MdThemeDarkPrimaryContainer,
    onPrimaryContainer = MdThemeDarkOnPrimaryContainer,
    secondary = MdThemeDarkSecondary,
    background = MdThemeDarkBackground,
    surface = MdThemeDarkSurface
)

@Composable
fun ADDnevnikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
