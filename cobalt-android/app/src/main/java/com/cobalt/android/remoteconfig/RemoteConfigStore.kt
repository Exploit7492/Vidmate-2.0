package com.cobalt.android.remoteconfig

import android.content.Context
import com.cobalt.android.util.SettingsRepository

/**
 * The process-wide snapshot of the fetched remote config (Phase E7 / H3).
 *
 * `remote-config.json` (a public file in this project's repo) can now carry,
 * besides `cobaltInstanceUrl`:
 *
 *  - `supportedHosts`: extra host suffixes to treat as downloadable. This is
 *    what makes the extractor list **updatable without an app release** — the
 *    single most valuable VidMate capability for a downloader, since new sites
 *    appear faster than app releases. [UrlMatcher] consults this set on top of
 *    its built-in hosts.
 *  - `tabs`: the bottom-bar tabs to show (any of `home`/`library`/`shorts`/`me`).
 *    Server-side tab configuration, so a tab can be rolled out or pulled back
 *    centrally.
 *
 * The store is a singleton because the config is global to the process and is
 * read from non-ViewModel call sites (`UrlMatcher`, `MainActivity`). It is
 * seeded from a persisted cache at startup so a cold launch with no network
 * still honours the last-known config.
 */
object RemoteConfigStore {

    @Volatile private var extraHosts: Set<String> = emptySet()
    @Volatile private var tabs: List<String>? = null

    fun extraHosts(): Set<String> = extraHosts
    fun tabs(): List<String>? = tabs

    /** Loads the persisted snapshot (called from Application.onCreate). */
    fun seedFromCache(context: Context) {
        val settings = SettingsRepository(context)
        extraHosts = settings.remoteExtraHosts
        tabs = settings.remoteTabs.takeIf { it.isNotBlank() }
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
    }

    /** Applies a freshly fetched config and persists it for the next launch. */
    fun update(context: Context, hosts: Set<String>, tabs: List<String>?) {
        extraHosts = hosts
        this.tabs = tabs
        val settings = SettingsRepository(context)
        settings.remoteExtraHosts = hosts
        settings.remoteTabs = tabs?.joinToString(",") ?: ""
    }
}
