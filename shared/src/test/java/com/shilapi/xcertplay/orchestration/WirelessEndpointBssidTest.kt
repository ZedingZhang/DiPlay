package com.shilapi.xcertplay.orchestration

import com.shilapi.xcertplay.iap2.wire.Iap2ParameterList
import com.shilapi.xcertplay.network.WirelessHotspotBackend
import com.shilapi.xcertplay.network.WirelessHotspotInfo
import com.shilapi.xcertplay.transport.Iap2WirelessControlClient
import com.shilapi.xcertplay.transport.Iap2WirelessSecurity
import java.net.InetAddress
import org.junit.Assert.*
import org.junit.Test

class WirelessEndpointBssidTest {
    @Test fun knownP2pApAddressReachesWifiConfigurationAlongsideActualCredentials() {
        val endpoint = endpoint("12:22:33:44:55:66")
        val body = Iap2ParameterList.parse(Iap2WirelessControlClient.accessoryWiFiConfiguration(endpoint).payload).asList()
        assertArrayEquals(byteArrayOf(0x12, 0x22, 0x33, 0x44, 0x55, 0x66), body.single { it.id == 0 }.payload)
        assertEquals("DIRECT-dp-test\u0000", body.single { it.id == 1 }.payload.decodeToString())
        assertEquals("secret123\u0000", body.single { it.id == 2 }.payload.decodeToString())
        assertArrayEquals(byteArrayOf(2), body.single { it.id == 3 }.payload)
        assertArrayEquals(byteArrayOf(6), body.single { it.id == 4 }.payload)
        assertEquals("24:22:33:44:55:66", endpoint.deviceIdentifier)
    }

    @Test fun missingOrInvalidApAddressNeverFallsBackToSavedAirPlayIdentity() {
        for (mac in listOf(null, "02:00:00:00:00:00", "00:00:00:00:00:00",
            "ff:ff:ff:ff:ff:ff", "13:22:33:44:55:66", "12:22:33:44:55", "12:22:33:44:55:+1")) {
            val frame = Iap2WirelessControlClient.accessoryWiFiConfiguration(endpoint(mac))
            assertFalse(Iap2ParameterList.parse(frame.payload).asList().any { it.id == 0 })
        }
    }

    @Test fun provenManualHotspotAndLocalOnlyHotspotPayloadsRemainSsidOnly() {
        for (backend in listOf(WirelessHotspotBackend.MANUAL_HOTSPOT, WirelessHotspotBackend.LOCAL_ONLY_HOTSPOT)) {
            assertNull(endpoint("12:22:33:44:55:66", backend).bssid)
        }
    }

    private fun endpoint(bssid: String?, backend: WirelessHotspotBackend = WirelessHotspotBackend.WIFI_P2P) =
        wirelessCarPlayEndpoint(WirelessHotspotInfo("DIRECT-dp-test", "secret123", Iap2WirelessSecurity.WPA_WPA2,
            6, 2437, bssid, "p2p0", InetAddress.getByName("192.168.49.1"), "2.4 GHz", backend),
            "192.168.49.1", 7000, "24:22:33:44:55:66", "aabbcc", "1.0")
}
