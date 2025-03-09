package com.weather.utils.error

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.internal.http2.StreamResetException

sealed class NetworkError : Exception() {
    data class NoInternet(override val message: String) : NetworkError()
    data class Timeout(override val message: String) : NetworkError()
    data class ServerError(override val message: String) : NetworkError()
    data class ApiError(
        val code: Int,
        override val message: String
    ) : NetworkError()
    data class ValidationError(override val message: String) : NetworkError()
    data class PermissionDenied(override val message: String) : NetworkError()
    data class Unknown(override val message: String) : NetworkError()

    companion object {
        fun from(error: Throwable): NetworkError = when (error) {
            is UnknownHostException -> NoInternet("لا يوجد اتصال بالإنترنت")
            is SocketTimeoutException -> Timeout("انتهت مهلة الاتصال")
            is StreamResetException -> ServerError("خطأ في الخادم")
            is HttpError -> ApiError(error.code, "خطأ في الاتصال")
            is SecurityException -> PermissionDenied("تم رفض إذن الوصول للموقع")
            is IllegalArgumentException -> ValidationError("خطأ في البيانات المدخلة")
            is IOException -> ServerError(error.message ?: "خطأ في الخادم")
            else -> Unknown("حدث خطأ غير متوقع")
        }
    }
}
