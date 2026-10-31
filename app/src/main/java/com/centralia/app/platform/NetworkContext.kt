package com.centralia.app.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.centralia.app.domain.context.NetworkType

/**
 * Context-aware playback: reads which network the phone is on right now from
 * Android's ConnectivityManager (needs ACCESS_NETWORK_STATE, a normal
 * permission granted at install). The iOS app does the same with
 * NWPathMonitor (`NetworkContext.swift`).
 */
fun currentNetworkType(context: Context): NetworkType {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return NetworkType.UNKNOWN
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
    return NetworkType.from(
        connected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        wifiOrEthernet = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true,
        cellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    )
}
