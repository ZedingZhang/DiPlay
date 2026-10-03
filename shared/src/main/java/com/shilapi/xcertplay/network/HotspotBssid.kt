package com.shilapi.xcertplay.network

/** Only a real AP MAC may be sent as BSSID; a saved AirPlay ID is not a Wi-Fi address. */
internal fun hotspotBssidBytes(value: String?): ByteArray? {
    val parts = value?.split(":")?.takeIf { it.size == 6 } ?: return null
    val bytes = parts.map { part ->
        if (!part.matches(Regex("[0-9a-fA-F]{2}"))) return null
        part.toInt(16)
    }
    if ((bytes[0] and 1) != 0 || bytes.all { it == 0 } ||
        bytes == listOf(2, 0, 0, 0, 0, 0)) return null
    return bytes.map(Int::toByte).toByteArray()
}
