package com.example.avatar_ai_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Crimson,
    background = DarkGray,
    surface = Thunder,
    primaryContainer = Manatee,
    secondaryContainer = Nepal,
    onSurface = White,
    inverseOnSurface = Black

)

private val LightColorScheme = lightColorScheme(
    primary = Crimson,
    background = WildSand,
    surface = Alto,
    primaryContainer = Silver,
    secondaryContainer = Malibu,
    onSurface = Black,
    inverseOnSurface = White
)

@Composable
fun ARAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val myColorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = myColorScheme,
            typography = Typography,
            content = content
        )
    }
}