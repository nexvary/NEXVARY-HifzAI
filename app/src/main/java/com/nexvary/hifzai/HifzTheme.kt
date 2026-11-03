package com.nexvary.hifzai

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nexvary.hifzai.learning.AppAppearance

internal val HifzDarkColors = darkColorScheme(
    primary = Color(0xFFD6B465), onPrimary = Color(0xFF081521),
    background = Color(0xFF081521), onBackground = Color(0xFFEEF1F2),
    surface = Color(0xFF122B39), onSurface = Color(0xFFEEF1F2),
    surfaceVariant = Color(0xFF122B39), onSurfaceVariant = Color(0xFFB9C6CF),
    secondary = Color(0xFF43C8AA), onSecondary = Color(0xFF081521),
    secondaryContainer = Color(0xFF103B37), onSecondaryContainer = Color(0xFFBAF4E2),
    errorContainer = Color(0xFF5A2930), onErrorContainer = Color(0xFFFFE0DE),
    outline = Color(0xFF95A7B2)
)

internal val HifzLightColors = lightColorScheme(
    primary = Color(0xFF745515), onPrimary = Color.White,
    background = Color(0xFFF6F8F4), onBackground = Color(0xFF142E39),
    surface = Color.White, onSurface = Color(0xFF142E39),
    surfaceVariant = Color(0xFFE5ECE6), onSurfaceVariant = Color(0xFF425B62),
    secondary = Color(0xFF006A55), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F2E8), onSecondaryContainer = Color(0xFF123D32),
    errorContainer = Color(0xFFF8DEDC), onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF667C77)
)

@Composable
internal fun HifzTheme(appearance: AppAppearance, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (appearance.usesDarkColors(isSystemInDarkTheme()))
        HifzDarkColors else HifzLightColors, content = content)
}
