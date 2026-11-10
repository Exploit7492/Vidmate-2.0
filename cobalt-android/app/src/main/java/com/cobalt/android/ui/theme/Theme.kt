package com.cobalt.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Both palettes, mirroring `values/colors.xml` (light) and
 * `values-night/colors.xml` (dark) exactly, so a Compose screen and a
 * still-XML surface in the same screen never disagree.
 *
 * Light is "ancient blue crescent over glass": deep navy ink on a pale
 * blue-tinted frosted background, with a muted crescent gold as the second
 * accent. Dark is "polished dark + gold + ruby": warm near-black surfaces,
 * a gold primary and a ruby action colour.
 *
 * Which one is used is decided by [isSystemInDarkTheme], which
 * AppCompatDelegate's night mode (driven by `SettingsRepository.ThemeMode`
 * through `ThemeApplier`) already reflects — so the theme picker in Settings
 * changes Compose screens too.
 */

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1E4B7A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E5F6),
    onPrimaryContainer = Color(0xFF0F2438),
    secondary = Color(0xFFB3242E),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFC9A227),
    onTertiary = Color(0xFF2A2100),
    background = Color(0xFFEEF3FA),
    onBackground = Color(0xFF0F2438),
    surface = Color(0xFFE7EFF9),
    onSurface = Color(0xFF0F2438),
    surfaceVariant = Color(0xFFDCE8F6),
    onSurfaceVariant = Color(0xFF47617C),
    outline = Color(0xFFC2D2E4),
    error = Color(0xFFB3242E),
    onError = Color(0xFFFFFFFF)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF231C00),
    primaryContainer = Color(0xFF3A2F0A),
    onPrimaryContainer = Color(0xFFF6E3A8),
    secondary = Color(0xFFB21E35),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFE8CD74),
    onTertiary = Color(0xFF231C00),
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFF6F1E4),
    surface = Color(0xFF141519),
    onSurface = Color(0xFFF6F1E4),
    surfaceVariant = Color(0xFF1B1C22),
    onSurfaceVariant = Color(0xFFB9B4A6),
    outline = Color(0xFF33353E),
    error = Color(0xFFB21E35),
    onError = Color(0xFFFFFFFF)
)

/**
 * Glass tokens that Material3's [androidx.compose.material3.ColorScheme] has no
 * slot for. Read them through [CobaltTheme.glass] rather than hardcoding a
 * translucent colour at a call site.
 */
data class GlassColors(
    val fill: Color,
    val fillStrong: Color,
    val border: Color,
    val sheen: Color
)

private val LightGlass = GlassColors(
    fill = Color(0x8CF2F7FE),
    fillStrong = Color(0xD9FFFFFF),
    border = Color(0x66FFFFFF),
    sheen = Color(0x33FFFFFF)
)

private val DarkGlass = GlassColors(
    fill = Color(0xB3141519),
    fillStrong = Color(0xE61B1C22),
    border = Color(0x40D4AF37),
    sheen = Color(0x1AD4AF37)
)

object CobaltTheme {
    val glass: GlassColors
        @Composable get() = LocalGlassColors.current
}

private val LocalGlassColors = staticCompositionLocalOf { LightGlass }

@Composable
fun CobaltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val glass = if (darkTheme) DarkGlass else LightGlass
    CompositionLocalProvider(LocalGlassColors provides glass) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
