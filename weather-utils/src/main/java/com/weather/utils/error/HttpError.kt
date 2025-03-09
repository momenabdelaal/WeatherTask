package com.weather.utils.error

/**
 * Custom HTTP error class to handle API errors without Retrofit dependency
 */
class HttpError(val code: Int, override val message: String? = null) : Exception(message) {
    companion object {
        const val UNAUTHORIZED = 401
        const val NOT_FOUND = 404
        const val TOO_MANY_REQUESTS = 429
        const val INTERNAL_SERVER_ERROR = 500
        const val BAD_GATEWAY = 502
        const val SERVICE_UNAVAILABLE = 503
        const val GATEWAY_TIMEOUT = 504
    }
}
