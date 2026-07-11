package com.yenyuyyy.airshare.server

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaHttpHandlerTest {
    @Test
    fun `authorized root lists media while escaping untrusted metadata`() {
        val item = MediaItem(
            id = "photo/one",
            displayName = "<sun & \"sea\">.jpg",
            mimeType = "image/jpeg",
            kind = MediaKind.IMAGE,
            sizeBytes = 4,
        )
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(listOf(item), mapOf(item.id to byteArrayOf(1, 2, 3, 4))),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/?token=correct-token"))
        val html = response.bodyBytes().toString(Charsets.UTF_8)

        assertEquals(200, response.statusCode)
        assertEquals("text/html; charset=utf-8", response.header("Content-Type"))
        assertTrue(html.contains("&lt;sun &amp; &quot;sea&quot;&gt;.jpg"))
        assertFalse(html.contains("<sun & \"sea\">.jpg"))
    }

    @Test
    fun `request without the share token is rejected before media is exposed`() {
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(emptyList(), emptyMap()),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/"))
        val html = response.bodyBytes().toString(Charsets.UTF_8)

        assertEquals(401, response.statusCode)
        assertEquals("Bearer", response.header("WWW-Authenticate"))
        assertTrue(html.contains("Link unavailable"))
    }

    @Test
    fun `bearer token authenticates clients without putting the token in the URL`() {
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(emptyList(), emptyMap()),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(
            HttpRequest(
                method = "GET",
                target = "/",
                headers = mapOf("Authorization" to "Bearer correct-token"),
            ),
        )

        assertEquals(200, response.statusCode)
    }

    @Test
    fun `methods other than GET and HEAD are rejected`() {
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(emptyList(), emptyMap()),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("POST", "/?token=correct-token"))

        assertEquals(405, response.statusCode)
        assertEquals("GET, HEAD", response.header("Allow"))
    }

    @Test
    fun `HEAD returns the same representation length as GET without a body`() {
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(emptyList(), emptyMap()),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val get = handler.handle(HttpRequest("GET", "/?token=correct-token"))
        val head = handler.handle(HttpRequest("HEAD", "/?token=correct-token"))

        assertEquals(get.header("Content-Length"), head.header("Content-Length"))
        assertEquals(0, head.bodyBytes().size)
    }

    @Test
    fun `root clamps and renders twenty newest media items per page`() {
        val items = (1..25).map { index ->
            MediaItem(
                id = "id-$index",
                displayName = "media-$index.jpg",
                mimeType = "image/jpeg",
                kind = MediaKind.IMAGE,
                sizeBytes = 1,
            )
        }
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(items, emptyMap()),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/?page=2&token=correct-token"))
        val html = response.bodyBytes().toString(Charsets.UTF_8)

        assertTrue(html.contains("media-21.jpg"))
        assertTrue(html.contains("media-25.jpg"))
        assertFalse(html.contains("media-20.jpg"))
        assertTrue(html.contains("Page 2 / 2"))
        assertTrue(html.contains("/?token=correct-token&amp;page=1"))
    }

    @Test
    fun `empty catalog explains that there is nothing to share`() {
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(emptyList(), emptyMap()),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/?token=correct-token"))
        val html = response.bodyBytes().toString(Charsets.UTF_8)

        assertTrue(html.contains("<h2>No media</h2>"))
        assertTrue(html.contains("There are no photos or videos to share."))
    }

    @Test
    fun `media cards keep encoded identifiers and token in browser links`() {
        val item = MediaItem(
            id = "folder/a b?&",
            displayName = "clip.mp4",
            mimeType = "video/mp4",
            kind = MediaKind.VIDEO,
            sizeBytes = 4,
        )
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(listOf(item), emptyMap()),
            accessToken = "t +/&",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/?token=t%20%2B%2F%26"))
        val html = response.bodyBytes().toString(Charsets.UTF_8)

        assertTrue(html.contains("/asset/folder%2Fa%20b%3F%26?token=t%20%2B%2F%26"))
        assertTrue(html.contains("/thumbnail/folder%2Fa%20b%3F%26?token=t%20%2B%2F%26"))
        assertTrue(html.contains(">VIDEO</span>"))
    }

    @Test
    fun `thumbnail route returns the generated JPEG with an exact length`() {
        val item = MediaItem("photo/one", "photo.jpg", "image/jpeg", MediaKind.IMAGE, 4)
        val thumbnail = byteArrayOf(9, 8, 7)
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(
                items = listOf(item),
                assets = emptyMap(),
                thumbnails = mapOf(item.id to thumbnail),
            ),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(
            HttpRequest("GET", "/thumbnail/photo%2Fone?token=correct-token"),
        )

        assertEquals(200, response.statusCode)
        assertEquals("image/jpeg", response.header("Content-Type"))
        assertEquals("3", response.header("Content-Length"))
        assertTrue(thumbnail.contentEquals(response.bodyBytes()))
    }

    @Test
    fun `asset route streams the original media with download metadata`() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        val item = MediaItem("video-one", "holiday.mp4", "video/mp4", MediaKind.VIDEO, bytes.size.toLong())
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(listOf(item), mapOf(item.id to bytes)),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/asset/video-one?token=correct-token"))

        assertEquals(200, response.statusCode)
        assertEquals("video/mp4", response.header("Content-Type"))
        assertEquals("6", response.header("Content-Length"))
        assertEquals("bytes", response.header("Accept-Ranges"))
        assertEquals(
            "attachment; filename=\"holiday.mp4\"; filename*=UTF-8''holiday.mp4",
            response.header("Content-Disposition"),
        )
        assertTrue(bytes.contentEquals(response.bodyBytes()))
    }

    @Test
    fun `download filename cannot inject response headers and retains UTF-8 metadata`() {
        val item = MediaItem(
            "asset-one",
            "旅行\"\r\nX-Evil: yes.mp4",
            "video/mp4",
            MediaKind.VIDEO,
            1,
        )
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(listOf(item), mapOf(item.id to byteArrayOf(1))),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/asset/asset-one?token=correct-token"))
        val disposition = response.header("Content-Disposition")!!

        assertFalse(disposition.contains('\r'))
        assertFalse(disposition.contains('\n'))
        assertTrue(disposition.contains("filename=\"_____X-Evil: yes.mp4\""))
        assertTrue(disposition.contains("filename*=UTF-8''%E6%97%85%E8%A1%8C___X-Evil%3A%20yes.mp4"))
    }

    @Test
    fun `asset byte range returns only the requested inclusive interval`() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        val item = MediaItem("video-one", "clip.mp4", "video/mp4", MediaKind.VIDEO, 6)
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(listOf(item), mapOf(item.id to bytes)),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(
            HttpRequest(
                "GET",
                "/asset/video-one?token=correct-token",
                mapOf("Range" to "bytes=2-4"),
            ),
        )

        assertEquals(206, response.statusCode)
        assertEquals("bytes 2-4/6", response.header("Content-Range"))
        assertEquals("3", response.header("Content-Length"))
        assertTrue(byteArrayOf(2, 3, 4).contentEquals(response.bodyBytes()))
    }

    @Test
    fun `open ended suffix and oversized ranges follow HTTP byte semantics`() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        val item = MediaItem("video-one", "clip.mp4", "video/mp4", MediaKind.VIDEO, 6)
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(listOf(item), mapOf(item.id to bytes)),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )
        val examples = listOf(
            Triple("bytes=3-", "bytes 3-5/6", byteArrayOf(3, 4, 5)),
            Triple("bytes=-2", "bytes 4-5/6", byteArrayOf(4, 5)),
            Triple("bytes=4-99", "bytes 4-5/6", byteArrayOf(4, 5)),
        )

        examples.forEach { (range, expectedHeader, expectedBody) ->
            val response = handler.handle(
                HttpRequest(
                    "GET",
                    "/asset/video-one?token=correct-token",
                    mapOf("Range" to range),
                ),
            )
            assertEquals(expectedHeader, response.header("Content-Range"))
            assertTrue(expectedBody.contentEquals(response.bodyBytes()))
        }
    }

    @Test
    fun `malformed unsupported and unsatisfiable ranges return 416`() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        val item = MediaItem("video-one", "clip.mp4", "video/mp4", MediaKind.VIDEO, 6)
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(listOf(item), mapOf(item.id to bytes)),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        listOf("bytes=9-10", "bytes=abc").forEach { range ->
            val response = handler.handle(
                HttpRequest(
                    "GET",
                    "/asset/video-one?token=correct-token",
                    mapOf("Range" to range),
                ),
            )
            assertEquals(416, response.statusCode)
            assertEquals("bytes */6", response.header("Content-Range"))
            assertEquals(0, response.bodyBytes().size)
        }
    }

    @Test
    fun `unsupported range units and multipart ranges are safely ignored`() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        val item = MediaItem("video-one", "clip.mp4", "video/mp4", MediaKind.VIDEO, 6)
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(listOf(item), mapOf(item.id to bytes)),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        listOf("items=0-1", "bytes=0-1,4-5").forEach { range ->
            val response = handler.handle(
                HttpRequest(
                    "GET",
                    "/asset/video-one?token=correct-token",
                    mapOf("Range" to range),
                ),
            )
            assertEquals(200, response.statusCode)
            assertNull(response.header("Content-Range"))
            assertTrue(bytes.contentEquals(response.bodyBytes()))
        }
    }

    @Test
    fun `responses prevent caching sniffing framing and referrer leakage`() {
        val handler = MediaHttpHandler(
            catalog = FakeMediaCatalog(emptyList(), emptyMap()),
            accessToken = "correct-token",
            strings = TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/?token=correct-token"))

        assertEquals("no-store", response.header("Cache-Control"))
        assertEquals("nosniff", response.header("X-Content-Type-Options"))
        assertEquals("DENY", response.header("X-Frame-Options"))
        assertEquals("no-referrer", response.header("Referrer-Policy"))
        assertTrue(response.header("Content-Security-Policy")!!.contains("default-src 'self'"))
        assertEquals("camera=(), microphone=(), geolocation=()", response.header("Permissions-Policy"))
    }

    @Test
    fun `catalog failure becomes a generic 500 response without leaking details`() {
        val failingCatalog = object : MediaCatalog {
            override fun totalCount(): Int = error("private storage detail")
            override fun list(offset: Int, limit: Int): List<MediaItem> = error("unused")
            override fun find(id: String): MediaItem? = error("unused")
            override fun thumbnail(id: String, targetSizePx: Int): ByteArray? = error("unused")
            override fun openAsset(id: String): InputStream? = error("unused")
        }
        val handler = MediaHttpHandler(failingCatalog, "correct-token", TestWebStrings)

        val response = handler.handle(HttpRequest("GET", "/?token=correct-token"))
        val body = response.bodyBytes().toString(Charsets.UTF_8)

        assertEquals(500, response.statusCode)
        assertFalse(body.contains("private storage detail"))
    }

    @Test
    fun `invalid catalog MIME metadata falls back to a safe binary content type`() {
        val item = MediaItem(
            "asset-one",
            "clip.bin",
            "video/mp4\r\nX-Evil: yes",
            MediaKind.VIDEO,
            1,
        )
        val handler = MediaHttpHandler(
            FakeMediaCatalog(listOf(item), mapOf(item.id to byteArrayOf(1))),
            "correct-token",
            TestWebStrings,
        )

        val response = handler.handle(HttpRequest("GET", "/asset/asset-one?token=correct-token"))

        assertEquals("application/octet-stream", response.header("Content-Type"))
    }

    @Test
    fun `HEAD ignores Range and reports the full representation without opening the asset`() {
        val item = MediaItem("asset-one", "clip.mp4", "video/mp4", MediaKind.VIDEO, 10)
        val handler = MediaHttpHandler(
            FakeMediaCatalog(listOf(item), emptyMap()),
            "correct-token",
            TestWebStrings,
        )

        val response = handler.handle(
            HttpRequest(
                "HEAD",
                "/asset/asset-one?token=correct-token",
                mapOf("Range" to "bytes=4-7"),
            ),
        )

        assertEquals(200, response.statusCode)
        assertNull(response.header("Content-Range"))
        assertEquals("10", response.header("Content-Length"))
        assertEquals(0, response.bodyBytes().size)
    }

    @Test
    fun `If-Range without a matching validator falls back to the full representation`() {
        val bytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        val item = MediaItem("asset-one", "clip.mp4", "video/mp4", MediaKind.VIDEO, 6)
        val handler = MediaHttpHandler(
            FakeMediaCatalog(listOf(item), mapOf(item.id to bytes)),
            "correct-token",
            TestWebStrings,
        )

        val response = handler.handle(
            HttpRequest(
                "GET",
                "/asset/asset-one?token=correct-token",
                mapOf("Range" to "bytes=1-2", "If-Range" to "\"stale-validator\""),
            ),
        )

        assertEquals(200, response.statusCode)
        assertNull(response.header("Content-Range"))
        assertTrue(bytes.contentEquals(response.bodyBytes()))
    }
}

private class FakeMediaCatalog(
    private val items: List<MediaItem>,
    private val assets: Map<String, ByteArray>,
    private val thumbnails: Map<String, ByteArray> = emptyMap(),
) : MediaCatalog {
    override fun totalCount(): Int = items.size

    override fun list(offset: Int, limit: Int): List<MediaItem> = items.drop(offset).take(limit)

    override fun find(id: String): MediaItem? = items.firstOrNull { it.id == id }

    override fun thumbnail(id: String, targetSizePx: Int): ByteArray? = thumbnails[id]

    override fun openAsset(id: String): InputStream? = assets[id]?.let(::ByteArrayInputStream)
}

private object TestWebStrings : WebStrings {
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
