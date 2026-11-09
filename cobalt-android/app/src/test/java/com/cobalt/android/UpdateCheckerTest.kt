package com.cobalt.android

import com.cobalt.android.update.UpdateChecker
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** JVM tests for the self-update decision (I2). */
class UpdateCheckerTest {

    private fun config(versionCode: Int, apkUrl: String, versionName: String = "2.0") =
        JSONObject(
            """{"update":{"versionCode":$versionCode,"versionName":"$versionName","apkUrl":"$apkUrl"}}"""
        )

    @Test fun newerVersionWithApkUrlIsAnUpdate() {
        val update = UpdateChecker.parseUpdate(config(2, "https://example.com/app.apk"), 1)
        assertEquals(2, update!!.versionCode)
        assertEquals("2.0", update.versionName)
        assertEquals("https://example.com/app.apk", update.apkUrl)
    }

    @Test fun sameOrOlderVersionIsNotAnUpdate() {
        assertNull(UpdateChecker.parseUpdate(config(1, "https://example.com/app.apk"), 1))
        assertNull(UpdateChecker.parseUpdate(config(0, "https://example.com/app.apk"), 1))
    }

    @Test fun versionBumpWithoutDownloadUrlIsNotActionable() {
        assertNull(UpdateChecker.parseUpdate(config(5, ""), 1))
        assertNull(UpdateChecker.parseUpdate(config(5, "ftp://nope"), 1))
    }

    @Test fun missingUpdateBlockMeansNoUpdate() {
        assertNull(UpdateChecker.parseUpdate(JSONObject("""{"cobaltInstanceUrl":"x"}"""), 1))
    }
}
