package io.github.rysanekrivera.powerfulcoroutines.connectivity

import android.net.ConnectivityManager
import android.net.Network
import io.github.rysanekrivera.powerfulcoroutines.connectivity.ConnectivityObserver.networkAvailability

internal class ConnectivityManagerNetworkCallbacks : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        networkAvailability.value = true
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        networkAvailability.value = false
    }
}