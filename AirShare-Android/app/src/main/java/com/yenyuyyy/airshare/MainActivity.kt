package com.yenyuyyy.airshare

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.PersistableBundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenyuyyy.airshare.ui.AirShareScreen
import com.yenyuyyy.airshare.ui.AirShareViewModel
import com.yenyuyyy.airshare.ui.MediaAccess
import com.yenyuyyy.airshare.ui.PermissionAction
import com.yenyuyyy.airshare.ui.ShareStatus
import com.yenyuyyy.airshare.ui.theme.AirShareTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AirShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val languageState by viewModel.languageState.collectAsStateWithLifecycle()
            var permissionRequestReturned by rememberSaveable { mutableStateOf(false) }
            val canRequestMediaAgain = viewModel.requiredMediaPermissions()
                .any(::shouldShowRequestPermissionRationale)

            val mediaPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) {
                permissionRequestReturned = true
                viewModel.refreshPermissions()
            }
            val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { grants ->
                if (grants.values.all { it } && viewModel.hasLocalNetworkAccess()) {
                    viewModel.startSharing()
                } else {
                    viewModel.showStartBlocked(languageState.text.localNetworkDenied)
                }
            }

            KeepScreenAwake(uiState.shareStatus is ShareStatus.Active)

            AirShareTheme {
                AirShareScreen(
                    state = uiState,
                    text = languageState.text,
                    selectedLanguage = languageState.option,
                    permissionAction = if (
                        permissionRequestReturned &&
                        uiState.mediaAccess == MediaAccess.NONE &&
                        !canRequestMediaAgain
                    ) {
                        PermissionAction.OPEN_SETTINGS
                    } else {
                        PermissionAction.REQUEST
                    },
                    onLanguageSelected = viewModel::setLanguageOption,
                    onRequestPermission = {
                        mediaPermissionLauncher.launch(viewModel.requiredMediaPermissions())
                    },
                    onOpenSettings = ::openAppSettings,
                    onChangeSelection = {
                        mediaPermissionLauncher.launch(viewModel.requiredMediaPermissions())
                    },
                    onStartSharing = {
                        if (viewModel.hasLocalNetworkAccess()) {
                            viewModel.startSharing()
                        } else {
                            val permissions = viewModel.requiredLocalNetworkPermissions()
                            if (permissions.isEmpty()) {
                                viewModel.showStartBlocked(languageState.text.localNetworkDenied)
                            } else {
                                localNetworkPermissionLauncher.launch(permissions)
                            }
                        }
                    },
                    onStopSharing = viewModel::stopSharing,
                    onCopyAddress = ::copyAddress,
                    onDismissNotice = viewModel::dismissNotice,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.refreshSystemLanguage()
    }

    override fun onStop() {
        viewModel.onAppBackgrounded()
        super.onStop()
    }

    private fun copyAddress(address: String) {
        val clip = ClipData.newPlainText("AirShare address", address)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    @androidx.compose.runtime.Composable
    private fun KeepScreenAwake(enabled: Boolean) {
        DisposableEffect(enabled) {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                if (enabled) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }
}
