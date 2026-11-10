package com.cobalt.android.plugin

import android.content.Context

/**
 * The contract every add-on implements. A host gets a one-time [onLoad] and a
 * [onUnload] when it is disabled, so a module can register hooks (a custom
 * extractor, an extra share target, a download post-processor) without the app
 * having to know the module's types at compile time.
 *
 * Deliberately tiny: the platform owns the lifecycle, the module only reacts.
 */
interface PluginHost {
    fun onLoad(context: Context, registry: PluginRegistry)
    fun onUnload() {}
}
