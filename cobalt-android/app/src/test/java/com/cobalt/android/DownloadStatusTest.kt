package com.cobalt.android

import com.cobalt.android.download.DownloadRecord
import com.cobalt.android.download.DownloadStatus
import com.cobalt.android.download.StatusConverters
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStatusTest {
    @Test fun defaultStatusIsQueued() = assertEquals(DownloadStatus.QUEUED, DownloadRecord().status)

    @Test fun statusEnumRoundTrip() {
        val c = StatusConverters()
        DownloadStatus.values().forEach { assertEquals(it, c.toStatus(c.fromStatus(it))) }
    }

    // 6 since Phase 20 added CONVERTING and FAILED_NETWORK (was 5 pre-Phase 20).
    @Test fun allStatusesPresent() = assertEquals(6, DownloadStatus.values().size)

    @Test fun recordHasMediaStoreUriField() {
        val r = DownloadRecord(mediaStoreUriString = "content://media/external/downloads/123")
        assertEquals("content://media/external/downloads/123", r.mediaStoreUriString)
    }
}
