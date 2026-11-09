package com.cobalt.android.util

/**
 * Content-region preference (ISO-3166 alpha-2, or "" = unspecified). Distinct
 * from the UI language: this biases which feed/search results a source returns
 * (`gl` / `region` query params), the same axis VidMate exposes as its
 * "location" picker.
 */
object RegionHelper {

    val SUPPORTED = listOf(
        "" to "unspecified",
        "US" to "United States",
        "GB" to "United Kingdom",
        "IN" to "India",
        "BR" to "Brazil",
        "ID" to "Indonesia",
        "NG" to "Nigeria",
        "DE" to "Germany",
        "FR" to "France",
        "JP" to "Japan",
        "KR" to "South Korea",
        "MX" to "Mexico",
        "PH" to "Philippines",
        "PK" to "Pakistan",
        "BD" to "Bangladesh",
        "ZA" to "South Africa"
    )
}
