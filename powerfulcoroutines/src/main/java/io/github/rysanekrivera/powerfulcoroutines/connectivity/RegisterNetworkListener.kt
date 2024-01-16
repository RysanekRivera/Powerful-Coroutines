package io.github.rysanekrivera.powerfulcoroutines.connectivity

import android.app.Application
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RegisterNetworkListener {

    private val _isApplicationRegistered = MutableStateFlow(false)
    val isNetworkListenerRegistered = _isApplicationRegistered.asStateFlow()

    operator fun invoke(application: Application) {
        val connectivityManager = application.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkAccessRequest = NetworkRequest.Builder().addCapability(NET_CAPABILITY_INTERNET).build()
        val networkCallbacks = ConnectivityManagerNetworkCallbacks()
        connectivityManager.registerNetworkCallback(networkAccessRequest, networkCallbacks)
        _isApplicationRegistered.value = true
    }

}





