package com.shilapi.xcertplay.orchestration

/** Live connection help for the local UI. Never put these credentials in status/log messages. */
class WirelessJoinDetails(val ssid: String, val passphrase: String, val checkUrl: String?) {
    override fun toString(): String = "WirelessJoinDetails(<redacted>)"
}
