package com.cobalt.android.ui.theme

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

/**
 * Both palettes, mirroring `values/colors.xml` (light) and
 * `values-night/colors.xml` (dark) exactly, so a Compose screen and a
 * still-XML surface in the same screen never disagree. Which one is used is
 * decided by [isSystemInDarkTheme], which AppCompatDelegate's night mode
 * (driven by `SettingsRepository.ThemeMode` through `ThemeApplier`) already
 * reflects — so the theme picker in Settings changes Compose screens too.
 */

private val CobaltLightColorScheme = lightColorScheme(
    primary = Color(0xFF0099FF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFE0342F),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111418),
    surface = Color(0xFFF6F7F9),
    onSurface = Color(0xFF111418),
    surfaceVariant = Color(0xFFECEEF1),
    onSurfaceVariant = Color(0xFF5A6169),
    outline = Color(0xFFE2E5EA),
    error = Color(0xFFE0342F),
    onError = Color(0xFFFFFFFF)
)

private val CobaltDarkColorScheme = darkColorScheme(
    primary = Color(0xFF0099FF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFEA3F40),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFA8A8A8),
    outline = Color(0xFF2A2A2A),
    error = Color(0xFFEA3F40),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun CobaltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Only affects the dark scheme — collapses surface/background to pure
    // black to match the VidMate-derived look. Ignored in light mode.
    pureBlack: Boolean = true,
    // Real system dynamic color (Android 12+/API 31+) when explicitly asked
    // for; the default keeps this app's own brand palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> CobaltDarkColorScheme
        else -> CobaltLightColorScheme
    }
    val finalScheme = if (darkTheme && pureBlack) {
        colorScheme.copy(background = Color.Black, surface = Color.Black)
    } else {
        colorScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        content = content
    )
}
