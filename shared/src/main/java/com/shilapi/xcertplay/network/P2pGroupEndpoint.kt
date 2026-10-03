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
): String? {
    // WifiP2pGroup.owner.deviceAddress identifies the P2P device, not necessarily its GO AP.
    return hardwareAddress?.takeIf { hotspotBssidBytes(it) != null }
        ?: HotspotInterfaceBssid.fromAddresses(interfaceAddresses.map { it.address })
}

/** Disabled P2P cannot be repaired by channel retries or by removing another app's group. */
class P2pUnavailableException : IOException(
    "Wi-Fi Direct is disabled by Android. Turn off this device's personal hotspot, " +
        "keep Wi-Fi on, then reconnect. If needed, turn Wi-Fi off and on in system settings.",
)
