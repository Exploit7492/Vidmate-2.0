package com.cobalt.android.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

/**
 * Turns a persisted [SettingsRepository.ThemeMode] into real
 * `AppCompatDelegate`/`DynamicColors` calls. Shared by `CobaltApplication`
 * (applied once at process start, before any Activity exists) and the
 * Settings screen (applied immediately on change, then the activity
 * recreates).
 *
 * LIGHT and DARK are now genuinely distinct: `values/colors.xml` is the light
 * palette and `values-night/colors.xml` is the dark one, and `Theme.Cobalt`
 * extends `Theme.Material3.DayNight`, so the night-mode flag actually selects
 * a different resource set. DYNAMIC follows the system night mode and turns
 * on Material You re-tinting on API 31+.
 */
object ThemeApplier {
    fun apply(context: Context, mode: SettingsRepository.ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                SettingsRepository.ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                SettingsRepository.ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                SettingsRepository.ThemeMode.DYNAMIC -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        if (mode == SettingsRepository.ThemeMode.DYNAMIC) {
            DynamicColors.applyToActivitiesIfAvailable(
                context.applicationContext as android.app.Application
            )
        }
    }
}
