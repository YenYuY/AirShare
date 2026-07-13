package com.yenyuyyy.airshare.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yenyuyyy.airshare.language.AppLanguage
import com.yenyuyyy.airshare.language.AppLanguageOption
import com.yenyuyyy.airshare.language.textFor
import com.yenyuyyy.airshare.ui.theme.AirShareTheme

@Preview(
    name = "Phone · Compact",
    group = "Adaptive",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
private fun PhoneReadyPreview() {
    AirSharePreview(
        state = readyState,
        language = AppLanguage.TRADITIONAL_CHINESE,
    )
}

@Preview(
    name = "Tablet portrait · Medium",
    group = "Adaptive",
    widthDp = 800,
    heightDp = 1280,
    showBackground = true,
)
@Composable
private fun TabletPortraitPreview() {
    AirSharePreview(
        state = readyState,
        language = AppLanguage.JAPANESE,
    )
}

@Preview(
    name = "Tablet landscape · Expanded",
    group = "Adaptive",
    widthDp = 1100,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun TabletLandscapePreview() {
    AirSharePreview(
        state = activeState,
        language = AppLanguage.ENGLISH,
    )
}

@Preview(
    name = "Desktop window · Large",
    group = "Adaptive",
    widthDp = 1440,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun DesktopWindowPreview() {
    AirSharePreview(
        state = activeState,
        language = AppLanguage.TRADITIONAL_CHINESE,
    )
}

@Composable
private fun AirSharePreview(
    state: AirShareUiState,
    language: AppLanguage,
) {
    AirShareTheme {
        AirShareScreen(
            state = state,
            text = textFor(language),
            selectedLanguage = when (language) {
                AppLanguage.ENGLISH -> AppLanguageOption.ENGLISH
                AppLanguage.TRADITIONAL_CHINESE -> AppLanguageOption.TRADITIONAL_CHINESE
                AppLanguage.JAPANESE -> AppLanguageOption.JAPANESE
            },
            permissionAction = PermissionAction.REQUEST,
            onLanguageSelected = {},
            onRequestPermission = {},
            onOpenSettings = {},
            onChangeSelection = {},
            onStartSharing = {},
            onStopSharing = {},
            onCopyAddress = {},
            onDismissNotice = {},
        )
    }
}

private val readyState = AirShareUiState(
    mediaAccess = MediaAccess.FULL,
    network = NetworkStatus(onWifi = true, address = "192.168.1.24"),
)

private val activeState = readyState.copy(
    shareStatus = ShareStatus.Active(
        url = "http://192.168.1.24:8080/?token=preview-session-token",
    ),
)
