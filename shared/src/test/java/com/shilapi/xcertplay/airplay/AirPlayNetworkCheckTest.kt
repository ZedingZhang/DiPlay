package com.shilapi.xcertplay.airplay

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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AirPlayNetworkCheckTest {
    @Test fun safariCheckUsesAirPlaySocketAndDoesNotActivateCarPlay() {
        val activated = AtomicInteger()
        val mediaClosed = AtomicInteger()
        val ended = CountDownLatch(1)
        val diagnostics = CopyOnWriteArrayList<String>()
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { listener ->
            Socket(listener.inetAddress, listener.localPort).use { client ->
                client.soTimeout = 3_000
                val session = AirPlaySession(listener.accept(),
                    AirPlayConfig(deviceName = "test", deviceId = "02:00:00:00:00:02",
                        btMac = "02:00:00:00:00:01", sourceVersion = "1",
                        main = AirPlayDisplayConfig(widthPixels = 800, heightPixels = 480)),
                    AirPlayIdentity.generate(), PairingStore(), null,
                    object : AirPlaySessionListener {
                        override fun onSessionActive(session: AirPlaySession) { activated.incrementAndGet() }
                        override fun onSessionEnded(session: AirPlaySession) { ended.countDown() }
                        override fun onDebugLog(message: String) { diagnostics += message }
                    }, object : AirPlayMediaHandler {
                        override fun onSessionClosed(session: AirPlaySession) { mediaClosed.incrementAndGet() }
                    })
                try {
                    session.start()
                    client.getOutputStream().write(
                        "GET /diplay/network-check HTTP/1.1\r\nHost: test\r\n\r\n".toByteArray())
                    client.getOutputStream().flush()
                    val response = client.getInputStream().readBytes().toString(Charsets.UTF_8)
                    assertTrue(response.startsWith("HTTP/1.1 200 OK\r\n"))
                    assertTrue(response.contains("Connection: close\r\n"))
                    assertTrue(response.contains("DiPlay network check OK\n"))
                    assertTrue(ended.await(3, TimeUnit.SECONDS))
                    assertEquals(0, activated.get())
                    assertEquals(0, mediaClosed.get())
                    assertTrue(session.activeStreams.isEmpty())
                    assertNull(session.cipher)
                    assertTrue(diagnostics.contains("DiPlay network check reached"))
                } finally { session.close() }
            }
        }
    }
}
