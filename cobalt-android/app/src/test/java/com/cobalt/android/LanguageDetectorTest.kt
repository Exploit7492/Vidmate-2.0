package com.cobalt.android

import com.cobalt.android.util.LanguageDetector
import org.junit.Assert.assertEquals
import org.junit.Test

/** JVM tests for the script-based language guess used by the content rule (§1). */
class LanguageDetectorTest {

    @Test fun japanese_has_kana() {
        assertEquals("ja", LanguageDetector.detect("こんにちは世界"))
    }

    @Test fun chinese_is_han_without_kana() {
        assertEquals("zh", LanguageDetector.detect("你好世界"))
    }

    @Test fun korean_is_hangul() {
        assertEquals("ko", LanguageDetector.detect("안녕하세요"))
    }

    @Test fun cyrillic_is_russian() {
        assertEquals("ru", LanguageDetector.detect("Привет мир"))
    }

    @Test fun arabic() {
        assertEquals("ar", LanguageDetector.detect("مرحبا بالعالم"))
    }

    @Test fun latin_text_is_undetermined() {
        assertEquals(LanguageDetector.UNDETERMINED, LanguageDetector.detect("Hello world"))
    }

    @Test fun blank_is_undetermined() {
        assertEquals(LanguageDetector.UNDETERMINED, LanguageDetector.detect("   "))
    }
}
