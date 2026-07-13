package com.yenyuyyy.airshare.server

import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URI
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ServerEndpoint internal constructor(
    val port: Int,
    private val accessToken: String,
    val bindAddress: InetAddress?,
) {
    val boundSessionUrl: URI?
        get() = bindAddress
            ?.takeUnless { it.isAnyLocalAddress }
            ?.let(::sessionUrl)

    fun sessionUrl(advertisedAddress: InetAddress): URI {
        val host = requireNotNull(advertisedAddress.hostAddress).substringBefore('%')
        val authorityHost = if (':' in host) "[$host]" else host
        val encodedToken = URLEncoder.encode(accessToken, Charsets.UTF_8.name()).replace("+", "%20")
        return URI.create("http://$authorityHost:$port/?token=$encodedToken")
    }

    override fun toString(): String =
        "ServerEndpoint(port=$port, bindAddress=$bindAddress)"
}

class AirShareHttpServer(
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
    private val activeSockets = ConcurrentHashMap.newKeySet<Socket>()
    @Volatile private var serverSocket: ServerSocket? = null
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
        val socket = ServerSocket()
        try {
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(bindAddress, port))
            val executor = ThreadPoolExecutor(
                maxConcurrentClients,
                maxConcurrentClients,
                0L,
                TimeUnit.MILLISECONDS,
                ArrayBlockingQueue<Runnable>(maxConcurrentClients),
                { task -> Thread(task, "AirShare-HTTP-worker").apply { isDaemon = true } },
                ThreadPoolExecutor.AbortPolicy(),
            )
            serverSocket = socket
            workers = executor
            acceptThread = Thread({ acceptConnections(socket, executor) }, "AirShare-HTTP-accept").apply {
                isDaemon = true
                start()
            }
            return ServerEndpoint(
                port = socket.localPort,
                accessToken = accessToken,
                bindAddress = bindAddress,
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

    private fun acceptConnections(socket: ServerSocket, executor: ExecutorService) {
        while (!socket.isClosed) {
            try {
                val client = socket.accept()
                activeSockets += client
                try {
                    executor.execute { handleClient(client) }
                } catch (_: RejectedExecutionException) {
                    activeSockets -= client
                    runCatching { client.close() }
                }
            } catch (_: SocketException) {
                if (socket.isClosed) break
            } catch (_: IOException) {
                if (socket.isClosed) break
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { client ->
                client.soTimeout = socketReadTimeoutMillis
                val result = BoundedHttpRequestReader(
                    maxHeaderBytes = maxHeaderBytes,
                    maxReadDurationMillis = maxHeaderReadDurationMillis,
                ).read(client.inputStream)
                val response = when (result) {
                    is RequestReadResult.Success -> runCatching { handler.handle(result.request) }
                        .getOrElse { internalServerError() }
                    is RequestReadResult.Failure -> textError(result.statusCode, result.reasonPhrase, result.message)
                    RequestReadResult.Closed -> return
                }
                HttpResponseWriter.write(response, client.outputStream)
                if (result is RequestReadResult.Failure) {
                    drainBufferedInput(client)
                }
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

    private fun drainBufferedInput(client: Socket) {
        val input = client.inputStream
        val buffer = ByteArray(4 * 1024)
        var remaining = 64 * 1024
        while (remaining > 0 && input.available() > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (read <= 0) break
            remaining -= read
        }
    }

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

    companion object {
        const val DEFAULT_PORT = 8080
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
