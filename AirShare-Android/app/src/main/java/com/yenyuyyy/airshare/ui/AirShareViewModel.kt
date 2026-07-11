package com.yenyuyyy.airshare.ui

import android.app.Application
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yenyuyyy.airshare.language.AppLanguage
import com.yenyuyyy.airshare.language.AppLanguageOption
import com.yenyuyyy.airshare.language.AppText
import com.yenyuyyy.airshare.language.LanguagePreferences
import com.yenyuyyy.airshare.language.LocalizedWebStrings
import com.yenyuyyy.airshare.language.textFor
import com.yenyuyyy.airshare.media.MediaStoreMediaCatalog
import com.yenyuyyy.airshare.platform.AndroidPermissionReader
import com.yenyuyyy.airshare.platform.MediaPermissionAccess
import com.yenyuyyy.airshare.platform.WifiIpv4Monitor
import com.yenyuyyy.airshare.platform.WifiIpv4State
import com.yenyuyyy.airshare.server.AirShareTlsServer
import java.net.InetAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LanguageUiState(
    val option: AppLanguageOption,
    val language: AppLanguage,
    val text: AppText,
)

class AirShareViewModel(application: Application) : AndroidViewModel(application) {
    private val permissionReader = AndroidPermissionReader(application)
    private val mediaCatalog = MediaStoreMediaCatalog(application.contentResolver)
    private val wifiMonitor = WifiIpv4Monitor(application)
    private val languagePreferences = LanguagePreferences(application)
    private val webStrings = LocalizedWebStrings(AppLanguageOption.AUTO.resolve())

    private val _uiState = MutableStateFlow(AirShareUiState())
    val uiState: StateFlow<AirShareUiState> = _uiState.asStateFlow()

    private val initialLanguage = AppLanguageOption.AUTO.resolve()
    private val _languageState = MutableStateFlow(
        LanguageUiState(
            option = AppLanguageOption.AUTO,
            language = initialLanguage,
            text = textFor(initialLanguage),
        ),
    )
    val languageState: StateFlow<LanguageUiState> = _languageState.asStateFlow()

    private val serverLifecycle = ServerLifecycleCoordinator()

    init {
        viewModelScope.launch {
            languagePreferences.languageOption.collect(::applyLanguageOption)
        }
        viewModelScope.launch {
            wifiMonitor.states().collect { status ->
                val network = when (status) {
                    WifiIpv4State.Disconnected -> NetworkStatus()
                    is WifiIpv4State.Connected -> NetworkStatus(
                        onWifi = true,
                        address = status.address,
                    )
                }
                dispatch(AirShareEvent.NetworkChanged(network))
            }
        }
        refreshPermissions()
    }

    fun requiredMediaPermissions(): Array<String> = permissionReader.mediaPermissionsToRequest()

    fun requiredLocalNetworkPermissions(): Array<String> =
        permissionReader.localNetworkPermissionsToRequest()

    fun hasLocalNetworkAccess(): Boolean = permissionReader.hasLocalNetworkAccess()

    fun refreshPermissions() {
        val access = when (permissionReader.mediaAccess()) {
            MediaPermissionAccess.NONE -> MediaAccess.NONE
            MediaPermissionAccess.PARTIAL -> MediaAccess.PARTIAL
            MediaPermissionAccess.FULL -> MediaAccess.FULL
        }
        dispatch(AirShareEvent.MediaAccessChanged(access))
    }

    fun refreshSystemLanguage() {
        if (_languageState.value.option == AppLanguageOption.AUTO) {
            applyLanguageOption(AppLanguageOption.AUTO)
        }
    }

    fun setLanguageOption(option: AppLanguageOption) {
        applyLanguageOption(option)
        viewModelScope.launch {
            languagePreferences.setLanguageOption(option)
        }
    }

    fun startSharing() = dispatch(AirShareEvent.StartTapped)

    fun stopSharing() = dispatch(AirShareEvent.StopTapped)

    fun onAppBackgrounded() = dispatch(AirShareEvent.AppBackgrounded)

    fun dismissNotice() = dispatch(AirShareEvent.NoticeDismissed)

    fun showStartBlocked(message: String) = dispatch(AirShareEvent.StartBlocked(message))

    private fun applyLanguageOption(option: AppLanguageOption) {
        val language = option.resolve(LocaleList.getDefault())
        webStrings.update(language)
        _languageState.value = LanguageUiState(
            option = option,
            language = language,
            text = textFor(language),
        )
    }

    private fun dispatch(event: AirShareEvent) {
        val transition = AirShareStateMachine.reduce(_uiState.value, event)
        _uiState.value = transition.state
        transition.effects.forEach(::handleEffect)
    }

    private fun handleEffect(effect: AirShareEffect) {
        when (effect) {
            is AirShareEffect.StartServer -> {
                val generation = serverLifecycle.beginStart()
                viewModelScope.launch {
                    try {
                        val started = withContext(Dispatchers.IO) {
                            startServer(effect.address, generation)
                        }
                        if (!serverLifecycle.isCurrentStart(generation)) return@launch
                        dispatch(
                            AirShareEvent.ServerStarted(
                                url = started.url,
                                certificateFingerprint = started.fingerprint,
                            ),
                        )
                    } catch (_: ServerStartCancelledException) {
                        // A stop or newer start owns the state now.
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        val fallback = _languageState.value.text.genericError
                        val detail = error.message?.takeIf(String::isNotBlank)
                        dispatch(
                            AirShareEvent.ServerFailed(
                                if (detail == null) fallback else "$fallback $detail",
                            ),
                        )
                    }
                }
            }

            AirShareEffect.StopServer -> {
                val stopGeneration = serverLifecycle.cancelPendingStarts()
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching { serverLifecycle.stopThrough(stopGeneration) }
                }
            }
        }
    }

    private suspend fun startServer(address: String, generation: Long): StartedSession {
        val bindAddress = InetAddress.getByName(address)
        return serverLifecycle.start(
            generation = generation,
            create = {
                AirShareTlsServer(
                    catalog = mediaCatalog,
                    accessToken = AirShareTlsServer.generateAccessToken(),
                    strings = webStrings,
                    bindAddress = bindAddress,
                )
            },
            start = { server ->
                val endpoint = server.start()
                StartedSession(
                    url = (endpoint.boundSessionUrl ?: endpoint.sessionUrl(bindAddress)).toString(),
                    fingerprint = endpoint.certificateSha256,
                )
            },
        )
    }

    override fun onCleared() {
        serverLifecycle.closeNow()
        super.onCleared()
    }

    private data class StartedSession(
        val url: String,
        val fingerprint: String,
    )
}
