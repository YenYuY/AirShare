package com.yenyuyyy.airshare.server

import java.io.InputStream
import java.net.InetAddress
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AirShareTlsServerTest {
    @Test
    fun `generated share tokens are high entropy URL-safe values`() {
        val token = AirShareTlsServer.generateAccessToken()

        assertTrue(token.matches(Regex("^[A-Za-z0-9_-]{32}$")))
    }

    @Test
    fun `server exposes the HTTP handler only through its ephemeral TLS certificate`() {
        AirShareTlsServer(
            catalog = EmptyCatalog,
            accessToken = "correct-token",
            strings = SocketTestStrings,
            port = 0,
            bindAddress = InetAddress.getByName("127.0.0.1"),
        ).use { server ->
            val endpoint = server.start()

            val response = sendTlsRequest(
                endpoint,
                "GET /?token=correct-token HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n",
            )

            assertEquals(200, response.statusCode)
            assertTrue(endpoint.port > 0)
            endpoint.certificate.checkValidity()
            assertTrue(endpoint.certificate.serialNumber.bitLength() <= 159)
            assertEquals(endpoint.certificate.subjectX500Principal, endpoint.certificate.issuerX500Principal)
            assertEquals(
                "https://127.0.0.1:${endpoint.port}/?token=correct-token",
                endpoint.boundSessionUrl.toString(),
            )
            assertTrue(
                endpoint.certificate.subjectAlternativeNames
                    .any { alternativeName -> alternativeName[0] == 7 && alternativeName[1] == "127.0.0.1" },
            )
            assertFalse(endpoint.toString().contains("correct-token"))
            assertThrows(IllegalArgumentException::class.java) {
                endpoint.sessionUrl(InetAddress.getByName("192.0.2.10"))
            }
        }
    }

    @Test
    fun `oversized request headers are rejected at the wire boundary`() {
        AirShareTlsServer(
            catalog = EmptyCatalog,
            accessToken = "correct-token",
            strings = SocketTestStrings,
            port = 0,
            maxHeaderBytes = 256,
        ).use { server ->
            val endpoint = server.start()
            val request = buildString {
                append("GET /?token=correct-token HTTP/1.1\r\nHost: localhost\r\nX-Fill: ")
                append("a".repeat(300))
                append("\r\n\r\n")
            }

            val response = sendTlsRequest(endpoint, request)

            assertEquals(431, response.statusCode)
            assertEquals("nosniff", response.headers["x-content-type-options"])
        }
    }

    @Test
    fun `restarting creates a new ephemeral certificate`() {
        AirShareTlsServer(
            catalog = EmptyCatalog,
            accessToken = "correct-token",
            strings = SocketTestStrings,
            port = 0,
        ).use { server ->
            val first = server.start()
            server.stop()
            val second = server.start()

            assertTrue(first.certificateSha256 != second.certificateSha256)
        }
    }

    @Test
    fun `malformed content length is rejected instead of entering request routing`() {
        AirShareTlsServer(
            catalog = EmptyCatalog,
            accessToken = "correct-token",
            strings = SocketTestStrings,
            port = 0,
        ).use { server ->
            val endpoint = server.start()

            val response = sendTlsRequest(
                endpoint,
                "GET /?token=correct-token HTTP/1.1\r\nHost: localhost\r\nContent-Length: nope\r\n\r\n",
            )

            assertEquals(400, response.statusCode)
        }
    }

    @Test
    fun `session URL safely encodes the token and is immediately usable`() {
        AirShareTlsServer(
            catalog = EmptyCatalog,
            accessToken = "t +/&",
            strings = SocketTestStrings,
            port = 0,
            bindAddress = InetAddress.getByName("127.0.0.1"),
        ).use { server ->
            val endpoint = server.start()
            val url = endpoint.boundSessionUrl!!

            val response = sendTlsRequest(
                endpoint,
                "GET ${url.rawPath}?${url.rawQuery} HTTP/1.1\r\nHost: localhost\r\n\r\n",
            )

            assertEquals("token=t%20%2B%2F%26", url.rawQuery)
            assertEquals(200, response.statusCode)
        }
    }

    @Test
    fun `identical duplicate Content-Length zero values are accepted`() {
        AirShareTlsServer(
            catalog = EmptyCatalog,
            accessToken = "correct-token",
            strings = SocketTestStrings,
            port = 0,
        ).use { server ->
            val endpoint = server.start()

            val response = sendTlsRequest(
                endpoint,
                "GET /?token=correct-token HTTP/1.1\r\nHost: localhost\r\nContent-Length: 0\r\nContent-Length: 0\r\n\r\n",
            )

            assertEquals(200, response.statusCode)
        }
    }

    @Test
    fun `HTTP 1_1 requires exactly one Host header`() {
        AirShareTlsServer(
            catalog = EmptyCatalog,
            accessToken = "correct-token",
            strings = SocketTestStrings,
            port = 0,
        ).use { server ->
            val endpoint = server.start()

            val missing = sendTlsRequest(
                endpoint,
                "GET /?token=correct-token HTTP/1.1\r\n\r\n",
            )
            val duplicated = sendTlsRequest(
                endpoint,
                "GET /?token=correct-token HTTP/1.1\r\nHost: localhost\r\nHost: localhost\r\n\r\n",
            )

            assertEquals(400, missing.statusCode)
            assertEquals(400, duplicated.statusCode)
        }
    }

    @Test
    fun `header parsing has an absolute deadline even when bytes keep arriving`() {
        AirShareTlsServer(
            catalog = EmptyCatalog,
            accessToken = "correct-token",
            strings = SocketTestStrings,
            port = 0,
            socketReadTimeoutMillis = 1_000,
            maxHeaderReadDurationMillis = 50,
        ).use { server ->
            val endpoint = server.start()
            val socket = trustedSocket(endpoint)
            val response = socket.use {
                it.soTimeout = 2_000
                it.startHandshake()
                it.outputStream.write('G'.code)
                it.outputStream.flush()
                Thread.sleep(100)
                it.outputStream.write('E'.code)
                it.outputStream.flush()
                parseRawResponse(it.inputStream.readBytes())
            }

            assertEquals(408, response.statusCode)
        }
    }

    private fun sendTlsRequest(endpoint: ServerEndpoint, rawRequest: String): RawResponse {
        val socket = trustedSocket(endpoint)
        return socket.use {
            it.soTimeout = 5_000
            it.startHandshake()
            it.outputStream.write(rawRequest.toByteArray(Charsets.ISO_8859_1))
            it.outputStream.flush()
            parseRawResponse(it.inputStream.readBytes())
        }
    }

    private fun trustedSocket(endpoint: ServerEndpoint): SSLSocket {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setCertificateEntry("airshare", endpoint.certificate)
        }
        val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }.trustManagers
        val context = SSLContext.getInstance("TLS").apply { init(null, trustManagers, null) }
        return context.socketFactory.createSocket("127.0.0.1", endpoint.port) as SSLSocket
    }

    private fun parseRawResponse(bytes: ByteArray): RawResponse {
        val marker = "\r\n\r\n".toByteArray(Charsets.ISO_8859_1)
        val markerIndex = bytes.indexOfSubsequence(marker)
        check(markerIndex >= 0) { "Response did not contain complete headers" }
        val headerText = bytes.copyOfRange(0, markerIndex).toString(Charsets.ISO_8859_1)
        val lines = headerText.split("\r\n")
        val statusCode = lines.first().split(' ')[1].toInt()
        val headers = lines.drop(1).associate { line ->
            val separator = line.indexOf(':')
            line.substring(0, separator).lowercase() to line.substring(separator + 1).trim()
        }
        return RawResponse(statusCode, headers, bytes.copyOfRange(markerIndex + marker.size, bytes.size))
    }
}

private data class RawResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: ByteArray,
)

private fun ByteArray.indexOfSubsequence(needle: ByteArray): Int {
    if (needle.isEmpty()) return 0
    for (start in 0..size - needle.size) {
        var matches = true
        for (offset in needle.indices) {
            if (this[start + offset] != needle[offset]) {
                matches = false
                break
            }
        }
        if (matches) return start
    }
    return -1
}

private object EmptyCatalog : MediaCatalog {
    override fun totalCount(): Int = 0
    override fun list(offset: Int, limit: Int): List<MediaItem> = emptyList()
    override fun find(id: String): MediaItem? = null
    override fun thumbnail(id: String, targetSizePx: Int): ByteArray? = null
    override fun openAsset(id: String): InputStream? = null
}

private object SocketTestStrings : WebStrings {
    override val appName = "AirShare"
    override val previousPage = "Previous"
    override val nextPage = "Next"
    override val downloadHint = "Download the original file."
    override val videoBadge = "VIDEO"
    override val emptyTitle = "No media"
    override val emptyMessage = "There are no photos or videos to share."
    override val unauthorizedTitle = "Link unavailable"
    override val unauthorizedMessage = "Use the complete link shown on the phone."
    override fun pageInfo(page: Int, pageCount: Int): String = "Page $page / $pageCount"
}
