package com.zhenxiang.superimage.ui.theme

import android.app.Activity
import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = black,
    onPrimary = white,
    primaryContainer = white,
    onPrimaryContainer = black,
    secondary = black,
    onSecondary = white,
    secondaryContainer = white,
    onSecondaryContainer = black,
    tertiary = black,
    onTertiary = white,
    tertiaryContainer = white,
    onTertiaryContainer = black,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    outline = black,
    background = white,
    onBackground = black,
    surface = white,
    onSurface = black,
    surfaceVariant = white,
    onSurfaceVariant = black,
    inverseSurface = black,
    inverseOnSurface = white,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = white,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)


private val DarkColors = darkColorScheme(
    primary = white,
    onPrimary = black,
    primaryContainer = black,
    onPrimaryContainer = white,
    secondary = white,
    onSecondary = black,
    secondaryContainer = black,
    onSecondaryContainer = white,
    tertiary = white,
    onTertiary = black,
    tertiaryContainer = black,
    onTertiaryContainer = white,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    outline = white,
    background = black,
    onBackground = white,
    surface = black,
    onSurface = white,
    surfaceVariant = black,
    onSurfaceVariant = white,
    inverseSurface = white,
    inverseOnSurface = black,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = black,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

@Composable
fun MonoTheme(
    lightMode: Boolean = !isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (lightMode) {
        LightColors
    } else {
        DarkColors
    }

    val view = LocalView.current
    val activity = view.context as? Activity
    activity?.let {
        val window = it.window
        if (!view.isInEditMode) {
            SideEffect {
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && lightMode) Color.BLACK else Color.TRANSPARENT
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightMode
                    isAppearanceLightNavigationBars = lightMode
                }
            }
        }
    }


    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes,
        content = content,
        typography = Typography
    )
}