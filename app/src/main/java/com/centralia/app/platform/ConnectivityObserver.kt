package com.centralia.app.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.centralia.app.domain.context.NetworkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Eventual connectivity (Sprint 3): tells the app, live, whether the phone is
 * online. Android calls the callback on its own background thread; a StateFlow
 * is safe to update from there and the UI collects it on the main thread.
 *
 * Used by the offline banner, by [com.centralia.app.data.offline.OfflineFirstLibraryRepository]
 * to skip requests that would fail, and to start syncing the outbox the moment
 * the connection comes back. Mirrors `ConnectivityMonitor` in the iOS app.
 */
class ConnectivityObserver(context: Context) {

    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(currentNetworkType(context) != NetworkType.OFFLINE)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        })
    }
}
