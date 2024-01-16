package io.github.rysanekrivera.powerfulcoroutines.connectivity.errors

class NetworkListenerNotRegistered: Throwable("Network Listener not Registered. Did you forget to call `RegisterNetworkListener(this)` on the application's onCreate")
