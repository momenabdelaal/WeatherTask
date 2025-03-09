package com.weather.utils.error

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.internal.http2.StreamResetException

sealed class NetworkError : Exception() {
    data class NoInternet(override val message: String? = null) : NetworkError()
    data class Timeout(override val message: String? = null) : NetworkError()
    data class ServerError(override val message: String? = null) : NetworkError()
    data class ApiError(
        val code: Int,
        override val message: String? = null
    ) : NetworkError()
    data class Unknown(override val message: String? = null) : NetworkError()

    companion object {
        fun from(error: Throwable): NetworkError = when (error) {
            is UnknownHostException -> NoInternet()
            is SocketTimeoutException -> Timeout()
            is StreamResetException -> ServerError()
            is HttpError -> ApiError(error.code)
            is IOException -> ServerError(error.message)
            else -> Unknown()
        }
    }
}
