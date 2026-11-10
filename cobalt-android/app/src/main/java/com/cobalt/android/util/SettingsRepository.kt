package com.cobalt.android.util

import android.content.Context
import com.cobalt.android.shorts.source.InvidiousShortsSource

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("cobalt_settings", Context.MODE_PRIVATE)

    /**
     * Phase 12: the app's own cobalt integration (see
     * `LinkResolverRepository.ResolvedFormat`) never exposes numeric
     * quality tiers (1080p/720p/...) — the cobalt API contract this app
     * talks to only returns a format *type* (video / audio / photo) per
     * `picker` entry, no resolution ladder. ARCHITECTURE.md's Phase 12
     * spec calls this key "default download resolution"; it's persisted
     * here as a format-type preference instead, since that's the only
     * axis of choice that actually exists in `ResolveResult.Success`.
     * Phase 14 is responsible for reading this to pre-select in
     * `ResolutionPickerDialog` (Phase 6) — not wired yet.
     */
    enum class DownloadFormatPreference { ASK, VIDEO, AUDIO }

    /**
     * Phase 12/13: single 3-way theme setting. LIGHT/DARK map straight to
     * `AppCompatDelegate`'s night mode; DYNAMIC follows the system's
     * light/dark setting *and* turns on Material You dynamic color via
     * `DynamicColors.applyToActivitiesIfAvailable()` (API 31+; a no-op
     * below that). See CobaltApplication for where this is read and
     * ARCHITECTURE.md's Phase 13 entry for a known limitation: LIGHT
     * currently looks identical to DARK, because `colors.xml` is a single
     * hardcoded dark palette with no `values-night` counterpart — a
     * pre-existing Phase 1 gap this phase surfaces but doesn't fix.
     */
    enum class ThemeMode { LIGHT, DARK, DYNAMIC }

    /**
     * Internal cache only — NOT a user-facing setting anymore. Holds the
     * last cobalt instance URL [com.cobalt.android.remoteconfig.RemoteConfigRepository]
     * successfully fetched from `remote-config.json`, so a later launch
     * with GitHub briefly unreachable still has something better than the
     * hardcoded default to fall back to. See that class's doc comment for
     * the full fallback chain. Not shown or editable in Settings — the
     * instance URL is centrally managed now, not per-device.
     */
    var cachedRemoteCobaltUrl: String
        get() = prefs.getString("cobalt_url", "") ?: ""
        set(v) { prefs.edit().putString("cobalt_url", v).apply() }

    /**
     * Optional. cobalt's real auth scheme (github.com/imputnet/cobalt,
     * docs/api.md) is `Authorization: Api-Key <value>` on every request —
     * only required if the configured instance (see
     * `RemoteConfigRepository.getCobaltInstanceUrl()`) has
     * API-key auth turned on server-side; the public cobalt.tools default
     * doesn't. Blank means "send no Authorization header", which
     * [LinkResolverRepository] treats as the no-auth-required case.
     */
    var cobaltApiKey: String
        get() = prefs.getString("cobalt_api_key", "") ?: ""
        set(v) { prefs.edit().putString("cobalt_api_key", v.trim()).apply() }

    var audioOnlyMode: Boolean
        get() = prefs.getBoolean("audio_only", false)
        set(v) { prefs.edit().putBoolean("audio_only", v).apply() }

    var clipboardTriggerEnabled: Boolean
        get() = prefs.getBoolean("clipboard_trigger", true)
        set(v) { prefs.edit().putBoolean("clipboard_trigger", v).apply() }

    var firstLaunchDone: Boolean
        get() = prefs.getBoolean("first_launch_done", false)
        set(v) { prefs.edit().putBoolean("first_launch_done", v).apply() }

    /**
     * Whether the intro/coach screens have been seen. Distinct from
     * [firstLaunchDone], which MainActivity owns for the one-time permission
     * and battery-optimisation prompts — onboarding must not consume that
     * flag, or those prompts would be skipped entirely.
     */
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(v) { prefs.edit().putBoolean("onboarding_done", v).apply() }

    /** See [DownloadFormatPreference] KDoc above for the "resolution" naming gap. */
    var defaultDownloadFormat: DownloadFormatPreference
        get() = runCatching {
            DownloadFormatPreference.valueOf(
                prefs.getString("default_download_format", DownloadFormatPreference.ASK.name)
                    ?: DownloadFormatPreference.ASK.name
            )
        }.getOrDefault(DownloadFormatPreference.ASK)
        set(v) { prefs.edit().putString("default_download_format", v.name).apply() }

    /**
     * Relative path under the public Downloads collection. Defaults to the
     * same value `MediaStoreWriter` currently hardcodes ("Download/Cobalt"),
     * so persisting this key doesn't change existing behavior until a
     * later phase reads it from `MediaStoreWriter.open()` instead of the
     * literal.
     */
    var downloadLocation: String
        get() = prefs.getString("download_location", DEFAULT_DOWNLOAD_LOCATION) ?: DEFAULT_DOWNLOAD_LOCATION
        set(v) {
            val cleaned = v.trim().trim('/').ifBlank { DEFAULT_DOWNLOAD_LOCATION }
            prefs.edit().putString("download_location", cleaned).apply()
        }

    /** See [ThemeMode] KDoc above. Defaults to DYNAMIC. */
    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.DYNAMIC.name) ?: ThemeMode.DYNAMIC.name)
        }.getOrDefault(ThemeMode.DYNAMIC)
        set(v) { prefs.edit().putString("theme_mode", v.name).apply() }

    /**
     * Phase 14: Invidious instance pool
     * ([InvidiousShortsSource.DEFAULT_INSTANCES] is the shipped default,
     * used whenever this key is unset or ends up empty after cleaning).
     * Persisted newline-separated so a multi-line EditText in Settings can
     * round-trip it directly with no extra parsing UI. Read once at
     * `ShortsFeedRepository`'s `constructor(context)` — same "takes effect
     * next time the feed/repository is (re)constructed, not live-patched
     * into an already-running feed" contract [downloadLocation] documents
     * above for `MediaStoreWriter`.
     */
    var invidiousInstances: List<String>
        get() = prefs.getString("invidious_instances", null)
            ?.split("\n")
            ?.map { it.trim().trimEnd('/') }
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: InvidiousShortsSource.DEFAULT_INSTANCES
        set(v) {
            val cleaned = v.map { it.trim().trimEnd('/') }.filter { it.isNotBlank() }
            prefs.edit().putString("invidious_instances", cleaned.joinToString("\n")).apply()
        }

    /**
     * Phase 14 (the spec's optional-but-recommended half): user-editable
     * seed-query pool for
     * [com.cobalt.android.shorts.source.ShortsQueryFeeder]. Empty means
     * "use the shipped default pool" — same null-coalescing shape as
     * [invidiousInstances], except the empty-list fallback lives in
     * `ShortsQueryFeeder.applyCustomQueries` itself rather than here, so
     * this getter can just return what's stored (possibly empty).
     * Applied at process start (`CobaltApplication`) and again immediately
     * on save from Settings, so a change is visible on the very next feed
     * refresh without requiring an app restart.
     */
    var customShortsQueries: List<String>
        get() = prefs.getString("custom_shorts_queries", null)
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(v) {
            val cleaned = v.map { it.trim() }.filter { it.isNotBlank() }
            prefs.edit().putString("custom_shorts_queries", cleaned.joinToString("\n")).apply()
        }

    /**
     * Phase 20: persisted last-picked (or explicitly set once Settings UI
     * exists) [com.cobalt.android.transcode.TranscodeProfile.id], or empty
     * for "always ask" (the current, only-implemented behavior —
     * `QualitySelectionSheet` always shows the full sheet this phase; it
     * does not yet read these keys to skip itself the way
     * `ResolutionPickerDialog` skips itself for [defaultDownloadFormat]).
     * Storage layer only, same split as Phase 12 (storage) preceding
     * Phase 13 (UI) — no `SettingsFragment` control writes these yet.
     * Empty string, not null, is the "ask" sentinel so `getString`'s
     * non-null default keeps these two properties non-nullable `String`
     * rather than `String?`, consistent with every other key above.
     */
    var defaultVideoQualityProfileId: String
        get() = prefs.getString("default_video_quality_profile_id", "") ?: ""
        set(v) { prefs.edit().putString("default_video_quality_profile_id", v).apply() }

    var defaultAudioQualityProfileId: String
        get() = prefs.getString("default_audio_quality_profile_id", "") ?: ""
        set(v) { prefs.edit().putString("default_audio_quality_profile_id", v).apply() }

    /**
     * BCP-47 language tag for the app UI, or "" for "follow the system
     * default". Applied through `AppCompatDelegate.setApplicationLocales`,
     * which on API 33+ is also surfaced in the system's per-app language
     * settings. See `LocaleHelper` for the apply path.
     */
    var appLanguage: String
        get() = prefs.getString("app_language", "") ?: ""
        set(v) { prefs.edit().putString("app_language", v.trim()).apply() }

    /**
     * ISO-3166 country/region code (e.g. "US", "IN") used to bias feed and
     * extractor queries, or "" for "let each source decide". This is a content
     * preference, distinct from [appLanguage] (the UI language).
     */
    var contentRegion: String
        get() = prefs.getString("content_region", "") ?: ""
        set(v) { prefs.edit().putString("content_region", v.trim().uppercase()).apply() }

    /** Master switch for download-progress notifications. */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(v) { prefs.edit().putBoolean("notifications_enabled", v).apply() }

    /** Whether a notification is posted when a download finishes. */
    var notifyOnComplete: Boolean
        get() = prefs.getBoolean("notify_on_complete", true)
        set(v) { prefs.edit().putBoolean("notify_on_complete", v).apply() }

    /**
     * Whether the on-device "because you watched X" recommender may post.
     * Default off: a recommender is opt-in, unlike download notifications,
     * because it is the app volunteering content rather than reporting on a
     * task the user started.
     */
    var recommendationsEnabled: Boolean
        get() = prefs.getBoolean("recommendations_enabled", false)
        set(v) { prefs.edit().putBoolean("recommendations_enabled", v).apply() }

    /** Extra host suffixes fetched from remote config (Phase E7), persisted so
     *  a cold launch with no network still honours the last-known list. */
    var remoteExtraHosts: Set<String>
        get() = prefs.getStringSet("remote_extra_hosts", emptySet()) ?: emptySet()
        set(v) { prefs.edit().putStringSet("remote_extra_hosts", v).apply() }

    /** Content-language rule: "any", "single", or "max2" (VidMate's rule). */
    var contentLanguageRule: String
        get() = prefs.getString("content_language_rule", "any") ?: "any"
        set(v) { prefs.edit().putString("content_language_rule", v).apply() }

    /** Preferred content language (ISO-639-1) used by [contentLanguageRule]. */
    var preferredContentLanguage: String
        get() = prefs.getString("preferred_content_language", "") ?: ""
        set(v) { prefs.edit().putString("preferred_content_language", v.trim().lowercase()).apply() }

    /** Parallel segment count for segmented downloads (1..16). */
    var parallelSegments: Int
        get() = prefs.getInt("parallel_segments", 4)
        set(v) { prefs.edit().putInt("parallel_segments", v.coerceIn(1, 16)).apply() }

    /** Floating (overlay) player enabled — needs SYSTEM_ALERT_WINDOW. */
    var floatingPlayerEnabled: Boolean
        get() = prefs.getBoolean("floating_player", false)
        set(v) { prefs.edit().putBoolean("floating_player", v).apply() }

    /** Online lyrics search enabled (uses LRCLIB). */
    var onlineLyricsEnabled: Boolean
        get() = prefs.getBoolean("online_lyrics", true)
        set(v) { prefs.edit().putBoolean("online_lyrics", v).apply() }

    /** Music sleep timer in minutes (0 = off). */
    var sleepTimerMinutes: Int
        get() = prefs.getInt("sleep_timer_minutes", 0)
        set(v) { prefs.edit().putInt("sleep_timer_minutes", v.coerceAtLeast(0)).apply() }

    /** Whether share links are shortened through the configured host. */
    var shortenShareLinks: Boolean
        get() = prefs.getBoolean("shorten_share_links", false)
        set(v) { prefs.edit().putBoolean("shorten_share_links", v).apply() }

    /** Share-link shortener host base URL (blank = built-in default). */
    var shareShortenerHost: String
        get() = prefs.getString("share_shortener_host", "") ?: ""
        set(v) { prefs.edit().putString("share_shortener_host", v.trim()).apply() }

    /** Persistent keep-alive (resident) notification enabled. */
    var keepAliveNotice: Boolean
        get() = prefs.getBoolean("keep_alive_notice", false)
        set(v) { prefs.edit().putBoolean("keep_alive_notice", v).apply() }

    /** Whether browser history is recorded (off = incognito-always). */
    var browserHistoryEnabled: Boolean
        get() = prefs.getBoolean("browser_history", true)
        set(v) { prefs.edit().putBoolean("browser_history", v).apply() }

    /** Whether browser history/bookmarks are retained at all. */
    var browserBookmarksEnabled: Boolean
        get() = prefs.getBoolean("browser_bookmarks", true)
        set(v) { prefs.edit().putBoolean("browser_bookmarks", v).apply() }

    /** Browser user-agent to report (blank = app default, spoofed as a browser). */
    var browserUserAgent: String
        get() = prefs.getString("browser_user_agent", "") ?: ""
        set(v) { prefs.edit().putString("browser_user_agent", v.trim()).apply() }

    /** Whether the browser hides its downloader identity (anti-detection). */
    var browserAntiDetection: Boolean
        get() = prefs.getBoolean("browser_anti_detection", true)
        set(v) { prefs.edit().putBoolean("browser_anti_detection", v).apply() }

    /** Dynamic code loading / hot-fix allowed (off by default; opt-in). */
    var hotfixEnabled: Boolean
        get() = prefs.getBoolean("hotfix_enabled", false)
        set(v) { prefs.edit().putBoolean("hotfix_enabled", v).apply() }

    /** Bottom-bar tab ids from remote config (Phase H3), comma-joined. */
    var remoteTabs: String
        get() = prefs.getString("remote_tabs", "") ?: ""
        set(v) { prefs.edit().putString("remote_tabs", v).apply() }

    /** Vault passcode (blank = vault not yet set up). Stored in app-private
     *  SharedPreferences; the vault's real protection is the private
     *  directory, not this value. */
    var vaultPasscode: String
        get() = prefs.getString("vault_passcode", "") ?: ""
        set(v) { prefs.edit().putString("vault_passcode", v).apply() }

    companion object {
        const val DEFAULT_DOWNLOAD_LOCATION = "Download/Cobalt"
    }
}
