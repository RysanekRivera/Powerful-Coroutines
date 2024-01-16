package io.github.rysanekrivera.powerfulcoroutines.coroutines

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.rysanekrivera.powerfulcoroutines.connectivity.ConnectivityObserver
import io.github.rysanekrivera.powerfulcoroutines.connectivity.RegisterNetworkListener
import io.github.rysanekrivera.powerfulcoroutines.connectivity.errors.NetworkListenerNotRegistered
import io.github.rysanekrivera.powerfulcoroutines.coroutines.errors.NoNetworkConnectionException
import io.github.rysanekrivera.powerfulcoroutines.request.NetworkRequest
import io.github.rysanekrivera.powerfulcoroutines.request.processResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import retrofit2.Response
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.Throws

@Throws(NoNetworkConnectionException::class, NetworkListenerNotRegistered::class)
inline fun <reified T> CoroutineScope.launchNetworkAwareApiCall(
    flowToReportStates: MutableStateFlow<NetworkRequest>? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart:() -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Response<T>
) = if (RegisterNetworkListener.isNetworkListenerRegistered.value) {
    launch(
        context,
        start
    ) {

        try {

            flowToReportStates?.value = NetworkRequest.Loading

            if (ConnectivityObserver.isNetworkAvailable.value) {
                runCatching {
                    onStart()
                    block().processResponse<T>()
                }.onSuccess {
                    flowToReportStates?.value = it

                    when (it) {
                        is NetworkRequest.Success<*> -> onSuccess()
                        is NetworkRequest.Error -> onError(Error(it.errorBodyString))
                        else -> Unit
                    }

                    finally.invoke()
                }.onFailure { e ->
                    e.handleOrRethrow(onError)
                    flowToReportStates?.value = NetworkRequest.Error(genericError = e)
                    finally.invoke()
                }
            } else {
                throw NoNetworkConnectionException()
            }

        } catch (e: Throwable) {
            e.handleOrRethrow(onError)
            flowToReportStates?.value = NetworkRequest.Error(genericError = e)
        }
    }
} else throw NetworkListenerNotRegistered()


/** Network Waiting calls**/
@Throws(NoNetworkConnectionException::class, NetworkListenerNotRegistered::class)
inline fun <reified T> CoroutineScope.launchNetworkWaitingApiCall(
    context: CoroutineContext = EmptyCoroutineContext,
    flowToReportStates: MutableStateFlow<NetworkRequest>? = null,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart:() -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Response<T>
){
    if (!RegisterNetworkListener.isNetworkListenerRegistered.value) throw NetworkListenerNotRegistered()

    launch(
        context,
        start
    ) {

        try {

            if (ConnectivityObserver.isNetworkAvailable.value) {
                launchNetworkAwareApiCall(flowToReportStates, context, start, onStart, onSuccess, onError, finally, block)
            } else {

                flowToReportStates?.value = NetworkRequest.WaitingForNetwork

                ConnectivityObserver.isNetworkAvailable.collect { isAvailable ->
                    when(isAvailable) {
                        true -> {

                            launchNetworkAwareApiCall (
                                flowToReportStates, context, start, onStart, onSuccess, onError,
                                finally = {
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

@Throws(NoNetworkConnectionException::class, NetworkListenerNotRegistered::class)
inline fun <reified T> LifecycleOwner.launchNetworkWaitingApiCall(
    flowToReportStates: MutableStateFlow<NetworkRequest>? = null,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart:() -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Response<T>
){
    if (!RegisterNetworkListener.isNetworkListenerRegistered.value) throw NetworkListenerNotRegistered()

    lifecycleScope.launch(
        context,
        start
    ) {

        try {

            if (ConnectivityObserver.isNetworkAvailable.value) {
                launchNetworkAwareApiCall(flowToReportStates, context, start, onStart, onSuccess, onError, finally, block)
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

                            launchNetworkAwareApiCall (
                                flowToReportStates, context, start, onStart, onSuccess, onError,
                                finally = {
                                    lifecycle.removeObserver(lifecycleObserver)
                                    finally()
                                    coroutineContext.job.cancel()
                                },
                                block = block
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

@Throws(NoNetworkConnectionException::class, NetworkListenerNotRegistered::class)
inline fun <reified T>LifecycleOwner.launchNetworkWaitingApiCallRepeatOnLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
    flowToReportStates: MutableStateFlow<NetworkRequest>? = null,
    lifecycleStateToRepeat: Lifecycle.State = Lifecycle.State.STARTED,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart: () -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Response<T>
) {

    lifecycleScope.launch(
        context,
        start
    ) {
        repeatOnLifecycle(lifecycleStateToRepeat) {
            this@launchNetworkWaitingApiCallRepeatOnLifecycle.launchNetworkWaitingApiCall(
                flowToReportStates = flowToReportStates,
                context = context,
                start = start,
                onStart = onStart,
                onSuccess = onSuccess,
                onError = onError,
                finally = finally,
                block = block
            )
        }
    }
}

