package com.yenyuyyy.airshare.server

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketTimeoutException

internal sealed interface RequestReadResult {
    data class Success(val request: HttpRequest) : RequestReadResult
    data class Failure(val statusCode: Int, val reasonPhrase: String, val message: String) : RequestReadResult
    data object Closed : RequestReadResult
}

internal class BoundedHttpRequestReader(
    private val maxHeaderBytes: Int,
    private val maxReadDurationMillis: Int,
) {
    init {
        require(maxHeaderBytes >= 128) { "maxHeaderBytes must be at least 128" }
        require(maxReadDurationMillis > 0) { "maxReadDurationMillis must be positive" }
    }

    fun read(input: InputStream): RequestReadResult {
        val bytes = ByteArrayOutputStream(minOf(maxHeaderBytes, 4096))
        var terminatorState = 0
        val startedAtNanos = System.nanoTime()
        val maxReadDurationNanos = maxReadDurationMillis * NANOS_PER_MILLISECOND
        while (true) {
            if (System.nanoTime() - startedAtNanos >= maxReadDurationNanos) return requestTimeout()
            val byte = try {
                input.read()
            } catch (_: SocketTimeoutException) {
                return requestTimeout()
            }
            if (System.nanoTime() - startedAtNanos >= maxReadDurationNanos) return requestTimeout()
            if (byte < 0) {
                return if (bytes.size() == 0) {
                    RequestReadResult.Closed
                } else {
                    badRequest("Incomplete HTTP headers")
                }
            }
            bytes.write(byte)
            if (bytes.size() > maxHeaderBytes) {
                return RequestReadResult.Failure(431, "Request Header Fields Too Large", "Request headers are too large")
            }
            terminatorState = when (terminatorState) {
                0 -> if (byte == CR) 1 else 0
                1 -> if (byte == LF) 2 else if (byte == CR) 1 else 0
                2 -> if (byte == CR) 3 else 0
                3 -> if (byte == LF) 4 else if (byte == CR) 1 else 0
                else -> 4
            }
            if (terminatorState == 4) break
        }

        val headerText = bytes.toByteArray()
            .copyOf(bytes.size() - 4)
            .toString(Charsets.ISO_8859_1)
        val lines = headerText.split("\r\n")
        if (lines.isEmpty()) return badRequest("Missing request line")
        val requestParts = lines.first().split(' ', limit = 3)
        if (
            requestParts.size != 3 || requestParts.any { it.isEmpty() } ||
            !TOKEN.matches(requestParts[0]) || !requestParts[1].startsWith('/') ||
            requestParts[2] !in SUPPORTED_HTTP_VERSIONS
        ) {
            return badRequest("Malformed request line")
        }

        val headers = linkedMapOf<String, String>()
        val headerCounts = mutableMapOf<String, Int>()
        for (line in lines.drop(1)) {
            val separator = line.indexOf(':')
            if (separator <= 0) return badRequest("Malformed header")
            val name = line.substring(0, separator)
            val value = line.substring(separator + 1).trim()
            if (!TOKEN.matches(name) || value.any { it.code < 0x20 && it != '\t' || it.code == 0x7f }) {
                return badRequest("Malformed header")
            }
            val normalizedName = name.lowercase()
            headerCounts[normalizedName] = (headerCounts[normalizedName] ?: 0) + 1
            headers[normalizedName] = headers[normalizedName]?.let { "$it,$value" } ?: value
        }
        val hostCount = headerCounts["host"] ?: 0
        val host = headers["host"]
        if (
            hostCount > 1 ||
            (requestParts[2] == "HTTP/1.1" && hostCount != 1) ||
            (host != null && !VALID_HOST.matches(host))
        ) {
            return badRequest("HTTP/1.1 requires exactly one valid Host header")
        }
        if (headers.containsKey("transfer-encoding")) return badRequest("Request bodies are not supported")
        headers["content-length"]?.let { rawContentLength ->
            val values = rawContentLength.split(',').map { it.trim() }
            val parsed = values.map { it.toLongOrNull() }
            val distinctValues = parsed.filterNotNull().distinct()
            if (parsed.any { it == null || it < 0 } || distinctValues.size != 1) {
                return badRequest("Invalid Content-Length")
            }
            if (distinctValues.single() != 0L) return badRequest("Request bodies are not supported")
        }

        return RequestReadResult.Success(
            HttpRequest(
                method = requestParts[0],
                target = requestParts[1],
                headers = headers,
            ),
        )
    }

    private fun badRequest(message: String) = RequestReadResult.Failure(400, "Bad Request", message)

    private fun requestTimeout() = RequestReadResult.Failure(408, "Request Timeout", "Request headers timed out")

    private companion object {
        const val CR = 13
        const val LF = 10
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val TOKEN = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")
        val VALID_HOST = Regex("^(?:\\[[0-9A-Fa-f:.]+](?::[0-9]{1,5})?|[A-Za-z0-9._~-]+(?::[0-9]{1,5})?)$")
        val SUPPORTED_HTTP_VERSIONS = setOf("HTTP/1.0", "HTTP/1.1")
    }
}

internal object HttpResponseWriter {
    fun write(response: HttpResponse, output: OutputStream) {
        try {
            val headerText = buildString {
                append("HTTP/1.1 ")
                append(response.statusCode)
                append(' ')
                append(response.reasonPhrase)
                append("\r\n")
                response.headers.forEach { (name, value) ->
                    require(HEADER_NAME.matches(name)) { "Invalid response header name" }
                    require('\r' !in value && '\n' !in value) { "Invalid response header value" }
                    append(name)
                    append(": ")
                    append(value)
                    append("\r\n")
                }
                append("Connection: close\r\n\r\n")
            }
            output.write(headerText.toByteArray(Charsets.ISO_8859_1))
            response.writeBodyTo(output)
            output.flush()
        } finally {
            response.closeBody()
        }
    }

    private val HEADER_NAME = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")
}
