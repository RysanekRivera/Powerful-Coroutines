package io.github.rysanekrivera.powerfulcoroutines.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ConnectivityObserver  {

    internal val networkAvailability: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isNetworkAvailable = networkAvailability.asStateFlow()
}