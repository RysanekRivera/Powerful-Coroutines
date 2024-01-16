package io.github.rysanekrivera.powerfulcoroutines.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

inline fun CoroutineScope.launchSafe(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart:() -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> Unit
) = launch(
    context,
    start
) {
    runCatching {
        onStart()
        block()
    }.onSuccess {
        onSuccess.invoke()
        finally.invoke()
    }.onFailure { e ->
        e.handleOrRethrow(onError)
        finally.invoke()
    }

}

inline fun <reified T> CoroutineScope.asyncSafe(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline onStart:() -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline finally: () -> Unit = {},
    crossinline block: suspend CoroutineScope.() -> T
): Deferred<T?> = async(context, start) {

    try {
       onStart()
       block()
    } catch (e: Throwable){
        e.handleOrRethrow(onError)
        null
    } finally {
        finally.invoke()
    }

}

inline fun Throwable.handleOrRethrow(
    onHandleException: ((Throwable) -> Unit) = {}
){
    if (this is CancellationException && this !is TimeoutCancellationException) throw this
    else onHandleException.invoke(this)
}

