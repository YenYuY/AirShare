package com.yenyuyyy.airshare.platform

object LocalNetworkPermissionPolicy {
    const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    const val ENFORCEMENT_API = 37

    fun isRuntimePermissionRequired(
        deviceSdkInt: Int,
        targetSdkInt: Int,
    ): Boolean = deviceSdkInt >= ENFORCEMENT_API && targetSdkInt >= ENFORCEMENT_API

    fun permissionsToRequest(
        deviceSdkInt: Int,
        targetSdkInt: Int,
    ): List<String> = if (isRuntimePermissionRequired(deviceSdkInt, targetSdkInt)) {
        listOf(ACCESS_LOCAL_NETWORK)
    } else {
        emptyList()
    }
}
