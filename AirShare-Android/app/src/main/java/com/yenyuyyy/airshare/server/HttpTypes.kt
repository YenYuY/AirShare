package com.yenyuyyy.airshare.server

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

data class HttpRequest(
    val method: String,
    val target: String,
    val headers: Map<String, String> = emptyMap(),
) {
    fun header(name: String): String? = headers.entries
        .firstOrNull { it.key.equals(name, ignoreCase = true) }
        ?.value
}

class HttpResponse(
    val statusCode: Int,
    val reasonPhrase: String,
    headers: Map<String, String>,
    private val bodyWriter: (OutputStream) -> Unit = {},
    private val closeAction: () -> Unit = {},
) {
    private val responseHeaders = LinkedHashMap<String, String>().apply {
        putAll(headers)
        putAll(SECURITY_HEADERS)
    }
    private val bodyClosed = AtomicBoolean(false)

    val headers: Map<String, String>
        get() = responseHeaders

    fun header(name: String): String? = responseHeaders.entries
        .firstOrNull { it.key.equals(name, ignoreCase = true) }
        ?.value

    fun writeBodyTo(output: OutputStream) {
        try {
            bodyWriter(output)
        } finally {
            closeBody()
        }
    }

    fun bodyBytes(): ByteArray = ByteArrayOutputStream().also(::writeBodyTo).toByteArray()

    internal fun withoutBody(): HttpResponse {
        closeBody()
        return HttpResponse(
            statusCode = statusCode,
            reasonPhrase = reasonPhrase,
            headers = responseHeaders,
        )
    }

    internal fun closeBody() {
        if (bodyClosed.compareAndSet(false, true)) closeAction()
    }

    private companion object {
        val SECURITY_HEADERS = mapOf(
            "Cache-Control" to "no-store",
            "X-Content-Type-Options" to "nosniff",
            "X-Frame-Options" to "DENY",
            "Referrer-Policy" to "no-referrer",
            "Content-Security-Policy" to "default-src 'self'; img-src 'self' data:; style-src 'unsafe-inline'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'",
            "Permissions-Policy" to "camera=(), microphone=(), geolocation=()",
            "Cross-Origin-Resource-Policy" to "same-origin",
        )
    }
}
