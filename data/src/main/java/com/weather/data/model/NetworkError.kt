package com.weather.data.model

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.internal.http2.StreamResetException

sealed class NetworkError : Exception() {
    data class NoInternet(override val message: String = "تأكد من اتصالك بالإنترنت") : NetworkError()
    data class Timeout(override val message: String = "انتهت مهلة الاتصال") : NetworkError()
    data class ServerError(override val message: String = "حدث خطأ في الخادم") : NetworkError()
    data class ApiError(
        val code: Int,
        override val message: String = when (code) {
            HttpError.UNAUTHORIZED -> "خطأ في مفتاح API"
            HttpError.NOT_FOUND -> "لم يتم العثور على الموقع"
            HttpError.TOO_MANY_REQUESTS -> "تم تجاوز حد الطلبات"
            HttpError.INTERNAL_SERVER_ERROR,
            HttpError.BAD_GATEWAY,
            HttpError.SERVICE_UNAVAILABLE,
            HttpError.GATEWAY_TIMEOUT -> "خطأ في خادم الطقس"
            else -> "حدث خطأ غير متوقع"
        }
    ) : NetworkError()
    data class Unknown(override val message: String = "حدث خطأ غير متوقع") : NetworkError()

    companion object {
        fun from(error: Throwable): NetworkError = when (error) {
            is UnknownHostException -> NoInternet()
            is SocketTimeoutException -> Timeout()
            is StreamResetException -> ServerError()
            is HttpError -> ApiError(error.code)
            is IOException -> ServerError(error.message ?: "حدث خطأ في الخادم")
            else -> Unknown()
        }
    }
}
