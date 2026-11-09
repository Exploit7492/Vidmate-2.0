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

    /** A small, curated list of UI languages to offer in Settings. The app's
     *  own strings are English today, but selecting a language here is a real,
     *  persisted, system-integrated preference (and the hook a translation
     *  pass would plug into). */
    val SUPPORTED = listOf(
        "" to "system default",
        "en" to "English",
        "hi" to "हिन्दी (Hindi)",
        "es" to "Español",
        "pt" to "Português",
        "ar" to "العربية",
        "id" to "Bahasa Indonesia",
        "ru" to "Русский",
        "fr" to "Français",
        "de" to "Deutsch",
        "bn" to "বাংলা (Bengali)",
        "ur" to "اردو",
        "tr" to "Türkçe",
        "vi" to "Tiếng Việt",
        "th" to "ไทย",
        "zh-CN" to "中文（简体）"
    )

    fun apply(tag: String) {
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
