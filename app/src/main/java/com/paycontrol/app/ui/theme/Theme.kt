package com.paycontrol.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta editorial: tinta profunda + teal sobrio + acento oro suave
private val Ink = Color(0xFF0F1C1A)
private val Mist = Color(0xFFF4F7F6)
private val Paper = Color(0xFFFAFBFB)
private val Teal = Color(0xFF1A6B63)
private val TealDeep = Color(0xFF0B3D3A)
private val Gold = Color(0xFFC4A574)
private val SoftLine = Color(0xFFE4EBE9)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EFEA),
    onPrimaryContainer = TealDeep,
    secondary = Gold,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFF3E8D8),
    onSecondaryContainer = Color(0xFF4A3A22),
    tertiary = TealDeep,
    background = Mist,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = SoftLine,
    onSurfaceVariant = Color(0xFF4A5A56),
    outline = Color(0xFFB7C5C1),
    error = Color(0xFFB42318)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BC4BB),
    onPrimary = TealDeep,
    primaryContainer = Color(0xFF134E48),
    onPrimaryContainer = Color(0xFFD8EFEA),
    secondary = Color(0xFFD4B98A),
    onSecondary = Color(0xFF2B2112),
    background = Color(0xFF0C1413),
    onBackground = Color(0xFFE8EEEC),
    surface = Color(0xFF121C1A),
    onSurface = Color(0xFFE8EEEC),
    surfaceVariant = Color(0xFF1E2B28),
    onSurfaceVariant = Color(0xFFB7C5C1),
    outline = Color(0xFF5A6B67),
    error = Color(0xFFFFB4AB)
)

private val DisplayFamily = FontFamily.Serif
private val BodyFamily = FontFamily.SansSerif

val PayControlTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    )
)

@Composable
fun PayControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PayControlTypography,
        content = content
    )
}
