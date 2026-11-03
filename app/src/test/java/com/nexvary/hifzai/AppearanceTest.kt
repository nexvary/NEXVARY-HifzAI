package com.nexvary.hifzai

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nexvary.hifzai.learning.AppAppearance
import org.junit.Assert.*
import org.junit.Test

class AppearanceTest {
    @Test fun legacyAndUnknownPreferencesRemainDarkAndSystemTracksItsInput() {
        for (value in listOf(null, "", "corrupt", "light")) assertEquals(AppAppearance.DARK, AppAppearance.fromStored(value))
        assertEquals(AppAppearance.LIGHT, AppAppearance.fromStored("LIGHT"))
        assertTrue(AppAppearance.DARK.usesDarkColors(false))
        assertFalse(AppAppearance.LIGHT.usesDarkColors(true))
        assertFalse(AppAppearance.SYSTEM.usesDarkColors(false))
        assertTrue(AppAppearance.SYSTEM.usesDarkColors(true))
    }

    @Test fun normalTextAndSemanticContainersKeepAtLeastFourPointFiveContrast() {
        fun contrast(a: Color, b: Color): Float {
            val first = a.luminance(); val second = b.luminance()
            return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
        }
        for (scheme in listOf(HifzDarkColors, HifzLightColors)) {
            val pairs = listOf(scheme.onBackground to scheme.background, scheme.onSurface to scheme.surface,
                scheme.onSurfaceVariant to scheme.background, scheme.onSurfaceVariant to scheme.surface,
                scheme.primary to scheme.background, scheme.primary to scheme.surface,
                scheme.secondary to scheme.background, scheme.secondary to scheme.surface,
                scheme.onPrimary to scheme.primary, scheme.onSecondary to scheme.secondary,
                scheme.onSecondaryContainer to scheme.secondaryContainer,
                scheme.onErrorContainer to scheme.errorContainer)
            for ((foreground, background) in pairs)
                assertTrue("Insufficient text contrast: $foreground / $background", contrast(foreground, background) >= 4.5f)
        }
    }
}
