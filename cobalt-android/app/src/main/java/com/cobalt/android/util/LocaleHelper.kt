package com.cobalt.android.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies the persisted UI language. Uses the AndroidX per-app language API
 * (`AppCompatDelegate.setApplicationLocales`), which on API 33+ also shows the
 * choice in the system's Settings → Apps → Cobalt → Language screen, and on
 * older versions is backed by AppCompat's own storage. An empty tag means
 * "follow the system default".
 */
object LocaleHelper {

    /**
     * The UI languages offered in Settings: the system default plus the full
     * [LanguageCatalog] (83 languages), so a translation pass has every slot
     * already wired. Selecting a language is a real, persisted,
     * system-integrated preference (`AppCompatDelegate.setApplicationLocales`).
     */
    val SUPPORTED: List<Pair<String, String>> =
        listOf("" to "system default") + LanguageCatalog.LANGUAGES

    fun apply(tag: String) {
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
