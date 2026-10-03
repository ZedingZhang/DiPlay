package com.shilapi.xcertplay.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Android-visible state only: this cannot inspect an iPhone VPN, proxy or firewall. */
internal object LocalNetworkEnvironment {
    @Suppress("DEPRECATION")
    fun diagnosticSummary(context: Context): String {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val activeVpn = runCatching {
            manager?.let { connectivity ->
                connectivity.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            }
        }.getOrNull()
        val visibleVpn = runCatching {
            manager?.let { connectivity ->
                connectivity.allNetworks.any { network ->
                    connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                }
            }
        }.getOrNull()
        val proxy = runCatching { manager?.let { it.defaultProxy != null } }.getOrNull()
        return "Wireless local network activeVpn=${activeVpn ?: "unknown"} " +
            "visibleVpn=${visibleVpn ?: "unknown"} proxyConfigured=${proxy ?: "unknown"}"
    }
}
