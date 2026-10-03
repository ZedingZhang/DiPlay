package com.shilapi.xcertplay.orchestration

import com.shilapi.xcertplay.network.P2pResetRequiredException
import com.shilapi.xcertplay.network.P2pUnavailableException
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class CarPlayFailureStatusTest {
    @Test fun disabledP2pSurvivesWrappingAndRequestsSettingsInsteadOfGroupReset() {
        val status = carPlayFailureStatus(IOException("hotspot startup failed", P2pUnavailableException()))
        assertTrue(status.wifiSettingsRequired)
        assertFalse(status.wifiResetRequired)
    }

    @Test fun competingGroupRetainsExplicitResetRecovery() {
        val status = carPlayFailureStatus(P2pResetRequiredException())
        assertTrue(status.wifiResetRequired)
        assertFalse(status.wifiSettingsRequired)
    }

    @Test fun transientTransportFailureRetainsAutomaticRecovery() {
        val status = carPlayFailureStatus(IOException("transport closed"))
        assertFalse(status.wifiSettingsRequired)
        assertFalse(status.wifiResetRequired)
        assertEquals("transport closed", status.message)
    }
}
