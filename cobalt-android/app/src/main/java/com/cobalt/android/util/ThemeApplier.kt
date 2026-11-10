package com.cobalt.android.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Turns a persisted [SettingsRepository.ThemeMode] into a real
 * `AppCompatDelegate` night-mode call. Shared by `CobaltApplication` (applied
 * once at process start, before any Activity exists) and the Settings screen
 * (applied immediately on change, then the activity recreates).
 *
 * This deliberately does *not* call `DynamicColors.applyToActivitiesIfAvailable()`.
 * Material You re-tinting layered on top of the app's own palette is what made
 * some screens resolve to a system-tinted light scheme and others to the app's
 * dark one; every surface now goes through `CobaltTheme`, and the night flag is
 * the only input it needs.
 */
object ThemeApplier {
    fun apply(context: Context, mode: SettingsRepository.ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                SettingsRepository.ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                SettingsRepository.ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                SettingsRepository.ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
