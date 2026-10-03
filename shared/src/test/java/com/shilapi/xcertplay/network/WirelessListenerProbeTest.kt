package com.shilapi.xcertplay.network

import com.shilapi.xcertplay.airplay.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WirelessListenerProbeTest {
    @Test fun checksActualServiceAndKeepsItReadyForAnotherConnection() {
        val host = InetAddress.getByName("127.0.0.1")
        val preferred = ServerSocket(0, 1, host).use { it.localPort }
        val service = Robolectric.buildService(CarPlayVpnService::class.java).create()
        val events = CopyOnWriteArrayList<String>()
        val activated = AtomicInteger()
        val mediaClosed = AtomicInteger()
        try {
            assertEquals(CarPlayVpnService.AttachResult.Started, service.get().attachWireless(host,
                AirPlayConfig(deviceName = "test", deviceId = "02:00:00:00:00:02",
                    btMac = "02:00:00:00:00:01", sourceVersion = "1", port = preferred,
                    main = AirPlayDisplayConfig(widthPixels = 800, heightPixels = 480)),
                AirPlayIdentity.generate(), PairingStore(), null,
                object : AirPlaySessionListener {
                    override fun onDebugLog(message: String) { events += message }
                    override fun onSessionActive(session: AirPlaySession) { activated.incrementAndGet() }
                }, object : AirPlayMediaHandler {
                    override fun onSessionClosed(session: AirPlaySession) { mediaClosed.incrementAndGet() }
                }))
            val port = service.get().boundPort()!!
            repeat(2) {
                val result = WirelessListenerProbe.check(host, port)
                assertEquals(result.diagnosticSummary(), "OK", result.outcome)
                assertEquals(200, result.status)
            }
            // Safari must receive the body and a clean EOF through the real service, which
            // normally sets SO_LINGER(0) on accepted CarPlay sockets.
            Socket(host, port).use { browser ->
                browser.soTimeout = 2_000
                browser.getOutputStream().write(
                    "GET /diplay/network-check HTTP/1.1\r\nHost: test\r\n\r\n".toByteArray())
                browser.getOutputStream().flush()
                val response = browser.getInputStream().readBytes().toString(Charsets.UTF_8)
                assertTrue(response.contains("DiPlay network check OK\n"))
            }
            assertTrue(service.get().isAttached())
            assertEquals(0, activated.get())
            assertEquals(0, mediaClosed.get())
            assertTrue(events.any { it.startsWith("AirPlay listener starting family=IPv4") })
            assertEquals(3, events.count { it.startsWith("AirPlay listener accepted family=IPv4 localPeer=true localMatchesEndpoint=true") })
            assertEquals(3, events.count { it == "DiPlay network check reached" })
            assertFalse(events.any { it.startsWith("AirPlay listener failed") })
        } finally { service.destroy() }
    }

    @Test fun aSilentAcceptedConnectionTimesOutWithoutWaitingIndefinitely() {
        val accepted = CountDownLatch(1)
        val done = CountDownLatch(1)
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val worker = Thread {
                server.accept().use {
                    accepted.countDown()
                    done.await(2, TimeUnit.SECONDS)
                }
            }.apply { isDaemon = true; start() }
            try {
                val result = WirelessListenerProbe.check(server.inetAddress, server.localPort, 150)
                assertTrue(accepted.await(1, TimeUnit.SECONDS))
                assertEquals("IO_FAILURE", result.outcome)
                assertEquals("READ", result.stage)
                assertEquals("SocketTimeoutException", result.error)
                assertTrue(result.elapsedMs < 1_500)
            } finally { done.countDown(); worker.join(2_000) }
        }
    }

    @Test fun anUnrelatedHttpPageCannotPassAsDiPlay() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val worker = Thread {
                server.accept().use { socket ->
                    socket.soTimeout = 2_000
                    val request = socket.getInputStream().bufferedReader()
                    while (!request.readLine().isNullOrEmpty()) { /* Consume the request before closing. */ }
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nnope".toByteArray())
                    socket.getOutputStream().flush()
                }
            }.apply { isDaemon = true; start() }
            try {
                val result = WirelessListenerProbe.check(server.inetAddress, server.localPort)
                assertEquals("BAD_RESPONSE", result.outcome)
                assertEquals(200, result.status)
            } finally { worker.join(2_000) }
        }
    }
}
