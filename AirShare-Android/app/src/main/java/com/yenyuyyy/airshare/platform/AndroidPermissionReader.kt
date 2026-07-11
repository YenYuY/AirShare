package com.yenyuyyy.airshare.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class AndroidPermissionReader(context: Context) {
    private val appContext = context.applicationContext

    fun mediaAccess(sdkInt: Int = Build.VERSION.SDK_INT): MediaPermissionAccess =
        MediaPermissionPolicy.resolve(
            sdkInt = sdkInt,
            grants = MediaPermissionGrants(
                readExternalStorage = isGranted(AirSharePermissionNames.READ_EXTERNAL_STORAGE),
                readImages = isGranted(AirSharePermissionNames.READ_MEDIA_IMAGES),
                readVideos = isGranted(AirSharePermissionNames.READ_MEDIA_VIDEO),
                readSelectedVisualMedia = isGranted(
                    AirSharePermissionNames.READ_MEDIA_VISUAL_USER_SELECTED,
                ),
            ),
        )

    fun mediaPermissionsToRequest(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> =
        MediaPermissionPolicy.permissionsToRequest(sdkInt).toTypedArray()

    fun hasLocalNetworkAccess(
        deviceSdkInt: Int = Build.VERSION.SDK_INT,
        targetSdkInt: Int = appContext.applicationInfo.targetSdkVersion,
    ): Boolean = !LocalNetworkPermissionPolicy.isRuntimePermissionRequired(
        deviceSdkInt = deviceSdkInt,
        targetSdkInt = targetSdkInt,
    ) || isGranted(LocalNetworkPermissionPolicy.ACCESS_LOCAL_NETWORK)

    fun localNetworkPermissionsToRequest(
        deviceSdkInt: Int = Build.VERSION.SDK_INT,
        targetSdkInt: Int = appContext.applicationInfo.targetSdkVersion,
    ): Array<String> = LocalNetworkPermissionPolicy.permissionsToRequest(
        deviceSdkInt = deviceSdkInt,
        targetSdkInt = targetSdkInt,
    ).toTypedArray()

    private fun isGranted(permission: String): Boolean =
        appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
