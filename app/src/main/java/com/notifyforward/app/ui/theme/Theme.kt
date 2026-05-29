package com.notifyforward.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.notifyforward.app.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary              = Primary,
    onPrimary            = OnPrimaryLight,
    primaryContainer     = Color(0xFFD3E2FF),
    onPrimaryContainer   = Color(0xFF001849),
    secondary            = Secondary,
    onSecondary          = Color(0xFF00201E),
    tertiary             = Tertiary,
    background           = BackgroundLight,
    onBackground         = OnBackgroundLight,
    surface              = SurfaceLight,
    onSurface            = OnSurfaceLight,
    surfaceVariant       = Color(0xFFE7E0EC),
    onSurfaceVariant     = Color(0xFF49454F),
    error                = ErrorLight,
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD6),
    outline              = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary              = PrimaryLight,
    onPrimary            = OnPrimaryDark,
    primaryContainer     = Color(0xFF004880),
    onPrimaryContainer   = Color(0xFFD3E2FF),
    secondary            = Color(0xFF4DD0C4),
    onSecondary          = Color(0xFF00363B),
    tertiary             = Color(0xFFCBB2FF),
    background           = BackgroundDark,
    onBackground         = OnBackgroundDark,
    surface              = SurfaceDark,
    onSurface            = OnSurfaceDark,
    surfaceVariant       = Color(0xFF49454F),
    onSurfaceVariant     = Color(0xFFCAC4D0),
    error                = ErrorDark,
    onError              = Color(0xFF601410),
    errorContainer       = Color(0xFF8C1D18),
    outline              = OutlineDark
)

@Composable
fun NotifyForwardTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark    = isSystemInDarkTheme()
    val useDarkTheme  = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
