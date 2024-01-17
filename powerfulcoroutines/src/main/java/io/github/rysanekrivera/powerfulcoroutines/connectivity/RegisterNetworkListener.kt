package io.github.rysanekrivera.powerfulcoroutines.connectivity

import android.app.Application
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RegisterNetworkListener {

    private val _isApplicationRegistered = MutableStateFlow(false)
    val isNetworkListenerRegistered = _isApplicationRegistered.asStateFlow()

    operator fun invoke(application: Application) {
        val connectivityManager = application.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkAccessRequest =  NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .addTransportType(TRANSPORT_CELLULAR)
            .addTransportType(TRANSPORT_WIFI)
            .addTransportType(TRANSPORT_ETHERNET)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) networkAccessRequest.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val networkCallbacks = ConnectivityManagerNetworkCallbacks()
        connectivityManager.registerNetworkCallback(networkAccessRequest.build(), networkCallbacks)
        _isApplicationRegistered.value = true
    }

}





