package com.yenyuyyy.airshare.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPermissionPolicyTest {
    @Test
    fun `Android 17 app targeting API 37 requires local network permission`() {
        assertTrue(
            LocalNetworkPermissionPolicy.isRuntimePermissionRequired(
                deviceSdkInt = 37,
                targetSdkInt = 37,
            ),
        )
    }

    @Test
    fun `API 37 helper returns the future permission string`() {
        assertEquals(
            listOf("android.permission.ACCESS_LOCAL_NETWORK"),
            LocalNetworkPermissionPolicy.permissionsToRequest(
                deviceSdkInt = 37,
                targetSdkInt = 37,
            ),
        )
    }

    @Test
    fun `Android 17 keeps implicit local access for apps targeting below API 37`() {
        assertFalse(
            LocalNetworkPermissionPolicy.isRuntimePermissionRequired(
                deviceSdkInt = 37,
                targetSdkInt = 35,
            ),
        )
    }
}
