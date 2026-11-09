package com.cobalt.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * The Compose theme every screen lands on. The dark palette mirrors the
 * VidMate (com.nemo.vidmate) design language recovered from the decompiled
 * 6.0204 build: a near-black surface stack with brand blue (#0099FF) as the
 * single accent and VidMate red (#EA3F40) as the secondary/like accent. These
 * are the same hex values as the XML `colors.xml` palette, so Compose and
 * still-XML surfaces (dialogs, sheets) stay visually consistent.
 */
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

// Light scheme exists for completeness/system-theme-switching correctness;
// the app ships a single dark palette, so this is only reached if a future
// phase adds real light-mode screens.
private val CobaltLightColorScheme = lightColorScheme(
    primary = Color(0xFF0099FF),
    secondary = Color(0xFFEA3F40),
    error = Color(0xFFEA3F40)
)

@Composable
fun CobaltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Every shipped screen is a near-pure-black dark surface already, so the
    // default keeps a Compose screen matching its still-XML neighbours.
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
        darkTheme -> CobaltDarkColorScheme
        else -> CobaltLightColorScheme
    }
    val finalScheme = if (darkTheme && pureBlack) {
        colorScheme.copy(background = Color.Black)
    } else {
        colorScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        content = content
    )
}
