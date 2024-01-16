package com.example.network.request

import android.util.Log
import androidx.compose.runtime.Composable
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.internal.http2.Header
import okhttp3.internal.toHeaderList
import retrofit2.Response

data class ResponseData(
    val code: Int = 0,
    val isSuccessful: Boolean = false,
    val message: String? = null,
    val body: String? = null,
    val errorBody: String? = null,
    val rawResponse: okhttp3.Response? = null,
    val headers: List<Header> = emptyList(),
) {
    override fun toString(): String {
        return """
        code: $code
        isSuccessful: $isSuccessful
        message: $message
        body: $body
        errorBody: $errorBody
        headers: $headers
        rawResponse: $rawResponse
      """.trimIndent()
    }
}

fun NetworkRequest.emitValue(flow: MutableStateFlow<NetworkRequest>) { flow.value = this }

fun <T> Response<T>.saveResponseData() = ResponseData(
    code = code(),
    isSuccessful = isSuccessful,
    body = try {
        Gson().toJson(body())
    } catch (e: Throwable) {
        null
    },
    errorBody = errorBody()?.string(),
    message = message().toString(),
    rawResponse = raw(),
    headers = headers().toHeaderList(),
)

inline fun <reified T> Response<T>.processResponse(): NetworkRequest = if (isSuccessful) NetworkRequest.Success(this) else NetworkRequest.Error(saveResponseData())

inline fun <reified T> Response<T>.processResponseAndEmitTo(flow: MutableStateFlow<NetworkRequest>) { flow.value = processResponse() }

inline fun <reified T> Response<T>.processResponseAsJsonString(): String? = if (isSuccessful) Gson().toJson(body()) else errorBody()?.string()

fun String.getJsonBodyElementValue(element: String): String = Gson().fromJson(this, JsonObject::class.java)[element].asString

inline fun <reified E> String.convertErrorBodyToObject(): E? = try {
    Gson().fromJson(this, E::class.java)
} catch (e: Throwable) {
    Log.e("NetworkResponse.Error", "${e.message}")
    null
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> NetworkRequest.handleNetworkRequest(
    onIdle: () -> Unit = {},
    onLoading: () -> Unit = {},
    onWaitingForNetwork: () -> Unit = {},
    onSuccess: (NetworkRequest.Success<T>) -> Unit = {},
    onError: (NetworkRequest.Error) -> Unit = {}
) {
    when(this) {
        is NetworkRequest.Idle -> onIdle.invoke()
        is NetworkRequest.Loading -> onLoading.invoke()
        is NetworkRequest.WaitingForNetwork -> onWaitingForNetwork.invoke()
        is NetworkRequest.Success<*> -> onSuccess(this as NetworkRequest.Success<T>)
        is NetworkRequest.Error -> { onError(this) }
    }
}

@Composable
@Suppress("UNCHECKED_CAST")
inline fun <reified T> NetworkRequest.HandleNetworkRequest(
    onIdle: @Composable () -> Unit = {},
    onLoading: @Composable () -> Unit = {},
    onWaitingForNetwork: @Composable () -> Unit = {},
    onSuccess: @Composable (NetworkRequest.Success<T>) -> Unit = {},
    onError: @Composable (NetworkRequest.Error) -> Unit = {}
) {
    when(this) {
        is NetworkRequest.Idle -> onIdle.invoke()
        is NetworkRequest.Loading -> onLoading.invoke()
        is NetworkRequest.WaitingForNetwork -> onWaitingForNetwork.invoke()
        is NetworkRequest.Success<*> -> onSuccess(this as NetworkRequest.Success<T>)
        is NetworkRequest.Error -> { onError(this) }
    }
}
