package com.trenits.fivestar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Indigo = Color(0xFF4F46E5)
val IndigoLight = Color(0xFFA5B4FC)
val Amber = Color(0xFFF59E0B)
val AmberLight = Color(0xFFFBBF24)
val Emerald = Color(0xFF10B981)
val Rose = Color(0xFFE11D48)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Emerald,
    background = Color(0xFFF6F5FB),
    onBackground = Color(0xFF1B1B24),
    surface = Color.White,
    onSurface = Color(0xFF1B1B24),
    surfaceVariant = Color(0xFFEDEBF7),
    onSurfaceVariant = Color(0xFF4B4A5A),
    outline = Color(0xFFBDBBCF),
    error = Rose,
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = AmberLight,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Emerald,
    background = Color(0xFF0F0E17),
    onBackground = Color(0xFFEDEDF5),
    surface = Color(0xFF1A1928),
    onSurface = Color(0xFFEDEDF5),
    surfaceVariant = Color(0xFF26243A),
    onSurfaceVariant = Color(0xFFC4C2D6),
    outline = Color(0xFF5E5B78),
    error = Color(0xFFFB7185),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

@Composable
fun FiveStarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
