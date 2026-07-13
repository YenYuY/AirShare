package com.yenyuyyy.airshare.platform

object AirSharePermissionNames {
    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    const val READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES"
    const val READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO"
    const val READ_MEDIA_VISUAL_USER_SELECTED =
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
}

enum class MediaPermissionAccess {
    NONE,
    PARTIAL,
    FULL,
}

data class MediaPermissionGrants(
    val readExternalStorage: Boolean = false,
    val readImages: Boolean = false,
    val readVideos: Boolean = false,
    val readSelectedVisualMedia: Boolean = false,
)

object MediaPermissionPolicy {
    fun permissionsToRequest(sdkInt: Int): List<String> {
        require(sdkInt >= 29) { "AirShare supports Android API 29 and newer" }
        return when {
            sdkInt >= 34 -> listOf(
                AirSharePermissionNames.READ_MEDIA_IMAGES,
                AirSharePermissionNames.READ_MEDIA_VIDEO,
                AirSharePermissionNames.READ_MEDIA_VISUAL_USER_SELECTED,
            )
            sdkInt >= 33 -> listOf(
                AirSharePermissionNames.READ_MEDIA_IMAGES,
                AirSharePermissionNames.READ_MEDIA_VIDEO,
            )
            else -> listOf(AirSharePermissionNames.READ_EXTERNAL_STORAGE)
        }
    }

    fun resolve(
        sdkInt: Int,
        grants: MediaPermissionGrants,
    ): MediaPermissionAccess {
        require(sdkInt >= 29) { "AirShare supports Android API 29 and newer" }
        return when {
            sdkInt >= 33 && grants.readImages && grants.readVideos -> MediaPermissionAccess.FULL
            sdkInt >= 34 && grants.readSelectedVisualMedia -> MediaPermissionAccess.PARTIAL
            sdkInt >= 33 && (grants.readImages || grants.readVideos) -> MediaPermissionAccess.PARTIAL
            sdkInt >= 33 -> MediaPermissionAccess.NONE
            grants.readExternalStorage -> MediaPermissionAccess.FULL
            else -> MediaPermissionAccess.NONE
        }
    }
}
