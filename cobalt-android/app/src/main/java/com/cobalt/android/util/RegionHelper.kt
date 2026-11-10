package com.cobalt.android.util

/**
 * Content-region preference (ISO-3166 alpha-2, or "" = unspecified). Distinct
 * from the UI language: this biases which feed/search results a source returns
 * (`gl` / `region` query params), the same axis VidMate exposes as its
 * "location" picker.
 */
object RegionHelper {

    /** The content regions offered in Settings: unspecified plus the full
     *  [LanguageCatalog] region list (107 regions). */
    val SUPPORTED: List<Pair<String, String>> =
        listOf("" to "unspecified") + LanguageCatalog.REGIONS
}
