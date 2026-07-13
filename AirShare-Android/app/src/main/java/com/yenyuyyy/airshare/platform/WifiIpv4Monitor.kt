package com.yenyuyyy.airshare.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.net.Inet4Address
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

sealed interface WifiIpv4State {
    data object Disconnected : WifiIpv4State

    data class Connected(val address: String) : WifiIpv4State
}

class WifiIpv4Monitor(
    private val connectivityManager: ConnectivityManager,
) {
    constructor(context: Context) : this(
        context.applicationContext.getSystemService(ConnectivityManager::class.java),
    )

    fun states(): Flow<WifiIpv4State> = callbackFlow {
        val connections = ConcurrentHashMap<Network, WifiIpv4State.Connected>()

        fun publish() {
            trySend(connections.values.firstOrNull() ?: WifiIpv4State.Disconnected)
        }

        fun update(network: Network, linkProperties: LinkProperties? = null) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
                connections.remove(network)
                publish()
                return
            }

            val properties = linkProperties ?: connectivityManager.getLinkProperties(network)
            val address = properties?.linkAddresses
                ?.asSequence()
                ?.map { it.address }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                ?.hostAddress

            if (address == null) {
                connections.remove(network)
            } else {
                connections[network] = WifiIpv4State.Connected(address)
            }
            publish()
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                update(network)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                update(network)
            }

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: LinkProperties,
            ) {
                update(network, linkProperties)
            }

            override fun onLost(network: Network) {
                connections.remove(network)
                publish()
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        connectivityManager.activeNetwork?.let(::update)
        publish()

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged().conflate()
}
