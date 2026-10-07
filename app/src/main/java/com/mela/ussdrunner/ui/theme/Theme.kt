package com.mela.ussdrunner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mela.ussdrunner.domain.model.ThemeMode

val Teal = Color(0xFF0F6E68)
val TealLight = Color(0xFF7BD4CC)
val Gold = Color(0xFFC4892A)
val Paper = Color(0xFFF4F1EA)
val Ink = Color(0xFF10211F)
val Night = Color(0xFF0B1413)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EFEB),
    onPrimaryContainer = Color(0xFF083834),
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E2C4),
    onSecondaryContainer = Color(0xFF3D2A0A),
    tertiary = Color(0xFF3F5F7A),
    background = Paper,
    onBackground = Ink,
    surface = Color(0xFFFBF8F2),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6E3DA),
    onSurfaceVariant = Color(0xFF3E4A48),
    outline = Color(0xFF6E7A77),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF0C524D),
    onPrimaryContainer = Color(0xFFC8F3EE),
    secondary = Color(0xFFE8B86D),
    onSecondary = Color(0xFF3D2A0A),
    secondaryContainer = Color(0xFF5C3F12),
    onSecondaryContainer = Color(0xFFF8E4C6),
    tertiary = Color(0xFFA8C5DE),
    background = Night,
    onBackground = Color(0xFFE4E7E5),
    surface = Color(0xFF121C1B),
    onSurface = Color(0xFFE4E7E5),
    surfaceVariant = Color(0xFF2A3533),
    onSurfaceVariant = Color(0xFFBFC9C6),
    outline = Color(0xFF899390),
    error = Color(0xFFFFB4AB),
)

@Composable
fun MelaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
