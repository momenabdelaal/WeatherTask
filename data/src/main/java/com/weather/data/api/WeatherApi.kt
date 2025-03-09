package com.weather.data.api

import com.weather.data.model.WeatherResponse
import com.weather.data.model.ForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = API_KEY,
        @Query("lang") language: String = DEFAULT_LANGUAGE,
        @Query("units") units: String = DEFAULT_UNITS
    ): WeatherResponse

    @GET("weather")
    suspend fun getCurrentWeatherByCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String = API_KEY,
        @Query("lang") language: String = DEFAULT_LANGUAGE,
        @Query("units") units: String = DEFAULT_UNITS
    ): WeatherResponse

    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = API_KEY,
        @Query("lang") language: String = DEFAULT_LANGUAGE,
        @Query("units") units: String = DEFAULT_UNITS,
        @Query("cnt") count: Int = 40 // Get maximum number of timestamps (5 days)
    ): ForecastResponse

    @GET("forecast")
    suspend fun getForecastByCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String = API_KEY,
        @Query("lang") language: String = DEFAULT_LANGUAGE,
        @Query("units") units: String = DEFAULT_UNITS,
        @Query("cnt") count: Int = 40 // Get maximum number of timestamps (5 days)
    ): ForecastResponse

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        // TODO: Move this to BuildConfig or encrypted storage
        const val API_KEY = "d5068f3e8e1687af8cf6645791f5adb8"
        const val DEFAULT_LANGUAGE = "ar"
        const val DEFAULT_UNITS = "metric"
        
        // Validation constants
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
        const val MIN_CITY_LENGTH = 2
        const val MAX_CITY_LENGTH = 50
        
        // Error messages in Arabic
        const val ERROR_INVALID_COORDINATES = "إحداثيات غير صالحة"
        const val ERROR_INVALID_CITY = "اسم المدينة غير صالح"
        const val ERROR_API_KEY = "مفتاح API غير صالح"
        const val ERROR_SERVER = "حدث خطأ في الخادم"
        const val ERROR_NETWORK = "حدث خطأ في الشبكة"
    }
}
