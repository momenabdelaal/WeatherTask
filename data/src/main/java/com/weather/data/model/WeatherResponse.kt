package com.weather.data.model

import com.google.gson.annotations.SerializedName


data class WeatherResponse(
    @SerializedName("coord") val coordinates: CoordinatesData,
    @SerializedName("weather") val weather: List<WeatherData> = emptyList(),
    @SerializedName("base") val base: String,
    @SerializedName("main") val main: MainData,
    @SerializedName("visibility") val visibility: Int,
    @SerializedName("wind") val wind: WindData,
    @SerializedName("clouds") val clouds: Clouds,
    @SerializedName("dt") val dateTime: Long,
    @SerializedName("sys") val sys: Sys,
    @SerializedName("timezone") val timezone: Int,
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("cod") val cod: Int
) {

    fun hasValidWeatherData(): Boolean =
        coordinates.isValid() &&
                weather.isNotEmpty() && main.isValid() && wind.isValid()
}


data class CoordinatesData(
    @SerializedName("lon") val longitude: Double,
    @SerializedName("lat") val latitude: Double
) {

    fun isValid(): Boolean = latitude in -90.0..90.0 && longitude in -180.0..180.0
}


data class WeatherData(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)


data class MainData(
    @SerializedName("temp") val temperature: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("pressure") val pressure: Int,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("sea_level") val seaLevel: Int?,
    @SerializedName("grnd_level") val groundLevel: Int?
) {
    companion object {
        const val MIN_TEMP = -100.0 // Lowest recorded temperature on Earth is around -89.2°C
        const val MAX_TEMP = 100.0  // Highest recorded temperature on Earth is around 56.7°C
    }
    

    fun isValid(): Boolean =
        // Check temperature ranges
        temperature in MIN_TEMP..MAX_TEMP &&
        feelsLike in MIN_TEMP..MAX_TEMP &&
        tempMin in MIN_TEMP..MAX_TEMP &&
        tempMax in MIN_TEMP..MAX_TEMP &&
        // Ensure min temp is less than max temp
        tempMin <= tempMax &&
        // Check other weather parameters
        humidity in 0..100 && // Humidity is always 0-100%
        pressure > 0 // Pressure should always be positive
}


data class WindData(
    @SerializedName("speed") val speed: Double,
    @SerializedName("deg") val degree: Int,
    @SerializedName("gust") val gust: Double?
) {

    fun isValid(): Boolean = speed >= 0 && degree in 0..360
}


data class Clouds(
    @SerializedName("all") val all: Int
)


data class Sys(
    @SerializedName("type") val type: Int?,
    @SerializedName("id") val id: Int?,
    @SerializedName("country") val country: String,
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long
)
