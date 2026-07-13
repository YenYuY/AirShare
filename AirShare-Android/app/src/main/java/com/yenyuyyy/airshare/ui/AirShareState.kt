package com.yenyuyyy.airshare.ui

enum class MediaAccess {
    NONE,
    PARTIAL,
    FULL,
}

data class NetworkStatus(
    val onWifi: Boolean = false,
    val address: String? = null,
)

sealed interface ShareStatus {
    data object Idle : ShareStatus
    data object Starting : ShareStatus

    data class Active(val url: String) : ShareStatus

    data class Failed(val message: String) : ShareStatus
}

enum class UiNotice {
    WIFI_LOST,
    NETWORK_CHANGED,
    MEDIA_ACCESS_LOST,
}

data class AirShareUiState(
    val mediaAccess: MediaAccess = MediaAccess.NONE,
    val network: NetworkStatus = NetworkStatus(),
    val shareStatus: ShareStatus = ShareStatus.Idle,
    val notice: UiNotice? = null,
) {
    val canStart: Boolean
        get() = mediaAccess != MediaAccess.NONE &&
            network.onWifi &&
            network.address != null &&
            shareStatus !is ShareStatus.Starting &&
            shareStatus !is ShareStatus.Active
}

sealed interface AirShareEvent {
    data object StartTapped : AirShareEvent
    data object StopTapped : AirShareEvent
    data object AppBackgrounded : AirShareEvent
    data object NoticeDismissed : AirShareEvent
    data class MediaAccessChanged(val access: MediaAccess) : AirShareEvent
    data class NetworkChanged(val status: NetworkStatus) : AirShareEvent
    data class ServerStarted(val url: String) : AirShareEvent
    data class ServerFailed(val message: String) : AirShareEvent
    data class StartBlocked(val message: String) : AirShareEvent
}

sealed interface AirShareEffect {
    data class StartServer(val address: String) : AirShareEffect
    data object StopServer : AirShareEffect
}

data class AirShareTransition(
    val state: AirShareUiState,
    val effects: List<AirShareEffect> = emptyList(),
)

object AirShareStateMachine {
    fun reduce(state: AirShareUiState, event: AirShareEvent): AirShareTransition = when (event) {
        AirShareEvent.StartTapped -> {
            val address = state.network.address
            if (!state.canStart || address == null) {
                AirShareTransition(state)
            } else {
                AirShareTransition(
                    state.copy(shareStatus = ShareStatus.Starting, notice = null),
                    effects = listOf(AirShareEffect.StartServer(address)),
                )
            }
        }

        AirShareEvent.StopTapped -> stopIfNeeded(state)
        AirShareEvent.AppBackgrounded -> stopIfNeeded(state)
        AirShareEvent.NoticeDismissed -> AirShareTransition(state.copy(notice = null))

        is AirShareEvent.MediaAccessChanged -> {
            val updated = state.copy(mediaAccess = event.access)
            if (event.access == MediaAccess.NONE && state.shareStatus.isSharing()) {
                AirShareTransition(
                    updated.copy(
                        shareStatus = ShareStatus.Idle,
                        notice = UiNotice.MEDIA_ACCESS_LOST,
                    ),
                    effects = listOf(AirShareEffect.StopServer),
                )
            } else {
                AirShareTransition(updated)
            }
        }

        is AirShareEvent.NetworkChanged -> {
            val updated = state.copy(network = event.status)
            val notice = when {
                !state.shareStatus.isSharing() -> null
                !event.status.onWifi -> UiNotice.WIFI_LOST
                event.status != state.network -> UiNotice.NETWORK_CHANGED
                else -> null
            }
            if (notice != null) {
                AirShareTransition(
                    updated.copy(
                        shareStatus = ShareStatus.Idle,
                        notice = notice,
                    ),
                    effects = listOf(AirShareEffect.StopServer),
                )
            } else {
                AirShareTransition(updated)
            }
        }

        is AirShareEvent.ServerStarted -> {
            if (state.shareStatus is ShareStatus.Starting) {
                AirShareTransition(
                    state.copy(
                        shareStatus = ShareStatus.Active(
                            url = event.url,
                        ),
                        notice = null,
                    ),
                )
            } else {
                AirShareTransition(state, effects = listOf(AirShareEffect.StopServer))
            }
        }

        is AirShareEvent.ServerFailed -> {
            if (state.shareStatus is ShareStatus.Starting) {
                AirShareTransition(state.copy(shareStatus = ShareStatus.Failed(event.message)))
            } else {
                AirShareTransition(state)
            }
        }

        is AirShareEvent.StartBlocked -> AirShareTransition(
            state.copy(shareStatus = ShareStatus.Failed(event.message)),
        )
    }

    private fun stopIfNeeded(state: AirShareUiState): AirShareTransition {
        val effects = if (state.shareStatus.isSharing()) {
            listOf(AirShareEffect.StopServer)
        } else {
            emptyList()
        }
        return AirShareTransition(
            state.copy(shareStatus = ShareStatus.Idle, notice = null),
            effects = effects,
        )
    }

    private fun ShareStatus.isSharing(): Boolean =
        this is ShareStatus.Starting || this is ShareStatus.Active
}
