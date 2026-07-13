package com.yenyuyyy.airshare.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Size
import com.yenyuyyy.airshare.server.MediaCatalog
import com.yenyuyyy.airshare.server.MediaItem
import com.yenyuyyy.airshare.server.MediaKind
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class MediaStoreMediaCatalog(
    private val contentResolver: ContentResolver,
) : MediaCatalog {
    private val collection: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    override fun totalCount(): Int = contentResolver.query(
        collection,
        arrayOf(BaseColumns._ID),
        MEDIA_SELECTION,
        MEDIA_SELECTION_ARGS,
        null,
    )?.use { it.count } ?: 0

    override fun list(offset: Int, limit: Int): List<MediaItem> {
        require(offset >= 0) { "offset must not be negative" }
        require(limit >= 0) { "limit must not be negative" }
        if (limit == 0) return emptyList()

        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MEDIA_SELECTION)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, MEDIA_SELECTION_ARGS)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(
                    MediaStore.MediaColumns.DATE_TAKEN,
                    MediaStore.MediaColumns.DATE_ADDED,
                    BaseColumns._ID,
                ),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }

        return contentResolver.query(collection, ITEM_PROJECTION, queryArgs, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    cursor.toMediaItem()?.let { add(it) }
                }
            }
        } ?: emptyList()
    }

    override fun find(id: String): MediaItem? {
        val numericId = id.toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val selection = "${BaseColumns._ID} = ? AND ($MEDIA_SELECTION)"
        val selectionArgs = arrayOf(numericId.toString(), *MEDIA_SELECTION_ARGS)

        return contentResolver.query(
            collection,
            ITEM_PROJECTION,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.toMediaItem() else null
        }
    }

    override fun thumbnail(id: String, targetSizePx: Int): ByteArray? {
        require(targetSizePx > 0) { "targetSizePx must be positive" }
        val item = find(id) ?: return null
        val uri = uriFor(item.id) ?: return null
        val bitmap = try {
            contentResolver.loadThumbnail(uri, Size(targetSizePx, targetSizePx), null)
        } catch (_: FileNotFoundException) {
            return null
        }

        return ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_JPEG_QUALITY, output)) {
                null
            } else {
                output.toByteArray()
            }
        }
    }

    override fun openAsset(id: String): InputStream? {
        val item = find(id) ?: return null
        val uri = uriFor(item.id) ?: return null
        val descriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)
    }

    private fun Cursor.toMediaItem(): MediaItem? {
        val mediaType = getInt(getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE))
        val kind = when (mediaType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaKind.IMAGE
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaKind.VIDEO
            else -> return null
        }
        val id = getLong(getColumnIndexOrThrow(BaseColumns._ID)).toString()
        val displayNameIndex = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val mimeTypeIndex = getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val sizeIndex = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val displayName = if (isNull(displayNameIndex)) null else getString(displayNameIndex)
        val mimeType = if (isNull(mimeTypeIndex)) null else getString(mimeTypeIndex)
        val sizeBytes = if (isNull(sizeIndex)) UNKNOWN_SIZE else getLong(sizeIndex)

        return MediaItem(
            id = id,
            displayName = displayName?.takeIf { it.isNotBlank() } ?: "media-$id",
            mimeType = mimeType?.takeIf { it.isNotBlank() } ?: kind.fallbackMimeType,
            kind = kind,
            sizeBytes = resolveContentLength(id, sizeBytes),
        )
    }

    private fun resolveContentLength(id: String, reportedSize: Long): Long {
        if (reportedSize >= 0) return reportedSize
        val uri = uriFor(id) ?: throw IOException("Invalid MediaStore identifier: $id")
        return contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0 }
                ?: descriptor.parcelFileDescriptor.statSize.takeIf { it >= 0 }
        } ?: throw IOException("Unable to determine content length for media $id")
    }

    private fun uriFor(id: String): Uri? = id.toLongOrNull()
        ?.takeIf { it >= 0 }
        ?.let { ContentUris.withAppendedId(collection, it) }

    private val MediaKind.fallbackMimeType: String
        get() = when (this) {
            MediaKind.IMAGE -> "image/*"
            MediaKind.VIDEO -> "video/*"
        }

    private companion object {
        const val UNKNOWN_SIZE = -1L
        const val THUMBNAIL_JPEG_QUALITY = 85

        val MEDIA_SELECTION: String =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val MEDIA_SELECTION_ARGS: Array<String> = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val ITEM_PROJECTION: Array<String> = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.SIZE,
        )
    }
}
