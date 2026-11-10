package com.cobalt.android

import com.cobalt.android.plugin.PluginRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for parsing the bundled module manifest (§11). */
class PluginRegistryTest {

    private val json = """
        [
          {"id":"a","name":"A","versionName":"1","entryPoint":"p.A","required":true},
          {"id":"b","name":"B","versionName":"1","entryPoint":"p.B"},
          {"id":"","name":"bad","entryPoint":"p.X"},
          {"id":"c","name":"no entry"}
        ]
    """.trimIndent()

    @Test fun parses_valid_entries_and_skips_incomplete() {
        val list = PluginRegistry.parse(json)
        assertEquals(listOf("a", "b"), list.map { it.id })
    }

    @Test fun reads_required_flag() {
        val list = PluginRegistry.parse(json)
        assertTrue(list.first { it.id == "a" }.required)
        assertTrue(!list.first { it.id == "b" }.required)
    }

    @Test fun malformed_json_yields_empty() {
        assertTrue(PluginRegistry.parse("not json").isEmpty())
    }

    @Test fun empty_array_yields_empty() {
        assertTrue(PluginRegistry.parse("[]").isEmpty())
    }
}
