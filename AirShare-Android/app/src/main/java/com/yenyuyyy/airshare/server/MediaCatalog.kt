package com.yenyuyyy.airshare.server

import java.io.InputStream

enum class MediaKind {
    IMAGE,
    VIDEO,
}

data class MediaItem(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val kind: MediaKind,
    val sizeBytes: Long,
)

interface MediaCatalog {
    fun totalCount(): Int

    fun list(offset: Int, limit: Int): List<MediaItem>

    fun find(id: String): MediaItem?

    fun thumbnail(id: String, targetSizePx: Int = 320): ByteArray?

    fun openAsset(id: String): InputStream?
}
