package com.campusgomobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CampusGoBlue,
    onPrimary = Color.White,
    primaryContainer = Emerald600,
    onPrimaryContainer = Color.White,
    secondary = Violet600,
    onSecondary = Color.White,
    tertiary = Amber500,
    onTertiary = Zinc900,
    background = Zinc100,
    onBackground = Zinc900,
    surface = Color.White,
    onSurface = Zinc900,
    surfaceVariant = Zinc200,
    onSurfaceVariant = Zinc600,
    outline = Zinc200,
    error = Red600,
    onError = Color.White,
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Red700
)

private val DarkColorScheme = darkColorScheme(
    primary = CampusGoBlue,
    onPrimary = Color.White,
    primaryContainer = Emerald700,
    onPrimaryContainer = Color.White,
    secondary = Violet600,
    onSecondary = Color.White,
    tertiary = Amber600,
    onTertiary = Zinc900,
    background = Zinc900,
    onBackground = Zinc100,
    surface = Zinc800,
    onSurface = Zinc100,
    surfaceVariant = Zinc700,
    onSurfaceVariant = Zinc400,
    outline = Zinc700,
    error = Red700,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5)
)

@Composable
fun CampusGoMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
