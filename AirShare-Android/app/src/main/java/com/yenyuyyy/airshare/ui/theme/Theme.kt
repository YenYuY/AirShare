package com.yenyuyyy.airshare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = AirBlue,
    onPrimary = Color.White,
    primaryContainer = AirBlueContainer,
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF4E616F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E5F4),
    onSecondaryContainer = Color(0xFF0A1E2A),
    tertiary = Color(0xFF66587B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDDCFF),
    onTertiaryContainer = Color(0xFF211634),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Canvas,
    onBackground = Ink,
    surface = Canvas,
    onSurface = Ink,
    surfaceVariant = Color(0xFFDFE3E8),
    onSurfaceVariant = Color(0xFF43474C),
    outline = Color(0xFF74777C),
    outlineVariant = Color(0xFFC3C7CC),
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFF0F1F5),
    inversePrimary = AirBlueLight,
)

private val DarkColors = darkColorScheme(
    primary = AirBlueLight,
    onPrimary = Color(0xFF003352),
    primaryContainer = AirBlueContainerDark,
    onPrimaryContainer = Color(0xFFCDE5FF),
    secondary = Color(0xFFB5C9D8),
    onSecondary = Color(0xFF20333F),
    secondaryContainer = Color(0xFF374A57),
    onSecondaryContainer = Color(0xFFD1E5F4),
    tertiary = Color(0xFFD0BFE8),
    onTertiary = Color(0xFF362B4B),
    tertiaryContainer = Color(0xFF4D4162),
    onTertiaryContainer = Color(0xFFEDDCFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = CanvasDark,
    onBackground = InkDark,
    surface = CanvasDark,
    onSurface = InkDark,
    surfaceVariant = Color(0xFF43474C),
    onSurfaceVariant = Color(0xFFC3C7CC),
    outline = Color(0xFF8D9197),
    outlineVariant = Color(0xFF43474C),
    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2E3135),
    inversePrimary = AirBlue,
)

private val AirShareShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun AirShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AirShareTypography,
        shapes = AirShareShapes,
        content = content,
    )
}
