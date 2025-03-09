package com.weather.features.currentweather.domain.model

import com.weather.data.model.MainData


data class WeatherPressureModel(
    val pressure: Int,
    val seaLevel: Int?,
    val groundLevel: Int?
) {
    companion object {
        fun fromMain(main: MainData): WeatherPressureModel {
            return WeatherPressureModel(
                pressure = main.pressure,
                seaLevel = main.seaLevel,
                groundLevel = main.groundLevel
            )
        }
    }
}
