package com.yenyuyyy.airshare.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yenyuyyy.airshare.R
import com.yenyuyyy.airshare.language.AppLanguageOption
import com.yenyuyyy.airshare.language.AppText
import kotlinx.coroutines.launch

enum class PermissionAction {
    REQUEST,
    OPEN_SETTINGS,
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AirShareScreen(
    state: AirShareUiState,
    text: AppText,
    selectedLanguage: AppLanguageOption,
    permissionAction: PermissionAction,
    onLanguageSelected: (AppLanguageOption) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeSelection: () -> Unit,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    onCopyAddress: (String) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val contentScrollState = rememberScrollState()
    var privacyDetailsOpen by rememberSaveable { mutableStateOf(false) }
    val windowSize = currentWindowDpSize()
    val layout = currentWindowAdaptiveInfo()
        .windowSizeClass
        .toAirShareLayoutProfile(hasLargeWidth = windowSize.width >= AIRSHARE_LARGE_WIDTH_DP.dp)

    LaunchedEffect(state.notice) {
        val message = when (state.notice) {
            UiNotice.WIFI_LOST -> text.wifiLost
            UiNotice.NETWORK_CHANGED -> text.networkChanged
            UiNotice.MEDIA_ACCESS_LOST -> text.accessLost
            null -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onDismissNotice()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val copyAndNotify: (String) -> Unit = { address ->
                onCopyAddress(address)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(text.copied)
                }
            }
            if (layout.useSupportingPane) {
                SupportingPaneContent(
                    state = state,
                    text = text,
                    selectedLanguage = selectedLanguage,
                    permissionAction = permissionAction,
                    layout = layout,
                    scrollState = contentScrollState,
                    privacyDetailsOpen = privacyDetailsOpen,
                    onPrivacyDetailsOpenChange = { privacyDetailsOpen = it },
                    onLanguageSelected = onLanguageSelected,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                    onChangeSelection = onChangeSelection,
                    onStartSharing = onStartSharing,
                    onStopSharing = onStopSharing,
                    onCopyAddress = copyAndNotify,
                )
            } else {
                SinglePaneContent(
                    state = state,
                    text = text,
                    selectedLanguage = selectedLanguage,
                    permissionAction = permissionAction,
                    layout = layout,
                    scrollState = contentScrollState,
                    privacyDetailsOpen = privacyDetailsOpen,
                    onPrivacyDetailsOpenChange = { privacyDetailsOpen = it },
                    onLanguageSelected = onLanguageSelected,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                    onChangeSelection = onChangeSelection,
                    onStartSharing = onStartSharing,
                    onStopSharing = onStopSharing,
                    onCopyAddress = copyAndNotify,
                )
            }
        }
    }
}

@Composable
private fun SinglePaneContent(
    state: AirShareUiState,
    text: AppText,
    selectedLanguage: AppLanguageOption,
    permissionAction: PermissionAction,
    layout: AirShareLayoutProfile,
    scrollState: ScrollState,
    privacyDetailsOpen: Boolean,
    onPrivacyDetailsOpenChange: (Boolean) -> Unit,
    onLanguageSelected: (AppLanguageOption) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeSelection: () -> Unit,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    onCopyAddress: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = layout.maxContentWidth)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(
                start = layout.horizontalPadding,
                end = layout.horizontalPadding,
                top = 18.dp,
                bottom = 32.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header(
            text = text,
            selectedLanguage = selectedLanguage,
            onLanguageSelected = onLanguageSelected,
        )
        PrivacyStatement(
            text = text,
            detailsOpen = privacyDetailsOpen,
            onDetailsOpenChange = onPrivacyDetailsOpenChange,
        )
        StatusSurface(
            state = state,
            text = text,
            permissionAction = permissionAction,
            sessionLayout = layout.sessionLayout,
            qrSize = layout.qrSize,
            onRequestPermission = onRequestPermission,
            onOpenSettings = onOpenSettings,
            onChangeSelection = onChangeSelection,
            onStartSharing = onStartSharing,
            onStopSharing = onStopSharing,
            onCopyAddress = onCopyAddress,
        )
        if (state.shareStatus !is ShareStatus.Active) {
            HowItWorks(text)
        }
    }
}

@Composable
private fun SupportingPaneContent(
    state: AirShareUiState,
    text: AppText,
    selectedLanguage: AppLanguageOption,
    permissionAction: PermissionAction,
    layout: AirShareLayoutProfile,
    scrollState: ScrollState,
    privacyDetailsOpen: Boolean,
    onPrivacyDetailsOpenChange: (Boolean) -> Unit,
    onLanguageSelected: (AppLanguageOption) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeSelection: () -> Unit,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    onCopyAddress: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = layout.maxContentWidth)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(
                start = layout.horizontalPadding,
                end = layout.horizontalPadding,
                top = 24.dp,
                bottom = 40.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(layout.paneGap),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.width(layout.supportingPaneWidth),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Header(
                    text = text,
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = onLanguageSelected,
                )
                PrivacyStatement(
                    text = text,
                    detailsOpen = privacyDetailsOpen,
                    onDetailsOpenChange = onPrivacyDetailsOpenChange,
                )
                if (state.shareStatus !is ShareStatus.Active) {
                    HowItWorks(text)
                }
            }
            StatusSurface(
                state = state,
                text = text,
                permissionAction = permissionAction,
                sessionLayout = layout.sessionLayout,
                qrSize = layout.qrSize,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                onChangeSelection = onChangeSelection,
                onStartSharing = onStartSharing,
                onStopSharing = onStopSharing,
                onCopyAddress = onCopyAddress,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Header(
    text: AppText,
    selectedLanguage: AppLanguageOption,
    onLanguageSelected: (AppLanguageOption) -> Unit,
) {
    var languageMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = Color.White,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.padding(4.dp),
                tint = Color.Unspecified,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text.appName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = text.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(
                onClick = { languageMenuOpen = true },
                modifier = Modifier.semantics { contentDescription = text.languageLabel },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_language),
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = languageMenuOpen,
                onDismissRequest = { languageMenuOpen = false },
            ) {
                AppLanguageOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text.languageOption(option)) },
                        leadingIcon = if (option == selectedLanguage) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            languageMenuOpen = false
                            onLanguageSelected(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyStatement(
    text: AppText,
    detailsOpen: Boolean,
    onDetailsOpenChange: (Boolean) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text.privacyLabel, style = MaterialTheme.typography.titleMedium)
                Text(text.privacyBody, style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = { onDetailsOpenChange(true) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                ) {
                    Text(text.privacyDetailsLabel)
                }
            }
        }
    }
    if (detailsOpen) {
        AlertDialog(
            onDismissRequest = { onDetailsOpenChange(false) },
            title = { Text(text.privacyDetailsLabel) },
            text = { Text(text.privacyDetailsBody) },
            confirmButton = {
                TextButton(onClick = { onDetailsOpenChange(false) }) {
                    Text(text.dismiss)
                }
            },
        )
    }
}

@Composable
private fun StatusSurface(
    state: AirShareUiState,
    text: AppText,
    permissionAction: PermissionAction,
    sessionLayout: AirShareSessionLayout,
    qrSize: Dp,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeSelection: () -> Unit,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    onCopyAddress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when {
                state.mediaAccess == MediaAccess.NONE -> PermissionContent(
                    text = text,
                    permissionAction = permissionAction,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                )

                !state.network.onWifi || state.network.address == null -> WifiContent(text)

                state.shareStatus is ShareStatus.Active -> ActiveContent(
                    session = state.shareStatus,
                    text = text,
                    hasPartialAccess = state.mediaAccess == MediaAccess.PARTIAL,
                    sessionLayout = sessionLayout,
                    qrSize = qrSize,
                    onChangeSelection = onChangeSelection,
                    onCopyAddress = onCopyAddress,
                    onStopSharing = onStopSharing,
                )

                else -> ReadyContent(
                    state = state,
                    text = text,
                    onChangeSelection = onChangeSelection,
                    onStartSharing = onStartSharing,
                )
            }
        }
    }
}

@Composable
private fun PermissionContent(
    text: AppText,
    permissionAction: PermissionAction,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    StatusHeading(
        icon = R.drawable.ic_photo,
        title = text.permissionTitle,
        body = text.permissionBody,
    )
    Button(
        onClick = if (permissionAction == PermissionAction.OPEN_SETTINGS) onOpenSettings else onRequestPermission,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(if (permissionAction == PermissionAction.OPEN_SETTINGS) text.openSettings else text.grantPermission)
    }
}

@Composable
private fun WifiContent(text: AppText) {
    StatusHeading(
        icon = R.drawable.ic_wifi,
        title = text.wifiRequiredTitle,
        body = text.wifiRequiredBody,
    )
}

@Composable
private fun ReadyContent(
    state: AirShareUiState,
    text: AppText,
    onChangeSelection: () -> Unit,
    onStartSharing: () -> Unit,
) {
    StatusHeading(
        icon = R.drawable.ic_play,
        title = text.readyTitle,
        body = text.readyBody,
    )
    if (state.mediaAccess == MediaAccess.PARTIAL) {
        PartialAccessNotice(text, onChangeSelection)
    }
    val error = (state.shareStatus as? ShareStatus.Failed)?.message
    if (error != null) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Text(
                text = error.ifBlank { text.genericError },
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    Button(
        onClick = onStartSharing,
        enabled = state.canStart,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        if (state.shareStatus is ShareStatus.Starting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(10.dp))
            Text(text.starting)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text.startSharing)
        }
    }
}

@Composable
private fun ActiveContent(
    session: ShareStatus.Active,
    text: AppText,
    hasPartialAccess: Boolean,
    sessionLayout: AirShareSessionLayout,
    qrSize: Dp,
    onChangeSelection: () -> Unit,
    onCopyAddress: (String) -> Unit,
    onStopSharing: () -> Unit,
) {
    StatusHeading(
        icon = R.drawable.ic_check,
        title = text.activeTitle,
        body = text.activeBody,
        active = true,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusChip(R.drawable.ic_lock, text.encryptedLabel, Modifier.weight(1f))
        StatusChip(R.drawable.ic_wifi, text.localOnlyLabel, Modifier.weight(1f))
    }
    if (hasPartialAccess) {
        PartialAccessNotice(text, onChangeSelection)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    if (sessionLayout == AirShareSessionLayout.SIDE_BY_SIDE) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SessionQr(
                session = session,
                text = text,
                qrSize = qrSize,
                modifier = Modifier.width(qrSize + 20.dp),
            )
            SessionControls(
                session = session,
                text = text,
                onCopyAddress = onCopyAddress,
                onStopSharing = onStopSharing,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        SessionQr(
            session = session,
            text = text,
            qrSize = qrSize,
            modifier = Modifier.fillMaxWidth(),
        )
        SessionControls(
            session = session,
            text = text,
            onCopyAddress = onCopyAddress,
            onStopSharing = onStopSharing,
        )
    }
}

@Composable
private fun SessionQr(
    session: ShareStatus.Active,
    text: AppText,
    qrSize: Dp,
    modifier: Modifier = Modifier,
) {
    val qrCode = rememberQrCode(session.url)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text.openOnComputer,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier = Modifier.size(qrSize),
            shape = MaterialTheme.shapes.medium,
            color = Color.White,
        ) {
            Image(
                bitmap = qrCode,
                contentDescription = text.openOnComputer,
                modifier = Modifier.padding(10.dp),
                contentScale = ContentScale.Fit,
            )
        }
        SelectionContainer {
            Text(
                text = session.url,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SessionControls(
    session: ShareStatus.Active,
    text: AppText,
    onCopyAddress: (String) -> Unit,
    onStopSharing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = { onCopyAddress(session.url) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_copy),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text.copyAddress)
        }
        Text(
            text = text.keepAwakeHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text.certificateHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${text.fingerprintLabel}: ${session.certificateFingerprint}",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onStopSharing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stop),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text.stopSharing)
        }
    }
}

@Composable
private fun StatusHeading(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    active: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (active) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PartialAccessNotice(text: AppText, onChangeSelection: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text.partialAccessTitle, style = MaterialTheme.typography.titleMedium)
            Text(text.partialAccessBody, style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = onChangeSelection,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text(text.changeSelection)
            }
        }
    }
}

@Composable
private fun StatusChip(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HowItWorks(text: AppText) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text.howItWorks, style = MaterialTheme.typography.titleLarge)
        listOf(text.stepOne, text.stepTwo, text.stepThree).forEachIndexed { index, step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Text(
                    text = step,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
