package com.yenyuyyy.airshare.server

import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

class ServerEndpoint internal constructor(
    val port: Int,
    val certificate: X509Certificate,
    val certificateSha256: String,
    private val accessToken: String,
    val bindAddress: InetAddress?,
    private val certifiedIpAddresses: Set<String>,
) {
    val boundSessionUrl: URI?
        get() = bindAddress
            ?.takeUnless { it.isAnyLocalAddress }
            ?.let(::sessionUrl)

    fun sessionUrl(advertisedAddress: InetAddress): URI {
        val host = requireNotNull(advertisedAddress.hostAddress).substringBefore('%')
        require(host in certifiedIpAddresses) {
            "The advertised address is not present in the ephemeral certificate"
        }
        val authorityHost = if (':' in host) "[$host]" else host
        val encodedToken = URLEncoder.encode(accessToken, Charsets.UTF_8.name()).replace("+", "%20")
        return URI.create("https://$authorityHost:$port/?token=$encodedToken")
    }

    override fun toString(): String =
        "ServerEndpoint(port=$port, certificateSha256=$certificateSha256, bindAddress=$bindAddress)"
}

class AirShareTlsServer(
    catalog: MediaCatalog,
    private val accessToken: String,
    strings: WebStrings = EnglishWebStrings,
    private val port: Int = DEFAULT_PORT,
    private val bindAddress: InetAddress? = null,
    private val maxHeaderBytes: Int = DEFAULT_MAX_HEADER_BYTES,
    private val socketReadTimeoutMillis: Int = DEFAULT_SOCKET_TIMEOUT_MILLIS,
    private val maxHeaderReadDurationMillis: Int = DEFAULT_MAX_HEADER_READ_DURATION_MILLIS,
    private val maxConcurrentClients: Int = DEFAULT_MAX_CONCURRENT_CLIENTS,
) : Closeable {
    private val handler = MediaHttpHandler(catalog, accessToken, strings)
    private val activeSockets = ConcurrentHashMap.newKeySet<SSLSocket>()
    @Volatile private var serverSocket: SSLServerSocket? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile private var workers: ExecutorService? = null

    init {
        require(port in 0..65_535) { "port must be between 0 and 65535" }
        require(maxHeaderBytes >= 128) { "maxHeaderBytes must be at least 128" }
        require(socketReadTimeoutMillis > 0) { "socketReadTimeoutMillis must be positive" }
        require(maxHeaderReadDurationMillis > 0) { "maxHeaderReadDurationMillis must be positive" }
        require(maxConcurrentClients > 0) { "maxConcurrentClients must be positive" }
    }

    @Synchronized
    fun start(): ServerEndpoint {
        check(serverSocket == null) { "Server is already running" }
        val tls = EphemeralTlsFactory.create(bindAddress)
        val socket = tls.context.serverSocketFactory.createServerSocket() as SSLServerSocket
        try {
            socket.reuseAddress = true
            socket.needClientAuth = false
            socket.enabledProtocols = socket.supportedProtocols
                .filter { it == "TLSv1.3" || it == "TLSv1.2" }
                .toTypedArray()
            socket.bind(InetSocketAddress(bindAddress, port))
            val executor = ThreadPoolExecutor(
                maxConcurrentClients,
                maxConcurrentClients,
                0L,
                TimeUnit.MILLISECONDS,
                ArrayBlockingQueue<Runnable>(maxConcurrentClients),
                { task -> Thread(task, "AirShare-TLS-worker").apply { isDaemon = true } },
                ThreadPoolExecutor.AbortPolicy(),
            )
            serverSocket = socket
            workers = executor
            acceptThread = Thread({ acceptConnections(socket, executor) }, "AirShare-TLS-accept").apply {
                isDaemon = true
                start()
            }
            return ServerEndpoint(
                port = socket.localPort,
                certificate = tls.certificate,
                certificateSha256 = sha256Fingerprint(tls.certificate),
                accessToken = accessToken,
                bindAddress = bindAddress,
                certifiedIpAddresses = tls.certifiedIpAddresses,
            )
        } catch (error: Exception) {
            runCatching { socket.close() }
            workers?.shutdownNow()
            workers = null
            serverSocket = null
            acceptThread = null
            throw error
        }
    }

    private fun acceptConnections(socket: SSLServerSocket, executor: ExecutorService) {
        while (!socket.isClosed) {
            try {
                val client = socket.accept() as SSLSocket
                activeSockets += client
                try {
                    executor.execute { handleClient(client) }
                } catch (_: RejectedExecutionException) {
                    activeSockets -= client
                    runCatching { client.close() }
                }
            } catch (_: SocketException) {
                if (!socket.isClosed) continue
            } catch (_: SSLException) {
                // A failed TLS handshake is isolated to its connection.
            } catch (_: IOException) {
                if (socket.isClosed) break
            }
        }
    }

    private fun handleClient(socket: SSLSocket) {
        try {
            socket.use { client ->
                client.soTimeout = socketReadTimeoutMillis
                client.startHandshake()
                val response = when (
                    val result = BoundedHttpRequestReader(
                        maxHeaderBytes = maxHeaderBytes,
                        maxReadDurationMillis = maxHeaderReadDurationMillis,
                    ).read(client.inputStream)
                ) {
                    is RequestReadResult.Success -> runCatching { handler.handle(result.request) }
                        .getOrElse { internalServerError() }
                    is RequestReadResult.Failure -> textError(result.statusCode, result.reasonPhrase, result.message)
                    RequestReadResult.Closed -> return
                }
                HttpResponseWriter.write(response, client.outputStream)
            }
        } catch (_: Exception) {
            // The peer may disconnect at any point; closing the socket is sufficient.
        } finally {
            activeSockets -= socket
        }
    }

    @Synchronized
    fun stop() {
        val socket = serverSocket ?: return
        serverSocket = null
        socket.close()
        activeSockets.toList().forEach { runCatching { it.close() } }
        activeSockets.clear()
        workers?.shutdownNow()
        workers = null
        acceptThread?.join(1_000)
        acceptThread = null
    }

    override fun close() = stop()

    private fun textError(statusCode: Int, reasonPhrase: String, message: String): HttpResponse {
        val bytes = message.toByteArray(Charsets.UTF_8)
        return HttpResponse(
            statusCode,
            reasonPhrase,
            mapOf(
                "Content-Type" to "text/plain; charset=utf-8",
                "Content-Length" to bytes.size.toString(),
            ),
            bodyWriter = { it.write(bytes) },
        )
    }

    private fun internalServerError(): HttpResponse =
        textError(500, "Internal Server Error", "Internal Server Error")

    private fun sha256Fingerprint(certificate: X509Certificate): String =
        MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString(":") { "%02X".format(it.toInt() and 0xff) }

    companion object {
        const val DEFAULT_PORT = 8443
        const val DEFAULT_MAX_HEADER_BYTES = 16 * 1024
        const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 10_000
        const val DEFAULT_MAX_HEADER_READ_DURATION_MILLIS = 10_000
        const val DEFAULT_MAX_CONCURRENT_CLIENTS = 8

        fun generateAccessToken(): String {
            val bytes = ByteArray(24).also(SecureRandom()::nextBytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
