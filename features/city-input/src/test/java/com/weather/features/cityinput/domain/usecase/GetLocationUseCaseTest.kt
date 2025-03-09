package com.weather.features.cityinput.domain.usecase

import com.weather.core.location.LocationResult
import com.weather.core.location.LocationService
import com.weather.utils.error.ErrorHandler
import com.weather.utils.error.LocationValidationError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetLocationUseCaseTest {
    private lateinit var useCase: GetLocationUseCase
    private lateinit var locationService: LocationService
    private lateinit var errorHandler: ErrorHandler

    companion object {
        private const val TEST_LATITUDE = 51.5074
        private const val TEST_LONGITUDE = -0.1278
        private const val TEST_CITY = "London"
    }

    @Before
    fun setup() {
        locationService = mockk()
        errorHandler = mockk()
        useCase = GetLocationUseCase(locationService, errorHandler)
    }

    @Test
    fun `when location service returns success with valid coordinates, should return success`() = runTest {
        // Given
        val expectedResult = LocationResult.Success(TEST_LATITUDE, TEST_LONGITUDE, TEST_CITY)
        coEvery { locationService.getCurrentLocation() } returns expectedResult

        // When
        val result = useCase()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when location service returns zero coordinates, should return validation error`() = runTest {
        // Given
        val errorMessage = "إحداثيات غير صالحة"
        every { errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES) } returns errorMessage
        coEvery { locationService.getCurrentLocation() } returns LocationResult.Success(0.0, 0.0, "")

        // When
        val result = useCase()

        // Then
        assertEquals(LocationResult.Error.ServiceError(errorMessage), result)
    }

    @Test
    fun `when location service returns permission denied, should return permission error`() = runTest {
        // Given
        val expectedResult = LocationResult.Error.PermissionDenied
        coEvery { locationService.getCurrentLocation() } returns expectedResult

        // When
        val result = useCase()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when location service returns location disabled, should return location disabled error`() = runTest {
        // Given
        val expectedResult = LocationResult.Error.LocationDisabled
        coEvery { locationService.getCurrentLocation() } returns expectedResult

        // When
        val result = useCase()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when location service throws exception, should return service error`() = runTest {
        // Given
        val exception = Exception("Test error")
        val errorMessage = "خطأ في خدمة الموقع"
        coEvery { locationService.getCurrentLocation() } throws exception
        every { errorHandler.getLocationError(exception) } returns errorMessage

        // When
        val result = useCase()

        // Then
        assertEquals(LocationResult.Error.ServiceError(errorMessage), result)
    }
}
