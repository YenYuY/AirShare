package com.yenyuyyy.airshare.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPermissionPolicyTest {
    @Test
    fun `API 29 reports full access when legacy storage permission is granted`() {
        val access = MediaPermissionPolicy.resolve(
            sdkInt = 29,
            grants = MediaPermissionGrants(readExternalStorage = true),
        )

        assertEquals(MediaPermissionAccess.FULL, access)
    }

    @Test
    fun `API 33 reports full access only when image and video permissions are granted`() {
        val access = MediaPermissionPolicy.resolve(
            sdkInt = 33,
            grants = MediaPermissionGrants(
                readImages = true,
                readVideos = true,
            ),
        )

        assertEquals(MediaPermissionAccess.FULL, access)
    }

    @Test
    fun `API 33 reports partial access when only image permission is granted`() {
        val access = MediaPermissionPolicy.resolve(
            sdkInt = 33,
            grants = MediaPermissionGrants(readImages = true),
        )

        assertEquals(MediaPermissionAccess.PARTIAL, access)
    }

    @Test
    fun `API 34 reports partial access for user-selected photos and videos`() {
        val access = MediaPermissionPolicy.resolve(
            sdkInt = 34,
            grants = MediaPermissionGrants(readSelectedVisualMedia = true),
        )

        assertEquals(MediaPermissionAccess.PARTIAL, access)
    }

    @Test
    fun `API 29 requests only legacy media permission`() {
        assertEquals(
            listOf("android.permission.READ_EXTERNAL_STORAGE"),
            MediaPermissionPolicy.permissionsToRequest(sdkInt = 29),
        )
    }

    @Test
    fun `API 33 requests image and video permissions without legacy storage`() {
        assertEquals(
            listOf(
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
            ),
            MediaPermissionPolicy.permissionsToRequest(sdkInt = 33),
        )
    }

    @Test
    fun `API 34 includes selected visual media in the permission request`() {
        assertEquals(
            listOf(
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
            ),
            MediaPermissionPolicy.permissionsToRequest(sdkInt = 34),
        )
    }

    @Test
    fun `API 35 reports no access when every media permission is denied`() {
        val access = MediaPermissionPolicy.resolve(
            sdkInt = 35,
            grants = MediaPermissionGrants(),
        )

        assertEquals(MediaPermissionAccess.NONE, access)
    }
}
