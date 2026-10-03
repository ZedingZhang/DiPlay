package com.shilapi.xcertplay.network

import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress

/** A legacy Wi-Fi client joins the GO's AP; use its DHCP address before link-local IPv6. */
internal fun p2pGroupHostAddress(
    groupFormed: Boolean,
    isGroupOwner: Boolean,
    ownerAddress: InetAddress?,
    interfaceAddresses: List<InetAddress>,
    interfaceIndex: Int,
): InetAddress? {
    if (!groupFormed || !isGroupOwner) return null
    fun usableIpv4(address: InetAddress?) = address is Inet4Address &&
        !address.isAnyLocalAddress && !address.isLoopbackAddress &&
        !address.isLinkLocalAddress && !address.isMulticastAddress
    if (usableIpv4(ownerAddress)) return ownerAddress
    interfaceAddresses.firstOrNull(::usableIpv4)?.let { return it }
    // Retain IPv6-only firmware support, scoped to the actual GO interface.
    return wirelessHostAddress(interfaceAddresses, interfaceIndex)
}

internal fun p2pGroupBssid(
    hardwareAddress: String?,
    interfaceAddresses: List<InetAddress>,
    ownerDeviceAddress: String?,
): String? {
    fun usableMac(value: String?): Boolean {
        val bytes = value?.split(":")?.takeIf { it.size == 6 }
            ?.map { it.takeIf { part -> part.length == 2 }?.toIntOrNull(16) ?: return false }
            ?: return false
        return bytes.all { it in 0..255 } && (bytes[0] and 1) == 0 &&
            bytes.any { it != 0 } && bytes != listOf(2, 0, 0, 0, 0, 0)
    }
    return hardwareAddress?.takeIf(::usableMac)
        ?: HotspotInterfaceBssid.fromAddresses(interfaceAddresses.map { it.address })
        ?: ownerDeviceAddress?.takeIf(::usableMac)
}

/** Disabled P2P cannot be repaired by channel retries or by removing another app's group. */
class P2pUnavailableException : IOException(
    "Wi-Fi Direct is disabled by Android. Turn off this device's personal hotspot, " +
        "keep Wi-Fi on, then reconnect. If needed, turn Wi-Fi off and on in system settings.",
)
