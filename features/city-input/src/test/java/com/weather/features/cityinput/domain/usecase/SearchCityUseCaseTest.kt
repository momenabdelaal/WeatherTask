package com.weather.features.cityinput.domain.usecase

import com.weather.data.api.WeatherApi
import com.weather.data.model.CoordinatesData
import com.weather.data.model.WeatherResponse
import com.weather.data.repository.WeatherRepository
import com.weather.utils.error.ErrorHandler
import com.weather.utils.error.LocationValidationError
import com.weather.utils.error.NetworkError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchCityUseCaseTest {
    private lateinit var useCase: SearchCityUseCase
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var errorHandler: ErrorHandler

    companion object {
        private const val TEST_CITY = "London"
        private const val TEST_LATITUDE = 51.5074
        private const val TEST_LONGITUDE = -0.1278
    }

    @Before
    fun setup() {
        weatherRepository = mockk()
        errorHandler = mockk()
        useCase = SearchCityUseCase(weatherRepository, errorHandler)
    }

    @Test
    fun `when city name is empty, should return validation error`() = runTest {
        // Given
        val errorMessage = "اسم المدينة مطلوب"
        every { errorHandler.getLocationValidationError(LocationValidationError.EMPTY_LOCATION) } returns errorMessage

        // When
        val result = useCase("").first()

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as NetworkError.ValidationError
        assertEquals(errorMessage, error.message)
    }

    @Test
    fun `when city name is blank, should return validation error`() = runTest {
        // Given
        val errorMessage = "اسم المدينة مطلوب"
        every { errorHandler.getLocationValidationError(LocationValidationError.EMPTY_LOCATION) } returns errorMessage

        // When
        val result = useCase("   ").first()

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as NetworkError.ValidationError
        assertEquals(errorMessage, error.message)
    }

    @Test
    fun `when repository returns success, should return weather data`() = runTest {
        // Given
        val weather = mockk<WeatherResponse> {
            every { coordinates } returns CoordinatesData(TEST_LONGITUDE, TEST_LATITUDE)
            every { name } returns TEST_CITY
        }
        coEvery { 
            weatherRepository.getCurrentWeatherByCity(
                cityName = TEST_CITY,
                language = WeatherApi.DEFAULT_LANGUAGE
            )
        } returns flowOf(Result.success(weather))

        // When
        val result = useCase(TEST_CITY).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(weather, result.getOrNull())
    }

    @Test
    fun `when repository returns network error, should propagate error`() = runTest {
        // Given
        val networkError = NetworkError.NoInternet("لا يوجد اتصال بالإنترنت")
        coEvery { 
            weatherRepository.getCurrentWeatherByCity(
                cityName = TEST_CITY,
                language = WeatherApi.DEFAULT_LANGUAGE
            )
        } returns flowOf(Result.failure(networkError))

        // When
        val result = useCase(TEST_CITY).first()

        // Then
        assertTrue(result.isFailure)
        assertEquals(networkError, result.exceptionOrNull())
    }

    @Test
    fun `when repository throws security exception, should return permission denied error`() = runTest {
        // Given
        val exception = SecurityException("Permission denied")
        val errorMessage = "يرجى منح إذن الموقع"
        coEvery { 
            weatherRepository.getCurrentWeatherByCity(
                cityName = TEST_CITY,
                language = WeatherApi.DEFAULT_LANGUAGE
            )
        } returns flowOf(Result.failure(exception))
        every { errorHandler.getLocationError(exception) } returns errorMessage

        // When
        val result = useCase(TEST_CITY).first()

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as NetworkError.PermissionDenied
        assertEquals(errorMessage, error.message)
    }

    @Test
    fun `when repository throws unknown exception, should return unknown error`() = runTest {
        // Given
        val exception = RuntimeException("Unknown error")
        val errorMessage = "خطأ غير معروف"
        coEvery { 
            weatherRepository.getCurrentWeatherByCity(
                cityName = TEST_CITY,
                language = WeatherApi.DEFAULT_LANGUAGE
            )
        } returns flowOf(Result.failure(exception))
        every { errorHandler.getForecastError(exception) } returns errorMessage

        // When
        val result = useCase(TEST_CITY).first()

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as NetworkError.Unknown
        assertEquals(errorMessage, error.message)
    }
}
