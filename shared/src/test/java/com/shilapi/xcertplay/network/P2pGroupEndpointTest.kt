package com.shilapi.xcertplay.network

import java.net.Inet6Address
import java.net.InetAddress
import org.junit.Assert.*
import org.junit.Test

class P2pGroupEndpointTest {
    @Test fun legacyClientUsesFrameworkDhcpAddressEvenWithIpv6AlreadyPresent() {
        assertEquals(ip("192.168.49.1"), choose(ip("192.168.49.1"),
            listOf(ip("fe80::1234"), ip("192.168.1.1"))))
    }

    @Test fun interfaceIpv4IsFallbackForInvalidFrameworkAddresses() {
        for (invalid in listOf(null, ip("0.0.0.0"), ip("127.0.0.1"),
            ip("169.254.1.1"), ip("224.0.0.1"))) {
            assertEquals(ip("192.168.49.1"), choose(invalid,
                listOf(ip("fe80::1234"), ip("192.168.49.1"))))
        }
    }

    @Test fun ipv6OnlyGroupRetainsItsOwnInterfaceScope() {
        val wrongScope = Inet6Address.getByAddress(null, ip("fe80::1234").address, 3)
        assertEquals(7, (choose(null, listOf(wrongScope)) as Inet6Address).scopeId)
        assertNull(p2pGroupHostAddress(true, true, null, listOf(wrongScope), 0))
        assertNull(choose(null, listOf(ip("::1"), ip("2001:db8::1"))))
    }

    @Test fun neverAdvertisesAnUnformedGroupOrThePeersAddress() {
        assertNull(p2pGroupHostAddress(false, true, ip("192.168.49.1"), listOf(ip("fe80::1")), 7))
        assertNull(p2pGroupHostAddress(true, false, ip("192.168.49.1"), listOf(ip("fe80::1")), 7))
    }

    @Test fun hiddenAndroid11MacUsesOnlyUnambiguousEui64OfTheGroupInterface() {
        val addresses = listOf(ip("fe80::1022:33ff:fe44:5566"))
        assertEquals("12:22:33:44:55:66", p2pGroupBssid("02:00:00:00:00:00", addresses,
            "02:00:00:00:00:00"))
        assertEquals("24:22:33:44:55:66", p2pGroupBssid("24:22:33:44:55:66", addresses, null))
        assertNull(p2pGroupBssid(null, listOf(ip("fe80::1234")), "02:00:00:00:00:00"))
        assertNull(p2pGroupBssid(null, addresses + ip("fe80::2222:33ff:fe44:5566"), null))
        for (invalid in listOf("00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "01:22:33:44:55:66", "invalid")) {
            assertNull(p2pGroupBssid(invalid, emptyList(), invalid))
        }
    }

    private fun choose(owner: InetAddress?, addresses: List<InetAddress>) =
        p2pGroupHostAddress(true, true, owner, addresses, 7)
    private fun ip(value: String) = InetAddress.getByName(value)
}
