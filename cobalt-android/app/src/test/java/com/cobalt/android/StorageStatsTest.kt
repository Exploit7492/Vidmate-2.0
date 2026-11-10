package com.cobalt.android

import com.cobalt.android.util.StorageStats
import org.junit.Assert.assertEquals
import org.junit.Test

/** JVM tests for the shared size formatter (§5). */
class StorageStatsTest {

    @Test fun bytes_below_a_kilobyte() {
        assertEquals("0 B", StorageStats.formatBytes(0))
        assertEquals("512 B", StorageStats.formatBytes(512))
    }

    @Test fun kilobytes_and_megabytes() {
        assertEquals("1.0 KB", StorageStats.formatBytes(1024))
        assertEquals("1.0 MB", StorageStats.formatBytes(1024L * 1024))
    }

    @Test fun gigabytes() {
        assertEquals("1.0 GB", StorageStats.formatBytes(1024L * 1024 * 1024))
    }
}
