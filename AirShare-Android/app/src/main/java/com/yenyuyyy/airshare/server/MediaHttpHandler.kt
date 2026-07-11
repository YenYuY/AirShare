package com.yenyuyyy.airshare.server

import java.io.EOFException
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest

class MediaHttpHandler(
    private val catalog: MediaCatalog,
    private val accessToken: String,
    private val strings: WebStrings,
    private val pageSize: Int = 20,
) {
    init {
        require(accessToken.isNotEmpty()) { "accessToken must not be empty" }
        require(pageSize > 0) { "pageSize must be positive" }
    }

    fun handle(request: HttpRequest): HttpResponse {
        if (request.method != "GET" && request.method != "HEAD") {
            return textResponse(
                statusCode = 405,
                reasonPhrase = "Method Not Allowed",
                text = "Method Not Allowed",
                additionalHeaders = mapOf("Allow" to "GET, HEAD"),
            )
        }
        val response = handleGet(request)
        return if (request.method == "HEAD") response.withoutBody() else response
    }

    private fun handleGet(request: HttpRequest): HttpResponse {
        if (!hasValidToken(request)) {
            return htmlResponse(
                statusCode = 401,
                reasonPhrase = "Unauthorized",
                html = """
                    <!doctype html><html lang="${escapeHtml(strings.htmlLanguage)}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escapeHtml(strings.unauthorizedTitle)}</title>
                    <style>:root{font-family:Roboto,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;color:#e3e2e6;background:#111318;color-scheme:dark}body{margin:0;display:grid;min-height:100vh;place-items:center;padding:24px}.notice{max-width:520px;padding:32px;border:1px solid #44474f;border-radius:20px;background:#1b1b1f}.notice h1{margin-top:0}.notice p{margin-bottom:0;color:#c4c6d0}</style></head>
                    <body><main class="notice"><h1>${escapeHtml(strings.unauthorizedTitle)}</h1><p>${escapeHtml(strings.unauthorizedMessage)}</p></main></body></html>
                """.trimIndent(),
                additionalHeaders = mapOf("WWW-Authenticate" to "Bearer"),
            )
        }
        val path = request.target.substringBefore('?')
        return try {
            when {
                path == "/" -> serveIndex(request)
                path.startsWith("/thumbnail/") -> {
                    val id = decodePathSegment(path.removePrefix("/thumbnail/"))
                    serveThumbnail(id)
                }
                path.startsWith("/asset/") -> {
                    val id = decodePathSegment(path.removePrefix("/asset/"))
                    serveAsset(
                        id = id,
                        includeBody = request.method == "GET",
                        rangeHeader = request.header("Range").takeIf {
                            request.method == "GET" && request.header("If-Range") == null
                        },
                    )
                }
                else -> textResponse(404, "Not Found", "Not Found")
            }
        } catch (_: Exception) {
            textResponse(500, "Internal Server Error", "Internal Server Error")
        }
    }

    private fun serveIndex(request: HttpRequest): HttpResponse {
        val totalCount = catalog.totalCount().coerceAtLeast(0)
        val pageCount = maxOf(
            1L,
            (totalCount.toLong() + pageSize - 1L) / pageSize,
        ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val requestedPage = queryParameters(request.target)["page"]?.toIntOrNull() ?: 1
        val page = requestedPage.coerceIn(1, pageCount)
        val items = catalog.list(offset = (page - 1) * pageSize, limit = pageSize)
        val pageInfo = strings.pageInfo(page, pageCount)
        val html = buildString {
            append("<!doctype html><html lang=\"")
            append(escapeHtml(strings.htmlLanguage))
            append("\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
            append("<meta name=\"color-scheme\" content=\"dark\"><title>")
            append(escapeHtml(strings.appName))
            append("</title><style>")
            append(INDEX_CSS)
            append("</style></head><body><main><header><div class=\"brand\"><span class=\"brandmark\">A</span><h1>")
            append(escapeHtml(strings.appName))
            append("</h1></div></header><section class=\"grid\">")
            if (items.isEmpty()) {
                append("<section class=\"empty\"><h2>")
                append(escapeHtml(strings.emptyTitle))
                append("</h2><p>")
                append(escapeHtml(strings.emptyMessage))
                append("</p></section>")
            } else {
                items.forEach { item ->
                    val encodedId = encodeQueryComponent(item.id)
                    val encodedToken = encodeQueryComponent(accessToken)
                    val assetUrl = "/asset/$encodedId?token=$encodedToken"
                    val thumbnailUrl = "/thumbnail/$encodedId?token=$encodedToken"
                    append("<a class=\"card\" href=\"")
                    append(escapeHtml(assetUrl))
                    append("\" download><div class=\"thumb\"><img src=\"")
                    append(escapeHtml(thumbnailUrl))
                    append("\" alt=\"")
                    append(escapeHtml(item.displayName))
                    append("\">")
                    if (item.kind == MediaKind.VIDEO) {
                        append("<span class=\"badge\">")
                        append(escapeHtml(strings.videoBadge))
                        append("</span>")
                    }
                    append("</div><div class=\"meta\">")
                    append(escapeHtml(item.displayName))
                    append("</div></a>")
                }
            }
            append("</section><nav aria-label=\"")
            append(escapeHtml(pageInfo))
            append("\">")
            if (page > 1) {
                append("<a class=\"button\" href=\"")
                append(escapeHtml(pageUrl(page - 1)))
                append("\">")
                append(escapeHtml(strings.previousPage))
                append("</a>")
            } else {
                append("<span class=\"button disabled\" aria-disabled=\"true\">")
                append(escapeHtml(strings.previousPage))
                append("</span>")
            }
            append("<span class=\"page-info\">")
            append(escapeHtml(pageInfo))
            append("</span>")
            if (page < pageCount) {
                append("<a class=\"button\" href=\"")
                append(escapeHtml(pageUrl(page + 1)))
                append("\">")
                append(escapeHtml(strings.nextPage))
                append("</a>")
            } else {
                append("<span class=\"button disabled\" aria-disabled=\"true\">")
                append(escapeHtml(strings.nextPage))
                append("</span>")
            }
            append("</nav>")
            append("<footer>")
            append(escapeHtml(strings.downloadHint))
            append("</footer>")
            append("</main></body></html>")
        }
        return htmlResponse(200, "OK", html)
    }

    private fun serveThumbnail(id: String): HttpResponse {
        if (id.isEmpty()) return textResponse(404, "Not Found", "Not Found")
        val bytes = catalog.thumbnail(id, targetSizePx = 320)
            ?: return textResponse(404, "Not Found", "Not Found")
        return binaryResponse(
            statusCode = 200,
            reasonPhrase = "OK",
            contentType = "image/jpeg",
            bytes = bytes,
        )
    }

    private fun serveAsset(id: String, includeBody: Boolean, rangeHeader: String?): HttpResponse {
        if (id.isEmpty()) return textResponse(404, "Not Found", "Not Found")
        val item = catalog.find(id) ?: return textResponse(404, "Not Found", "Not Found")
        if (item.sizeBytes < 0) return textResponse(500, "Internal Server Error", "Internal Server Error")
        val normalizedRangeHeader = rangeHeader?.trim()
        val isSingleByteRange = normalizedRangeHeader?.let {
            it.startsWith("bytes=", ignoreCase = true) && ',' !in it
        } == true
        val requestedRange = normalizedRangeHeader
            ?.takeIf { isSingleByteRange }
            ?.let { parseRange(it, item.sizeBytes) }
        if (isSingleByteRange && requestedRange == null) {
            return HttpResponse(
                statusCode = 416,
                reasonPhrase = "Range Not Satisfiable",
                headers = mapOf(
                    "Content-Range" to "bytes */${item.sizeBytes}",
                    "Content-Length" to "0",
                    "Accept-Ranges" to "bytes",
                ),
            )
        }
        val start = requestedRange?.start ?: 0L
        val endInclusive = requestedRange?.endInclusive ?: (item.sizeBytes - 1)
        val responseLength = if (item.sizeBytes == 0L) 0L else endInclusive - start + 1
        val input = if (includeBody) {
            catalog.openAsset(id) ?: return textResponse(404, "Not Found", "Not Found")
        } else {
            null
        }
        val contentDisposition = contentDisposition(item.displayName)
        return HttpResponse(
            statusCode = if (requestedRange == null) 200 else 206,
            reasonPhrase = if (requestedRange == null) "OK" else "Partial Content",
            headers = mapOf(
                "Content-Type" to safeMimeType(item.mimeType),
                "Content-Length" to responseLength.toString(),
                "Content-Disposition" to contentDisposition,
                "Accept-Ranges" to "bytes",
            ) + if (requestedRange == null) {
                emptyMap()
            } else {
                mapOf("Content-Range" to "bytes $start-$endInclusive/${item.sizeBytes}")
            },
            bodyWriter = { output ->
                input?.use {
                    skipExactly(it, start)
                    copyExactly(it, output, responseLength)
                }
            },
            closeAction = { input?.close() },
        )
    }

    private fun safeMimeType(value: String): String =
        value.takeIf(MIME_TYPE::matches) ?: "application/octet-stream"

    private fun parseRange(value: String, totalLength: Long): LongRange? {
        val normalized = value.trim()
        if (!normalized.startsWith("bytes=", ignoreCase = true) || normalized.contains(',')) return null
        val bounds = normalized.substringAfter('=').split('-', limit = 2)
        if (bounds.size != 2 || (bounds[0].isEmpty() && bounds[1].isEmpty()) || totalLength <= 0) return null
        if (bounds[0].isEmpty()) {
            val suffixLength = bounds[1].toLongOrNull() ?: return null
            if (suffixLength <= 0) return null
            val actualLength = minOf(suffixLength, totalLength)
            return (totalLength - actualLength)..(totalLength - 1)
        }
        val start = bounds[0].toLongOrNull() ?: return null
        if (start < 0 || start >= totalLength) return null
        if (bounds[1].isEmpty()) return start..(totalLength - 1)
        val requestedEnd = bounds[1].toLongOrNull() ?: return null
        if (requestedEnd < start) return null
        return start..minOf(requestedEnd, totalLength - 1)
    }

    private fun skipExactly(input: InputStream, byteCount: Long) {
        var remaining = byteCount
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                if (input.read() < 0) throw EOFException("Media stream ended while seeking")
                remaining--
            }
        }
    }

    private fun contentDisposition(displayName: String): String {
        val safeUnicodeName = displayName
            .take(180)
            .map { character ->
                if (character == '\"' || character == '\\' || character.code < 0x20 || character.code == 0x7f) {
                    '_'
                } else {
                    character
                }
            }
            .joinToString("")
            .ifEmpty { "download" }
        val asciiFallback = safeUnicodeName.map { character ->
            if (character.code in 0x20..0x7e && character != '\"' && character != '\\') character else '_'
        }.joinToString("")
        return "attachment; filename=\"$asciiFallback\"; filename*=UTF-8''${encodeRfc5987(safeUnicodeName)}"
    }

    private fun encodeRfc5987(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val unsigned = byte.toInt() and 0xff
            val character = unsigned.toChar()
            if (
                character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' ||
                character == '!' || character == '#' || character == '$' || character == '&' ||
                character == '+' || character == '-' || character == '.' || character == '^' ||
                character == '_' || character == '`' || character == '|' || character == '~'
            ) {
                append(character)
            } else {
                append('%')
                append(HEX_DIGITS[unsigned ushr 4])
                append(HEX_DIGITS[unsigned and 0x0f])
            }
        }
    }

    private fun copyExactly(input: InputStream, output: java.io.OutputStream, length: Long) {
        var remaining = length
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw EOFException("Media stream ended with $remaining bytes remaining")
            if (read == 0) {
                val singleByte = input.read()
                if (singleByte < 0) throw EOFException("Media stream ended with $remaining bytes remaining")
                output.write(singleByte)
                remaining--
                continue
            }
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun binaryResponse(
        statusCode: Int,
        reasonPhrase: String,
        contentType: String,
        bytes: ByteArray,
        additionalHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse = HttpResponse(
        statusCode = statusCode,
        reasonPhrase = reasonPhrase,
        headers = mapOf(
            "Content-Type" to contentType,
            "Content-Length" to bytes.size.toString(),
        ) + additionalHeaders,
        bodyWriter = { it.write(bytes) },
    )

    private fun textResponse(
        statusCode: Int,
        reasonPhrase: String,
        text: String,
        additionalHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return HttpResponse(
            statusCode = statusCode,
            reasonPhrase = reasonPhrase,
            headers = mapOf(
                "Content-Type" to "text/plain; charset=utf-8",
                "Content-Length" to bytes.size.toString(),
            ) + additionalHeaders,
            bodyWriter = { it.write(bytes) },
        )
    }

    private fun htmlResponse(
        statusCode: Int,
        reasonPhrase: String,
        html: String,
        additionalHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val bytes = html.toByteArray(Charsets.UTF_8)
        return HttpResponse(
            statusCode = statusCode,
            reasonPhrase = reasonPhrase,
            headers = mapOf(
                "Content-Type" to "text/html; charset=utf-8",
                "Content-Length" to bytes.size.toString(),
            ) + additionalHeaders,
            bodyWriter = { it.write(bytes) },
        )
    }

    private fun hasValidToken(request: HttpRequest): Boolean {
        val bearerToken = request.header("Authorization")
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(' ')
            ?.trim()
        val queryToken = queryParameters(request.target)["token"]
        val supplied = bearerToken ?: queryToken
            ?: return false
        return MessageDigest.isEqual(
            supplied.toByteArray(Charsets.UTF_8),
            accessToken.toByteArray(Charsets.UTF_8),
        )
    }

    private fun queryParameters(target: String): Map<String, String> =
        target.substringAfter('?', missingDelimiterValue = "")
            .split('&')
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator < 0) return@mapNotNull null
                val key = decodeQueryComponent(pair.substring(0, separator))
                val value = decodeQueryComponent(pair.substring(separator + 1))
                key to value
            }
            .toMap()

    private fun pageUrl(page: Int): String =
        "/?token=${encodeQueryComponent(accessToken)}&page=$page"

    private fun decodeQueryComponent(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)

    private fun decodePathSegment(value: String): String =
        runCatching {
            URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8.name())
        }.getOrDefault(value)

    private fun encodeQueryComponent(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun escapeHtml(value: String): String = buildString(value.length) {
        value.forEach { character ->
            append(
                when (character) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '\"' -> "&quot;"
                    '\'' -> "&#39;"
                    else -> character
                },
            )
        }
    }

    private companion object {
        const val HEX_DIGITS = "0123456789ABCDEF"
        val MIME_TYPE = Regex("^[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*/[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*$")
        val INDEX_CSS = """
            :root{font-family:Roboto,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;color:#e3e2e6;background:#111318;color-scheme:dark}
            *{box-sizing:border-box}body{margin:0;background:#111318;color:#e3e2e6}main{width:min(1120px,100%);margin:auto;padding:24px}
            header{display:flex;align-items:center;justify-content:space-between;margin-bottom:24px}.brand{display:flex;align-items:center;gap:12px}
            .brandmark{display:grid;place-items:center;width:40px;height:40px;border-radius:12px;background:#a9c7ff;color:#0a305f;font-weight:800}
            h1{font-size:1.5rem;margin:0}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(164px,1fr));gap:14px}
            .card{display:block;overflow:hidden;border:1px solid #44474f;border-radius:16px;background:#1b1b1f;color:inherit;text-decoration:none;transition:border-color .15s ease,transform .15s ease}
            .card:hover,.card:focus-visible{border-color:#a9c7ff;transform:translateY(-2px);outline:none}.thumb{position:relative;aspect-ratio:1;background:#282a2f;overflow:hidden}
            .thumb img{display:block;width:100%;height:100%;object-fit:cover}.meta{padding:12px;font-size:.875rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
            .badge{position:absolute;right:8px;bottom:8px;padding:4px 8px;border-radius:999px;background:#202124e6;color:#f1f3f4;font-size:.6875rem;font-weight:700;letter-spacing:.06em}
            .empty{grid-column:1/-1;padding:48px 24px;border:1px solid #44474f;border-radius:20px;text-align:center;background:#1b1b1f}.empty h2{margin:0 0 8px}.empty p{margin:0;color:#c4c6d0}
            nav{display:flex;align-items:center;justify-content:center;gap:12px;margin-top:24px}.button{min-height:40px;padding:10px 16px;border-radius:999px;background:#a9c7ff;color:#0a305f;text-decoration:none;font-weight:600}
            .button.disabled{opacity:.38}.page-info{color:#c4c6d0;font-size:.875rem}footer{margin-top:20px;text-align:center;color:#c4c6d0;font-size:.8125rem}
            @media(max-width:520px){main{padding:16px}.grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}nav{gap:8px}.button{padding:10px 12px}.page-info{font-size:.75rem}}
            @media(prefers-reduced-motion:reduce){.card{transition:none}}
        """.trimIndent().replace("\n", "")
    }
}
