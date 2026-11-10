package com.cobalt.android.util

/**
 * Cheap, offline language guess from text, used only to feed
 * [ContentLanguageRule] when a source does not state an item's language. It
 * classifies by Unicode script, which is enough to separate the languages a
 * feed actually mixes (Latin/English vs. the CJK/Arabic/Cyrillic/Devanagari
 * blocks) without shipping a full language-identification model.
 *
 * Pure and Android-free, so it is unit-tested. Returns "und" (undetermined)
 * when nothing distinctive is found, and callers treat that as "don't filter".
 */
object LanguageDetector {

    const val UNDETERMINED = "und"

    /** Best-effort ISO-639-1 tag for [text], or [UNDETERMINED]. */
    fun detect(text: String): String {
        if (text.isBlank()) return UNDETERMINED
        var han = 0
        var kana = 0
        var hangul = 0
        var cyrillic = 0
        var arabic = 0
        var devanagari = 0
        for (ch in text) {
            when (Character.UnicodeBlock.of(ch)) {
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A -> han++
                Character.UnicodeBlock.HIRAGANA,
                Character.UnicodeBlock.KATAKANA -> kana++
                Character.UnicodeBlock.HANGUL_SYLLABLES,
                Character.UnicodeBlock.HANGUL_JAMO -> hangul++
                Character.UnicodeBlock.CYRILLIC -> cyrillic++
                Character.UnicodeBlock.ARABIC -> arabic++
                Character.UnicodeBlock.DEVANAGARI -> devanagari++
                else -> {}
            }
        }
        // Kana is the decisive marker for Japanese; Han without kana is Chinese.
        return when {
            kana > 0 -> "ja"
            hangul > 0 -> "ko"
            han > 0 -> "zh"
            arabic > 0 -> "ar"
            cyrillic > 0 -> "ru"
            devanagari > 0 -> "hi"
            else -> UNDETERMINED
        }
    }
}
