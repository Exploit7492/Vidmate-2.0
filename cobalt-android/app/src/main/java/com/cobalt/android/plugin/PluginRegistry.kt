package com.cobalt.android.plugin

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The set of known add-on modules, loaded from the bundled manifest
 * (`assets/plugins/index.json`) plus any descriptors the user has installed.
 *
 * Mirrors VidMate's module list: modules can be listed and toggled, and a
 * required module cannot be disabled. Parsing is pure (a JSON string in, a list
 * of descriptors out) so it is testable without a device.
 */
class PluginRegistry(private val context: Context) {

    private val descriptors = LinkedHashMap<String, PluginDescriptor>()
    private val disabled = mutableSetOf<String>()
    private val loaded = LinkedHashMap<String, PluginHost>()

    fun load() {
        descriptors.clear()
        for (descriptor in parse(readBundledManifest())) {
            descriptors[descriptor.id] = descriptor
        }
    }

    fun all(): List<PluginDescriptor> = descriptors.values.toList()

    fun enabled(): List<PluginDescriptor> =
        descriptors.values.filter { it.id !in disabled }

    /** A required module cannot be turned off; returns false when refused. */
    fun setEnabled(id: String, on: Boolean): Boolean {
        val descriptor = descriptors[id] ?: return false
        if (!on && descriptor.required) return false
        if (on) disabled.remove(id) else disabled.add(id)
        return true
    }

    fun isEnabled(id: String) = id !in disabled

    fun register(id: String, host: PluginHost) {
        loaded[id] = host
        host.onLoad(context, this)
    }

    fun unregister(id: String) {
        loaded.remove(id)?.onUnload()
    }

    private fun readBundledManifest(): String =
        runCatching {
            context.assets.open(MANIFEST_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("[]")

    companion object {
        const val MANIFEST_ASSET = "plugins/index.json"

        /** Parses a JSON array of descriptors, skipping entries with no id/entryPoint. */
        fun parse(json: String): List<PluginDescriptor> = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id").trim()
                val entry = o.optString("entryPoint").trim()
                if (id.isBlank() || entry.isBlank()) return@mapNotNull null
                PluginDescriptor(
                    id = id,
                    name = o.optString("name", id),
                    versionName = o.optString("versionName"),
                    entryPoint = entry,
                    codePath = o.optString("codePath"),
                    sha256 = o.optString("sha256"),
                    required = o.optBoolean("required", false)
                )
            }
        }.getOrDefault(emptyList())
    }
}
