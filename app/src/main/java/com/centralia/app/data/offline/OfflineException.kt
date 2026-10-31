package com.centralia.app.data.offline

/**
 * Eventual connectivity: shown when an action needs the server and the phone
 * is offline (network-only features such as saving a new short or signing in).
 */
class OfflineException(message: String) : Exception(message) {
    companion object {
        fun needsConnection(action: String) =
            OfflineException("You're offline. $action needs a connection. Try again when you're back online.")
    }
}
