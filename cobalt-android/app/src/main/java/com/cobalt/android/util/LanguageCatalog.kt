package com.cobalt.android.util

/**
 * The shipped UI-language catalogue (83 selectable languages) and the region
 * catalogue (107 selectable regions), matching the VidMate reference counts.
 *
 * The pair list is the single source of truth: [LocaleHelper.SUPPORTED] and
 * [RegionHelper.SUPPORTED] are derived from it, and a JVM test asserts the
 * counts so the "83 / 107" figures can't silently drift.
 */
object LanguageCatalog {

    /** BCP-47 tag to native display name. "" (system default) is added by callers. */
    val LANGUAGES: List<Pair<String, String>> = listOf(
        "en" to "English", "zh-CN" to "中文（简体）", "zh-TW" to "中文（繁體）",
        "hi" to "हिन्दी", "es" to "Español", "ar" to "العربية", "pt" to "Português",
        "bn" to "বাংলা", "ru" to "Русский", "ja" to "日本語", "de" to "Deutsch",
        "fr" to "Français", "id" to "Bahasa Indonesia", "ur" to "اردو", "tr" to "Türkçe",
        "it" to "Italiano", "vi" to "Tiếng Việt", "th" to "ไทย", "ko" to "한국어",
        "fa" to "فارسی", "ta" to "தமிழ்", "te" to "తెలుగు", "mr" to "मराठी",
        "gu" to "ગુજરાતી", "kn" to "ಕನ್ನಡ", "ml" to "മലയാളം", "pa" to "ਪੰਜਾਬੀ",
        "or" to "ଓଡ଼ିଆ", "as" to "অসমীয়া", "ne" to "नेपाली", "si" to "සිංහල",
        "my" to "မြန်မာ", "km" to "ខ្មែរ", "lo" to "ລາວ", "tl" to "Filipino",
        "ms" to "Bahasa Melayu", "jv" to "Basa Jawa", "su" to "Basa Sunda",
        "pl" to "Polski", "uk" to "Українська", "ro" to "Română", "nl" to "Nederlands",
        "el" to "Ελληνικά", "cs" to "Čeština", "hu" to "Magyar", "sv" to "Svenska",
        "da" to "Dansk", "fi" to "Suomi", "no" to "Norsk", "he" to "עברית",
        "sr" to "Српски", "hr" to "Hrvatski", "bg" to "Български", "sk" to "Slovenčina",
        "sl" to "Slovenščina", "lt" to "Lietuvių", "lv" to "Latviešu", "et" to "Eesti",
        "ca" to "Català", "eu" to "Euskara", "gl" to "Galego", "af" to "Afrikaans",
        "sw" to "Kiswahili", "am" to "አማርኛ", "ha" to "Hausa", "yo" to "Yorùbá",
        "ig" to "Igbo", "zu" to "isiZulu", "xh" to "isiXhosa", "so" to "Soomaali",
        "az" to "Azərbaycan", "kk" to "Қазақ", "uz" to "Oʻzbek", "ky" to "Кыргызча",
        "tg" to "Тоҷикӣ", "hy" to "Հայերեն", "ka" to "ქართული", "mn" to "Монгол",
        "bo" to "བོད་ཡིག", "ps" to "پښتو", "ku" to "Kurdî",
        "dv" to "ދިވެހި", "ti" to "ትግርኛ"
    )

    /** ISO-3166 alpha-2 code to English region name. "" (unspecified) added by callers. */
    val REGIONS: List<Pair<String, String>> = listOf(
        "US" to "United States", "GB" to "United Kingdom", "IN" to "India",
        "BR" to "Brazil", "ID" to "Indonesia", "NG" to "Nigeria", "DE" to "Germany",
        "FR" to "France", "JP" to "Japan", "KR" to "South Korea", "MX" to "Mexico",
        "PH" to "Philippines", "PK" to "Pakistan", "BD" to "Bangladesh",
        "ZA" to "South Africa", "EG" to "Egypt", "VN" to "Vietnam", "TH" to "Thailand",
        "TR" to "Türkiye", "IR" to "Iran", "IT" to "Italy", "ES" to "Spain",
        "CA" to "Canada", "AU" to "Australia", "AR" to "Argentina", "CO" to "Colombia",
        "CL" to "Chile", "PE" to "Peru", "VE" to "Venezuela", "RU" to "Russia",
        "UA" to "Ukraine", "PL" to "Poland", "RO" to "Romania", "NL" to "Netherlands",
        "BE" to "Belgium", "SE" to "Sweden", "NO" to "Norway", "DK" to "Denmark",
        "FI" to "Finland", "PT" to "Portugal", "GR" to "Greece", "CZ" to "Czechia",
        "HU" to "Hungary", "AT" to "Austria", "CH" to "Switzerland", "IE" to "Ireland",
        "NZ" to "New Zealand", "SA" to "Saudi Arabia", "AE" to "United Arab Emirates",
        "IL" to "Israel", "IQ" to "Iraq", "SY" to "Syria", "JO" to "Jordan",
        "LB" to "Lebanon", "KW" to "Kuwait", "QA" to "Qatar", "OM" to "Oman",
        "BH" to "Bahrain", "YE" to "Yemen", "MA" to "Morocco", "DZ" to "Algeria",
        "TN" to "Tunisia", "LY" to "Libya", "SD" to "Sudan", "KE" to "Kenya",
        "GH" to "Ghana", "ET" to "Ethiopia", "TZ" to "Tanzania", "UG" to "Uganda",
        "CM" to "Cameroon", "CI" to "Côte d'Ivoire", "SN" to "Senegal",
        "ZW" to "Zimbabwe", "ZM" to "Zambia", "MZ" to "Mozambique", "AO" to "Angola",
        "MY" to "Malaysia", "SG" to "Singapore", "MM" to "Myanmar", "KH" to "Cambodia",
        "LA" to "Laos", "NP" to "Nepal", "LK" to "Sri Lanka", "AF" to "Afghanistan",
        "KZ" to "Kazakhstan", "UZ" to "Uzbekistan", "AZ" to "Azerbaijan",
        "GE" to "Georgia", "AM" to "Armenia", "MN" to "Mongolia", "TW" to "Taiwan",
        "HK" to "Hong Kong", "MO" to "Macao", "CN" to "China", "CU" to "Cuba",
        "DO" to "Dominican Republic", "GT" to "Guatemala", "EC" to "Ecuador",
        "BO" to "Bolivia", "PY" to "Paraguay", "UY" to "Uruguay", "CR" to "Costa Rica",
        "PA" to "Panama", "PR" to "Puerto Rico", "RS" to "Serbia", "BG" to "Bulgaria", "MD" to "Moldova"
    )
}
