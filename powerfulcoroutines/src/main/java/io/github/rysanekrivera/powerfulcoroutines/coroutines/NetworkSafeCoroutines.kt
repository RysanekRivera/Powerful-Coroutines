package io.github.rysanekrivera.powerfulcoroutines.coroutines

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.rysanekrivera.powerfulcoroutines.connectivity.ConnectivityObserver
import io.github.rysanekrivera.powerfulcoroutines.connectivity.RegisterNetworkListener
import io.github.rysanekrivera.powerfulcoroutines.connectivity.RegisterNetworkListener.isNetworkListenerRegistered
import io.github.rysanekrivera.powerfulcoroutines.connectivity.errors.NetworkListenerNotRegistered
import io.github.rysanekrivera.powerfulcoroutines.coroutines.errors.NoNetworkConnectionException
import io.github.rysanekrivera.powerfulcoroutines.request.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * This is a version on launch that will run if the network is available and if not it will throw
 * [NoNetworkConnectionException]. This requires the [RegisterNetworkListener] to be registered
 * on the application:
 * ```
 * class MyApplication : Application() {
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         RegisterNetworkListener(this)
 *     }
 * }
 *  ```
 *
 *  Skipping this step will result in throwing [NetworkListenerNotRegistered] exception.
 *
 *  The use case is that if there is no network it will throw [NoNetworkConnectionException] but
 *  catch it and emit it in the [onError] and you can decide what happens. This is intended for
 *  events where, waiting for the network to come back before re-launching, is not a strict
 *  necessity.
 * **/
@Throws(NetworkListenerNotRegistered::class, NoNetworkConnectionException::class)
inline fun CoroutineScope.launchNetworkAware(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart:() -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Unit
) = if (isNetworkListenerRegistered.value) {
    launch(
        context,
        start
    ) {

        try {

            if (ConnectivityObserver.isNetworkAvailable.value) {
                launchSafe(context, start, onStart, onSuccess, onError, finally, block)
            } else {
                throw NoNetworkConnectionException()
            }

        } catch (e: Throwable) {

            e.handleOrRethrow(onError)

        }
    }
} else throw NetworkListenerNotRegistered()

/**
 * This is a version on async that will run if the network is available and if not it will throw
 * [NoNetworkConnectionException]. This requires the [RegisterNetworkListener] to be registered
 * on the application:
 * ```
 * class MyApplication : Application() {
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         RegisterNetworkListener(this)
 *     }
 * }
 *  ```
 *
 *  Skipping this step will result in throwing [NetworkListenerNotRegistered] exception.
 *
 *  The use case is that if there is no network it will throw [NoNetworkConnectionException] but
 *  catch it and emit it in the [onError] and you can decide what happens. This is intended for
 *  events where, waiting for the network to come back before re-launching, is not a strict
 *  necessity.
 * **/
@Throws(NetworkListenerNotRegistered::class, NoNetworkConnectionException::class)
inline fun <T> CoroutineScope.asyncNetworkAware(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart: () -> Unit,
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> T
): Deferred<T?> = if (isNetworkListenerRegistered.value) {
    async(context, start) {

        try {
            if (ConnectivityObserver.isNetworkAvailable.value) {
                onStart()
                block()
            } else throw NoNetworkConnectionException()
        } catch (e: Throwable) {
            e.handleOrRethrow(onError)
            null
        } finally {
            finally.invoke()
        }

    }
} else throw NetworkListenerNotRegistered()

/**
 * This is a version on launch that will run if the network is available and if not it will throw
 * [NoNetworkConnectionException]. This requires the [RegisterNetworkListener] to be registered
 * on the application:
 * ```
 * class MyApplication : Application() {
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         RegisterNetworkListener(this)
 *     }
 * }
 *  ```
 *
 *  Skipping this step will result in throwing [NetworkListenerNotRegistered] exception.
 *
 *  If there is no network it will report the state [NetworkRequest.WaitingForNetwork]
 *  in which you can inform the user that the network signal is weak and we are waiting for the
 *  network to re-establish connection or be stronger. This is lifecycle aware as long as
 *  the user remains on that screen the call will resume right when the internet connection returns.
 *  This lifecycle observer and coroutine will get unregistered and cancelled if the user leaves the
 *  screen or exits the app.
 *
 *  This use case is for strictly necessary network calls. The coroutine will resume operations
 *  right when the internet connection returns.
 * **/
@Throws(NetworkListenerNotRegistered::class, NoNetworkConnectionException::class)
inline fun LifecycleOwner.launchNetworkWaiting(
    flowToReportStates: MutableStateFlow<NetworkRequest>? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart:() -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Unit
){
    if (!isNetworkListenerRegistered.value) throw NetworkListenerNotRegistered()

    lifecycleScope.launch(
        context,
        start
    ) {

        try {

            if (ConnectivityObserver.isNetworkAvailable.value) {

                flowToReportStates?.value = NetworkRequest.Loading

                launchNetworkAware(context, start, onStart, onSuccess, onError, finally, block)
            } else {

                flowToReportStates?.value = NetworkRequest.WaitingForNetwork

                val lifecycleObserver = object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        super.onStop(owner)

                        lifecycle.removeObserver(this)

                        coroutineContext.job.cancel()
                    }
                }

                lifecycle.addObserver(lifecycleObserver)

                ConnectivityObserver.isNetworkAvailable.collect { isAvailable ->
                    when(isAvailable) {
                        true -> {

                            flowToReportStates?.value = NetworkRequest.Loading

                            launchNetworkAware (
                                context,
                                start,
                                onStart,
                                onSuccess,
                                onError,
                                finally = {
                                    lifecycle.removeObserver(lifecycleObserver)
                                    finally()
                                    coroutineContext.job.cancel()
                                },
                                block
                            )
                        }
                        else -> Unit

                    }
                }
            }
        } catch (e: Throwable) {
            e.handleOrRethrow(onError)
        }
    }
}

inline fun LifecycleOwner.launchNetworkWaitingRepeatOnLifecycle(
    flowToReportResult: MutableStateFlow<NetworkRequest>? = null,
    lifecycleStateToRepeat: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart: () -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Unit
) {

    lifecycleScope.launch(
        context,
        start
    ) {
        repeatOnLifecycle(lifecycleStateToRepeat) {
            launchNetworkWaiting(
                flowToReportResult,
                context,
                start,
                onStart,
                onSuccess,
                onError,
                finally,
                block
            )
        }
    }
}

inline fun <reified T> LifecycleOwner.asyncNetworkWaiting(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    delay: Long = 500,
    cancelTimeout: Long = 30000,
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> T
): Deferred<T?> = if (isNetworkListenerRegistered.value) {
    lifecycleScope.async(context, start) {

        try {
            if (ConnectivityObserver.isNetworkAvailable.value) {
                block()
            } else {

                val lifecycleObserver = object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        super.onStop(owner)
                        lifecycle.removeObserver(this)
                        coroutineContext.job.cancel()
                    }
                }

                lifecycle.addObserver(lifecycleObserver)

                val startTime = System.currentTimeMillis()

                while (!ConnectivityObserver.isNetworkAvailable.value) {
                    val timeDiff = System.currentTimeMillis() - startTime

                    if (timeDiff >= cancelTimeout) {
                        lifecycle.removeObserver(lifecycleObserver)
                        coroutineContext.job.cancel()
                    }

                    delay(delay)

                }

                lifecycle.removeObserver(lifecycleObserver)

                block()

            }
        } catch (e: Throwable) {
            e.handleOrRethrow(onError)
            null
        } finally {
            finally.invoke()
        }
    }
} else throw NetworkListenerNotRegistered()



