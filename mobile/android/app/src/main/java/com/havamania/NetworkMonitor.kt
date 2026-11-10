package com.havamania

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * İnternet bağlantısını anlık olarak takip eden yardımcı sınıf
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

class ConnectivityManagerNetworkMonitor(
    private val context: Context
) : NetworkMonitor {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override val isOnline: Flow<Boolean> = callbackFlow<Boolean?> {
        val manager = connectivityManager
        if (manager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val observesDefault = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) trySend(null)
            }

            override fun onLost(network: Network) {
                trySend(if (observesDefault) false else null)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(if (observesDefault) capabilities.hasValidatedInternet() else null)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.registerDefaultNetworkCallback(callback)
        } else {
            manager.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                callback
            )
        }
        trySend(null)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        .buffer(kotlinx.coroutines.channels.Channel.CONFLATED)
        .map { event ->
            val manager = connectivityManager
            event ?: (manager?.activeNetwork?.let(manager::getNetworkCapabilities)?.hasValidatedInternet() == true)
        }
        .distinctUntilChanged()

    private fun NetworkCapabilities.hasValidatedInternet() =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
