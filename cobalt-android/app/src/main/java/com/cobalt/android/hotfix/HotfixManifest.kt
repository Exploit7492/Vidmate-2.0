package com.cobalt.android.hotfix

/**
 * Records which hot-fix is active and how to verify it. Kept separate from
 * [HotfixLoader] so the version/URL policy lives in one place, the same way
 * [com.cobalt.android.remoteconfig.RemoteConfigRepository] centralises the
 * instance URL.
 */
object HotfixManifest {
    /** Newest patch version the app knows about; empty = none advertised. */
    const val LATEST_VERSION = ""

    /** Where a patch dex is fetched from; empty = hot-fixes are not served. */
    const val DOWNLOAD_URL = ""

    /** True when the app is configured to offer hot-fixes at all. */
    val isAvailable: Boolean get() = DOWNLOAD_URL.isNotBlank()
}
