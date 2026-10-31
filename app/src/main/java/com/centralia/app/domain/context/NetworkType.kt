package com.centralia.app.domain.context

/**
 * Context-aware playback: the kind of network the phone is on right now. It is
 * sent to `GET /v1/videos/{id}/playback?network=…`, and the backend decides how
 * to play the short (on mobile data it turns on data saver). Mirrors
 * `NetworkType` in the iOS app.
 */
enum class NetworkType(val wireValue: String) {
    WIFI("wifi"),
    CELLULAR("cellular"),
    OFFLINE("offline"),
    UNKNOWN("unknown");

    companion object {
        /**
         * Pure rule used by the platform reader: Wi-Fi or Ethernet counts as
         * Wi-Fi even if mobile data is also on; otherwise mobile data is cellular.
         */
        fun from(connected: Boolean, wifiOrEthernet: Boolean, cellular: Boolean): NetworkType = when {
            !connected -> OFFLINE
            wifiOrEthernet -> WIFI
            cellular -> CELLULAR
            else -> UNKNOWN
        }
    }
}
