package com.yenyuyyy.airshare.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirShareStateMachineTest {
    @Test
    fun `start requests the server only when media and Wi-Fi are ready`() {
        val ready = AirShareUiState(
            mediaAccess = MediaAccess.FULL,
            network = NetworkStatus(onWifi = true, address = "192.168.1.24"),
        )

        val transition = AirShareStateMachine.reduce(ready, AirShareEvent.StartTapped)

        assertTrue(transition.state.shareStatus is ShareStatus.Starting)
        assertEquals(
            listOf(AirShareEffect.StartServer(address = "192.168.1.24")),
            transition.effects,
        )
    }

    @Test
    fun `start is ignored while media access is missing`() {
        val blocked = AirShareUiState(
            mediaAccess = MediaAccess.NONE,
            network = NetworkStatus(onWifi = true, address = "192.168.1.24"),
        )

        val transition = AirShareStateMachine.reduce(blocked, AirShareEvent.StartTapped)

        assertEquals(blocked, transition.state)
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun `losing Wi-Fi stops an active session and explains why`() {
        val active = AirShareUiState(
            mediaAccess = MediaAccess.FULL,
            network = NetworkStatus(onWifi = true, address = "192.168.1.24"),
            shareStatus = ShareStatus.Active(
                url = "http://192.168.1.24:8080/?token=test",
            ),
        )

        val transition = AirShareStateMachine.reduce(
            active,
            AirShareEvent.NetworkChanged(NetworkStatus()),
        )

        assertEquals(ShareStatus.Idle, transition.state.shareStatus)
        assertEquals(UiNotice.WIFI_LOST, transition.state.notice)
        assertEquals(listOf(AirShareEffect.StopServer), transition.effects)
    }

    @Test
    fun `changing Wi-Fi address stops an active session bound to the previous address`() {
        val active = AirShareUiState(
            mediaAccess = MediaAccess.FULL,
            network = NetworkStatus(onWifi = true, address = "192.168.1.24"),
            shareStatus = ShareStatus.Active(
                url = "http://192.168.1.24:8080/?token=test",
            ),
        )

        val transition = AirShareStateMachine.reduce(
            active,
            AirShareEvent.NetworkChanged(
                NetworkStatus(onWifi = true, address = "192.168.2.18"),
            ),
        )

        assertEquals(ShareStatus.Idle, transition.state.shareStatus)
        assertEquals(UiNotice.NETWORK_CHANGED, transition.state.notice)
        assertEquals(listOf(AirShareEffect.StopServer), transition.effects)
    }

    @Test
    fun `losing media access stops an active session`() {
        val active = AirShareUiState(
            mediaAccess = MediaAccess.PARTIAL,
            network = NetworkStatus(onWifi = true, address = "192.168.1.24"),
            shareStatus = ShareStatus.Active(
                url = "http://192.168.1.24:8080/?token=test",
            ),
        )

        val transition = AirShareStateMachine.reduce(
            active,
            AirShareEvent.MediaAccessChanged(MediaAccess.NONE),
        )

        assertEquals(ShareStatus.Idle, transition.state.shareStatus)
        assertEquals(UiNotice.MEDIA_ACCESS_LOST, transition.state.notice)
        assertEquals(listOf(AirShareEffect.StopServer), transition.effects)
    }

    @Test
    fun `backgrounding always tears down the server`() {
        val active = AirShareUiState(
            mediaAccess = MediaAccess.FULL,
            network = NetworkStatus(onWifi = true, address = "192.168.1.24"),
            shareStatus = ShareStatus.Starting,
        )

        val transition = AirShareStateMachine.reduce(active, AirShareEvent.AppBackgrounded)

        assertEquals(ShareStatus.Idle, transition.state.shareStatus)
        assertEquals(listOf(AirShareEffect.StopServer), transition.effects)
    }

    @Test
    fun `a server that finishes after cancellation is stopped instead of reactivated`() {
        val idle = AirShareUiState(
            mediaAccess = MediaAccess.FULL,
            network = NetworkStatus(onWifi = true, address = "192.168.1.24"),
        )

        val transition = AirShareStateMachine.reduce(
            idle,
            AirShareEvent.ServerStarted(
                url = "http://192.168.1.24:8080/?token=late",
            ),
        )

        assertEquals(ShareStatus.Idle, transition.state.shareStatus)
        assertEquals(listOf(AirShareEffect.StopServer), transition.effects)
    }
}
