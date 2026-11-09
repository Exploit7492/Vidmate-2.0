package com.cobalt.android

import com.cobalt.android.link.ResolveFailureAdvisor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the resolve-failure guidance (E5). */
class ResolveFailureAdvisorTest {

    @Test fun geoErrorSuggestsVpn() {
        val out = ResolveFailureAdvisor.advise("Error: this video is not available in your country")
        assertTrue(out.contains("region-locked"))
        assertTrue(out.contains("VPN"))
    }

    @Test fun forbiddenSuggestsVpn() {
        val out = ResolveFailureAdvisor.advise("The cobalt instance rejected this link (HTTP 403).")
        assertTrue(out.contains("VPN"))
    }

    @Test fun rateLimitSuggestsRetry() {
        val out = ResolveFailureAdvisor.advise("Rate limited by the cobalt instance.")
        assertTrue(out.contains("busy"))
        assertTrue(out.contains("retry", ignoreCase = true))
    }

    @Test fun unrelatedErrorPassesThroughUnchanged() {
        val msg = "The cobalt instance returned no downloadable formats for this link."
        assertEquals(msg, ResolveFailureAdvisor.advise(msg))
    }
}
