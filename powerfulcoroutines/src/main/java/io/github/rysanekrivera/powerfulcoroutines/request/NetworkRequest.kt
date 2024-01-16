package io.github.rysanekrivera.powerfulcoroutines.request

import com.google.gson.Gson
import retrofit2.Response

sealed class NetworkRequest{

    data object Idle: NetworkRequest()

    data object Loading : NetworkRequest()

    data object WaitingForNetwork : NetworkRequest()

    data class Success<T>(val response: Response<T>): NetworkRequest() {

        val responseData: ResponseData?

        init {
            responseData = response.saveResponseData()
        }

        val data: T? = response.body()

        val successBodyJsonString = try {
            Gson().toJson(data)
        } catch (e: Throwable) {
            null
        }

    }

    data class Error(val responseData: ResponseData? = null, val genericError: Throwable? = null): NetworkRequest() {

      val errorBodyString = """
      ${responseData?.let {"responseData:\n$it" } ?: ""}
      ${genericError?.let {"genericError:\n$it" } ?: ""}
    """.trimIndent()

    }

}

