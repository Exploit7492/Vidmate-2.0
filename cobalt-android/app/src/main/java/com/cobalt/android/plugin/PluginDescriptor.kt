package com.cobalt.android.plugin

/**
 * Describes one add-on module. VidMate ships optional "modules" (its own
 * feature packs) described by a small manifest; this is the same idea: a
 * descriptor names the module, where its code lives, and what it is allowed to
 * do. Descriptors are plain data (no Android types) so the registry's parsing
 * and validation are unit-testable.
 */
data class PluginDescriptor(
    /** Stable id, e.g. "com.cobalt.plugin.downloader". */
    val id: String,
    /** Human-readable name shown in the Plugins screen. */
    val name: String,
    val versionName: String,
    /** Fully-qualified class implementing [PluginHost]. */
    val entryPoint: String,
    /**
     * File name (relative to the plugin dir) of the module's dex/jar. Empty for
     * a built-in module whose class already ships in the APK.
     */
    val codePath: String = "",
    /** Expected SHA-256 of [codePath]; empty disables the integrity check. */
    val sha256: String = "",
    /** True when the module must be loaded for the app to behave normally. */
    val required: Boolean = false
)
