package com.jeannie.artsyfinal.ui.theme

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
import android.util.Log

// Custom light color scheme
private val LightColorScheme = lightColorScheme(
    // Main primary color - the light blue
    primary = Color(0xFFE8E8FF),
    onPrimary = Color.Black,

    // Container colors - lighter versions of primary for surfaces
    primaryContainer = Color(0xFFD0DDFF),
    onPrimaryContainer = Color(0xFF001A43),

    // Background color - the main background of the app
    background = Color.White,
    onBackground = Color.Black,

    // Surface color - for cards, app bars, etc.
    surface = Color(0xFFE8F0FF),  // Very light blue for surface in light theme
    onSurface = Color(0xFF001A43),  // Dark text for contrast

    // Other essential colors
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),

    // Additional required colors
    tertiary = Color(0xFF324185),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBD7FC),
    onTertiaryContainer = Color(0xFF2B122C),


    error = Color(0xFFDFDFE6),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    //F2F2F7
//    surfaceVariant = Color(0xFFE7E0EC),
//    onSurfaceVariant = Color(0xFF49454F),

    surfaceVariant = Color(0xFFF2F2F7),  // Light gray with subtle lavender undertone
    onSurfaceVariant = Color(0xFF49454F),

    outline = Color(0xFFE8E8ED) ,
    outlineVariant = Color(0xFFCAC4D0)
)

// Custom dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF324185),             // Same brand blue for consistency
    //primary = Color.Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF324185),    // Darker container color
    onPrimaryContainer = Color(0xFFD0DDFF),  // Lighter text for contrast

    background = Color(0xFF121212),          // Pure dark background
    onBackground = Color.White,

    surface = Color(0xFF1E2A50),             // Navy surface for cards / top bar
    onSurface = Color.White,

    secondary = Color(0xFFBBC7DB),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D4859),
    onSecondaryContainer = Color(0xFFD7E3F8),

    tertiary = Color(0xFFE8E8FF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5A3D5C),
    onTertiaryContainer = Color(0xFFFBD7FC),

    error = Color(0xFF333333),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Updated color for dark/black surface variant
    surfaceVariant = Color(0xFF222222),  // Very dark black/charcoal
    onSurfaceVariant = Color(0xFFE1E1E1),  // Light gray for good contrast on dark background

    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun ArtsyFinalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to ensure your colors are used
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Add debug logging to see which theme is actually being applied
    Log.d("ThemeDebug", "Is Dark Theme: $darkTheme, Dynamic Color: $dynamicColor")

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Debug log the actual primary color being used
    Log.d("ThemeDebug", "Primary Color: ${colorScheme.primary}")

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}