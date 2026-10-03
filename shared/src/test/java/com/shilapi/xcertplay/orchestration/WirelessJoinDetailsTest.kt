package com.shilapi.xcertplay.orchestration

import org.junit.Assert.assertEquals
import org.junit.Test

class WirelessJoinDetailsTest {
    @Test fun connectionHelpCannotExposeCredentialsThroughObjectLogging() {
        val details = WirelessJoinDetails("DIRECT-test-only", "test-password-only",
            "http://192.168.49.1:7000/diplay/network-check")
        assertEquals("WirelessJoinDetails(<redacted>)", "$details")
    }
}
