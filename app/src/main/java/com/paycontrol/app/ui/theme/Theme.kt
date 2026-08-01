package com.paycontrol.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Identidad visual DimagPay — «Sapphire Atelier»:
 * tinta fría, papel perla, acento latón suave. Sin púrpura ni cream genérico.
 */
private val Ink = Color(0xFF121A24)
private val Pearl = Color(0xFFEEF1F5)
private val Paper = Color(0xFFFBFCFD)
private val Sapphire = Color(0xFF1F4E5F)
private val SapphireDeep = Color(0xFF143744)
private val Brass = Color(0xFFA8874F)
private val SoftLine = Color(0xFFD8E0E6)
private val MistText = Color(0xFF5A6874)

private val LightColors = lightColorScheme(
    primary = Sapphire,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E6EC),
    onPrimaryContainer = SapphireDeep,
    secondary = Brass,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E6D4),
    onSecondaryContainer = Color(0xFF3F3218),
    tertiary = SapphireDeep,
    tertiaryContainer = Color(0xFFC9DCE3),
    onTertiaryContainer = SapphireDeep,
    background = Pearl,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = SoftLine,
    onSurfaceVariant = MistText,
    outline = Color(0xFFAEBBC4),
    outlineVariant = Color(0xFFD0D9E0),
    error = Color(0xFFB42318),
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Color(0xFFF5F7F9),
    surfaceContainer = Color(0xFFEEF2F5),
    surfaceContainerHigh = SoftLine
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8EBCC9),
    onPrimary = SapphireDeep,
    primaryContainer = Color(0xFF1A4554),
    onPrimaryContainer = Color(0xFFD4E6EC),
    secondary = Color(0xFFD0B67E),
    onSecondary = Color(0xFF2A2110),
    secondaryContainer = Color(0xFF4A3C22),
    onSecondaryContainer = Color(0xFFF0E6D4),
    background = Color(0xFF0C1218),
    onBackground = Color(0xFFE8EEF2),
    surface = Color(0xFF121A22),
    onSurface = Color(0xFFE8EEF2),
    surfaceVariant = Color(0xFF1E2A34),
    onSurfaceVariant = Color(0xFFAEBBC4),
    outline = Color(0xFF5A6874),
    error = Color(0xFFFFB4AB)
)

@Composable
fun PayControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DimagPayTypography,
        content = content
    )
}

/** Alias de marca para nuevas pantallas. */
@Composable
fun DimagPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = PayControlTheme(darkTheme = darkTheme, content = content)
