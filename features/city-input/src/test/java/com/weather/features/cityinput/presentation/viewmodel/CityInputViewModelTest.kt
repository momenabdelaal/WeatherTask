package com.weather.features.cityinput.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import com.weather.core.datastore.WeatherDataStore
import com.weather.core.location.LocationPermissionHelper
import com.weather.core.location.LocationResult
import com.weather.core.model.LocationState
import com.weather.core.model.LocationStateImpl
import com.weather.data.model.CoordinatesData
import com.weather.data.model.WeatherResponse
import com.weather.features.cityinput.domain.model.CityInputEvent
import com.weather.features.cityinput.domain.model.CityInputState
import com.weather.features.cityinput.domain.usecase.GetLocationUseCase
import com.weather.features.cityinput.domain.usecase.SearchCityUseCase
import com.weather.utils.error.ErrorHandler
import com.weather.utils.error.LocationValidationError
import com.weather.utils.error.NetworkError
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CityInputViewModelTest {
    private lateinit var viewModel: CityInputViewModel
    private lateinit var getLocationUseCase: GetLocationUseCase
    private lateinit var searchCityUseCase: SearchCityUseCase
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private lateinit var errorHandler: ErrorHandler
    private lateinit var dataStore: WeatherDataStore
    private lateinit var savedStateHandle: SavedStateHandle
    private val testDispatcher = TestCoroutineDispatcher()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val TEST_CITY = "London"
        private const val TEST_LATITUDE = 51.5074
        private const val TEST_LONGITUDE = -0.1278
        private const val KEY_LATITUDE = "last_latitude"
        private const val KEY_LONGITUDE = "last_longitude"
        private const val KEY_CITY_NAME = "last_city_name"
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getLocationUseCase = mockk()
        searchCityUseCase = mockk()
        locationPermissionHelper = mockk(relaxed = true)
        errorHandler = mockk()
        dataStore = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()

        // Mock dataStore methods with null initial state
        coEvery { dataStore.saveLastLocation(any()) } returns Result.success(Unit)
       // every { dataStore.getLastLocation() } returns flowOf(null)

        // Mock error handler with Arabic error messages
        every { errorHandler.getForecastError(any()) } returns "خطأ في التنبؤ"
        every { errorHandler.getLocationError(any<SecurityException>()) } returns "يرجى منح إذن الموقع"
        every { errorHandler.getLocationError(any<IllegalStateException>()) } returns "خدمات الموقع معطلة"
        every { errorHandler.getLocationValidationError(any()) } returns "اسم المدينة مطلوب"

        viewModel = CityInputViewModel(
            getLocationUseCase,
            searchCityUseCase,
            locationPermissionHelper,
            errorHandler,
            dataStore,
            savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `initial state should be Initial when no saved location exists`() = runTest {
        assertEquals(CityInputState.Initial, viewModel.uiState.value)
    }

    @Test
    fun `should restore location from SavedStateHandle`() = runTest {
        // Given
        savedStateHandle[KEY_LATITUDE] = TEST_LATITUDE
        savedStateHandle[KEY_LONGITUDE] = TEST_LONGITUDE
        savedStateHandle[KEY_CITY_NAME] = TEST_CITY

        // When
        viewModel = CityInputViewModel(
            getLocationUseCase,
            searchCityUseCase,
            locationPermissionHelper,
            errorHandler,
            dataStore,
            savedStateHandle
        )

        // Then
        assertEquals(
            CityInputState.Success(
                LocationStateImpl.Available(TEST_LATITUDE, TEST_LONGITUDE, TEST_CITY)
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `should restore location from DataStore when SavedStateHandle is empty`() = runTest {
        // Given
        val savedLocation = LocationStateImpl.Available(TEST_LATITUDE, TEST_LONGITUDE, TEST_CITY)
        every { dataStore.getLastLocation() } returns flowOf(savedLocation)
        coEvery { dataStore.saveLastLocation(any()) } returns Result.success(Unit)

        // When
        viewModel = CityInputViewModel(
            getLocationUseCase,
            searchCityUseCase,
            locationPermissionHelper,
            errorHandler,
            dataStore,
            savedStateHandle
        )

        // Then
        assertEquals(CityInputState.Success(savedLocation), viewModel.uiState.value)
    }

    @Test
    fun `when searching empty city name, should emit validation error state`() = runTest {
        // Given
        val emptyQuery = ""
        val errorMessage = "اسم المدينة مطلوب test"
        every { errorHandler.getLocationValidationError(LocationValidationError.EMPTY_LOCATION) } returns errorMessage

        // When
        viewModel.handleEvent(CityInputEvent.SearchCity(emptyQuery))

        // Then
        assertEquals(CityInputState.ValidationError(errorMessage), viewModel.uiState.value)
    }

    @Test
    fun `when searching valid city, should emit success state`() = runTest {
        // Given
        val weather = mockk<WeatherResponse>(relaxed = true) {
            every { coordinates } returns CoordinatesData(TEST_LONGITUDE, TEST_LATITUDE)
            every { name } returns TEST_CITY
        }

        coEvery { searchCityUseCase(TEST_CITY) } returns flowOf(Result.success(weather))

        // When
        viewModel.handleEvent(CityInputEvent.SearchCity(TEST_CITY))

        // Then
        coVerify(exactly = 1) { searchCityUseCase(TEST_CITY) }
        coVerify(exactly = 1) {
            dataStore.saveLastLocation(match { location ->
                location is LocationStateImpl.Available &&
                location.latitude == TEST_LATITUDE &&
                location.longitude == TEST_LONGITUDE &&
                location.cityName == TEST_CITY
            })
        }
        assertEquals(
            CityInputState.Success(
                LocationStateImpl.Available(TEST_LATITUDE, TEST_LONGITUDE, TEST_CITY)
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `when searching city fails with network error, should emit error state`() = runTest {
        // Given
        val errorMessage = "لا يوجد اتصال بالإنترنت"
        val networkError = NetworkError.NoInternet(message = errorMessage)
        coEvery { searchCityUseCase(TEST_CITY) } returns flowOf(Result.failure(networkError))
        every { errorHandler.getForecastError(networkError) } returns errorMessage

        // When
        viewModel.handleEvent(CityInputEvent.SearchCity(TEST_CITY))

        // Then
        coVerify(exactly = 1) { searchCityUseCase(TEST_CITY) }
        assertEquals(CityInputState.Error(errorMessage), viewModel.uiState.value)
    }

    @Test
    fun `when getting current location succeeds, should emit success state`() = runTest {
        // Given
        val locationResult = LocationResult.Success(
            latitude = TEST_LATITUDE,
            longitude = TEST_LONGITUDE,
            cityName = TEST_CITY
        )
        coEvery { getLocationUseCase() } returns locationResult

        // When
        viewModel.handleEvent(CityInputEvent.GetCurrentLocation)

        // Then
        coVerify(exactly = 1) { getLocationUseCase() }
        coVerify(exactly = 1) {
            dataStore.saveLastLocation(match { location ->
                location is LocationStateImpl.Available &&
                location.latitude == TEST_LATITUDE &&
                location.longitude == TEST_LONGITUDE &&
                location.cityName == TEST_CITY
            })
        }
        assertEquals(
            CityInputState.Success(
                LocationStateImpl.Available(TEST_LATITUDE, TEST_LONGITUDE, TEST_CITY)
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `when requesting location permission, should delegate to helper`() = runTest {
        // Given
        val activity = mockk<Activity>(relaxed = true)

        // When
        viewModel.handleEvent(CityInputEvent.RequestLocationPermission(activity))

        // Then
        verify(exactly = 1) {
            locationPermissionHelper.requestLocationPermission(activity, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    @Test
    fun `when location permission denied, should emit permission required state`() = runTest {
        // Given
        val locationResult = LocationResult.Error.PermissionDenied
        val errorMessage = "يرجى منح إذن الموقع"
        coEvery { getLocationUseCase() } returns locationResult
        every { errorHandler.getLocationError(any<SecurityException>()) } returns errorMessage

        // When
        viewModel.handleEvent(CityInputEvent.GetCurrentLocation)

        // Then
        coVerify(exactly = 1) { getLocationUseCase() }
        assertEquals(CityInputState.PermissionRequired(errorMessage), viewModel.uiState.value)
    }

    @Test
    fun `when location disabled, should emit location disabled state`() = runTest {
        // Given
        val locationResult = LocationResult.Error.LocationDisabled
        val errorMessage = "خدمات الموقع معطلة"
        coEvery { getLocationUseCase() } returns locationResult
        every { errorHandler.getLocationError(any<IllegalStateException>()) } returns errorMessage

        // When
        viewModel.handleEvent(CityInputEvent.GetCurrentLocation)

        // Then
        coVerify(exactly = 1) { getLocationUseCase() }
        assertEquals(CityInputState.LocationDisabled(errorMessage), viewModel.uiState.value)
    }

    @Test
    fun `when saving location fails, should emit error state`() = runTest {
        // Given
        val weather = mockk<WeatherResponse>(relaxed = true) {
            every { coordinates } returns CoordinatesData(TEST_LONGITUDE, TEST_LATITUDE)
            every { name } returns TEST_CITY
        }
        val errorMessage = "خطأ في حفظ الموقع"
        val saveError = Exception("Save failed")

        coEvery { searchCityUseCase(TEST_CITY) } returns flowOf(Result.success(weather))
        coEvery { dataStore.saveLastLocation(any()) } returns Result.failure(saveError)
        every { errorHandler.getForecastError(saveError) } returns errorMessage

        // When
        viewModel.handleEvent(CityInputEvent.SearchCity(TEST_CITY))

        // Then
        coVerify(exactly = 1) { searchCityUseCase(TEST_CITY) }
        coVerify(exactly = 1) { dataStore.saveLastLocation(any()) }
        assertEquals(CityInputState.Error(errorMessage), viewModel.uiState.value)
    }

    @Test
    fun `when getting current location fails with permission denied, should emit permission required state`() = runTest {
        // Given
        val errorMessage = "يرجى منح إذن الموقع"
        coEvery { getLocationUseCase() } returns LocationResult.Error.PermissionDenied
        every { errorHandler.getLocationError(any<SecurityException>()) } returns errorMessage

        // When
        viewModel.handleEvent(CityInputEvent.GetCurrentLocation)

        // Then
        assertEquals(CityInputState.PermissionRequired(errorMessage), viewModel.uiState.value)
    }

    @Test
    fun `when getting current location fails with location disabled, should emit location disabled state`() = runTest {
        // Given
        val errorMessage = "خدمات الموقع معطلة"
        coEvery { getLocationUseCase() } returns LocationResult.Error.LocationDisabled
        every { errorHandler.getLocationError(any<IllegalStateException>()) } returns errorMessage

        // When
        viewModel.handleEvent(CityInputEvent.GetCurrentLocation)

        // Then
        assertEquals(CityInputState.LocationDisabled(errorMessage), viewModel.uiState.value)
    }

    @Test
    fun `when requesting location permission, should call locationPermissionHelper`() = runTest {
        // Given
        val activity = mockk<Activity>(relaxed = true)

        // When
        viewModel.handleEvent(CityInputEvent.RequestLocationPermission(activity))

        // Then
        verify(exactly = 1) { locationPermissionHelper.requestLocationPermission(activity, LOCATION_PERMISSION_REQUEST_CODE) }
    }

    @Test
    fun `when opening location settings, should call locationPermissionHelper`() = runTest {
        // Given
        val activity = mockk<Activity>(relaxed = true)

        // When
        viewModel.handleEvent(CityInputEvent.OpenLocationSettings(activity))

        // Then
        verify(exactly = 1) { locationPermissionHelper.openLocationSettings(activity) }
    }

    @Test
    fun `when opening app settings, should call locationPermissionHelper`() = runTest {
        // Given
        val activity = mockk<Activity>(relaxed = true)

        // When
        viewModel.openAppSettings(activity)

        // Then
        verify(exactly = 1) { locationPermissionHelper.openAppSettings(activity) }
    }

    @Test
    fun `when selecting city, should update location state and save to DataStore`() = runTest {
        // Given
        val cityName = "Cairo"
        val latitude = 30.0444
        val longitude = 31.2357

        // When
        viewModel.handleEvent(CityInputEvent.SelectCity(latitude, longitude, cityName))

        // Then
        coVerify(exactly = 1) {
            dataStore.saveLastLocation(match { location ->
                location is LocationStateImpl.Available &&
                location.latitude == latitude &&
                location.longitude == longitude &&
                location.cityName == cityName
            })
        }
        assertEquals(
            CityInputState.Success(
                LocationStateImpl.Available(latitude, longitude, cityName)
            ),
            viewModel.uiState.value
        )
    }
}
