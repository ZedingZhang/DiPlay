package com.shilapi.xcertplay.network

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/** Checks our already-bound listener, without contacting another device or activating CarPlay. */
internal object WirelessListenerProbe {
    data class Result(val outcome: String, val stage: String, val status: Int?,
        val error: String?, val elapsedMs: Long) {
        fun diagnosticSummary() = "Wireless listener self-check outcome=$outcome stage=$stage " +
            "status=${status ?: "none"} error=${error ?: "none"} elapsedMs=$elapsedMs"
    }

    fun check(address: InetAddress, port: Int, timeoutMillis: Int = 2_000): Result {
        require(timeoutMillis in 1..5_000)
        val started = System.nanoTime()
        val deadline = started + TimeUnit.MILLISECONDS.toNanos(timeoutMillis.toLong())
        var stage = "CONNECT"
        var status: Int? = null
        fun result(outcome: String, error: String? = null) = Result(outcome, stage, status, error,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started))
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), timeoutMillis)
                stage = "READ"
                socket.getOutputStream().apply {
                    write("GET /diplay/network-check HTTP/1.1\r\nHost: diplay-check\r\nConnection: close\r\n\r\n"
                        .toByteArray(Charsets.US_ASCII))
                    flush()
                }
                val input = socket.getInputStream()
                fun readByte(): Int {
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0) throw SocketTimeoutException("Listener check deadline")
                    socket.soTimeout = TimeUnit.NANOSECONDS.toMillis(remaining).coerceAtLeast(1).toInt()
                    return input.read().also { if (it < 0) throw EOFException("Listener check ended") }
                }
                val header = ByteArrayOutputStream()
                var tail = 0
                while (header.size() < 4_096 && tail != 0x0d0a0d0a) {
                    val byte = readByte()
                    header.write(byte)
                    tail = (tail shl 8) or byte
                }
                if (tail != 0x0d0a0d0a) return result("BAD_RESPONSE")
                val text = header.toString(Charsets.US_ASCII.name())
                status = Regex("^HTTP/1\\.[01] (\\d{3})").find(text)?.groupValues?.get(1)?.toInt()
                val length = Regex("(?im)^Content-Length: (\\d+)\\r?$").find(text)
                    ?.groupValues?.get(1)?.toIntOrNull()
                if (status != 200 || length == null || length !in 1..4_096) return result("BAD_RESPONSE")
                val body = ByteArray(length) { readByte().toByte() }.toString(Charsets.UTF_8)
                stage = "RESPONSE"
                result(if (body.startsWith("DiPlay network check OK\n")) "OK" else "BAD_RESPONSE")
            }
        } catch (failure: IOException) {
            result("IO_FAILURE", failure.javaClass.simpleName)
        } catch (failure: RuntimeException) {
            result("UNAVAILABLE", failure.javaClass.simpleName)
        }
    }
}
