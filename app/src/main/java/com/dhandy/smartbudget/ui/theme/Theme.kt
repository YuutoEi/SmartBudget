package com.dhandy.smartbudget.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LightIndigo,
    secondary = LightTeal,
    tertiary = LightPink,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Slate700,
    onPrimary = Slate900,
    onSecondary = Slate900,
    onTertiary = Slate900,
    onBackground = Color.White,
    onSurface = Color.White,
    error = CoralRed
)

private val LightColorScheme = lightColorScheme(
    primary = DarkIndigo,
    secondary = CyberTeal,
    tertiary = NeonPink,
    background = Slate50,
    surface = Color.White,
    surfaceVariant = Slate100,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    error = CoralRed
)

@Composable
fun SmartBudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is set to false by default for our colorful theme
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}