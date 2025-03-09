package com.weather.core.model

sealed interface LocationState {
    val latitude: Double?
    val longitude: Double?
    val cityName: String?
    val isAvailable: Boolean
    val asAvailable: Available?

    interface Available {
        val latitude: Double
        val longitude: Double
        val cityName: String
    }
}

sealed class LocationStateImpl : LocationState {
    data class Available(
        override val latitude: Double,
        override val longitude: Double,
        override val cityName: String
    ) : LocationStateImpl(), LocationState.Available {
        override val isAvailable: Boolean = true
        override val asAvailable: LocationState.Available = this
    }

    data object Unavailable : LocationStateImpl() {
        override val latitude: Double? = null
        override val longitude: Double? = null
        override val cityName: String? = null
        override val isAvailable: Boolean = false
        override val asAvailable: Available? = null
    }

    data object Loading : LocationStateImpl() {
        override val latitude: Double? = null
        override val longitude: Double? = null
        override val cityName: String? = null
        override val isAvailable: Boolean = false
        override val asAvailable: Available? = null
    }

    companion object {
        val DEFAULT = Available(
            latitude = 30.0444, // Cairo's coordinates as default
            longitude = 31.2357,
            cityName = "القاهرة" // Cairo in Arabic
        )

        fun loading(): LocationStateImpl = Loading
        fun unavailable(): LocationStateImpl = Unavailable
        fun available(latitude: Double, longitude: Double, cityName: String): LocationStateImpl = 
            Available(latitude, longitude, cityName)
    }

    abstract override val isAvailable: Boolean
    abstract override val asAvailable: LocationState.Available?
}
