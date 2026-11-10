package com.cobalt.android.plugin.builtin

import android.content.Context
import com.cobalt.android.plugin.PluginHost
import com.cobalt.android.plugin.PluginRegistry

/**
 * The modules that ship inside the APK. They exist so the Plugins screen has a
 * real list to show and so a downloaded module has something to hook into; each
 * is a thin marker that registers its capability with the [PluginRegistry].
 *
 * A downloaded (external) module implements [PluginHost] the same way and is
 * registered under its own id, which is what lets the feature set grow without
 * an app update.
 */
class CoreModule : PluginHost {
    override fun onLoad(context: Context, registry: PluginRegistry) {
        // The download engine and queue are always present; nothing to wire.
    }
}

class ExtractorsModule : PluginHost {
    override fun onLoad(context: Context, registry: PluginRegistry) {
        // Site extractors (Innertube/NewPipe) are compiled in; this module is
        // the hook point for an externally-supplied extractor pack.
    }
}

class TorrentModule : PluginHost {
    override fun onLoad(context: Context, registry: PluginRegistry) {
        // BitTorrent support is compiled in via libtorrent4j; the module marks
        // it as toggleable so a user who never downloads magnets can turn the
        // native libraries' code path off.
    }
}
