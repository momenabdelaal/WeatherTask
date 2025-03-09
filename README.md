# Weather Now & Later
An Android application that provides current weather information and future forecasts using clean architecture and modern architectural patterns.

## Features
- Search cities and view current weather conditions
- Display 7-day weather forecast
- Save last searched city using DataStore
- Dark mode support
- Full Arabic localization and error messages

## Architecture

### Architectural Patterns
- **City Input Screen**: MVVM with StateFlow
- **Current Weather Display**: MVVM with StateFlow
- **Forecast List**: MVI with Side Effects

### Clean Architecture
```
app/                    # Main Application
├── core/              # Shared Components
│   ├── datastore/    # DataStore Implementation
│   ├── location/     # Location Services
│   └── network/     # Network Components
│
├── data/             # Data Layer
│   ├── remote/      # OpenWeather API Integration
│   ├── model/       # Data Models
│   └── repository/  # Repositories
│
├── features/         # Features
│   ├── city-input/   # City Input (MVVM)
│   ├── current-weather/ # Current Weather (MVVM)
│   └── forecast/    # Forecast (MVI)
│
└── weather-utils/   # Error Handling & Theme
    ├── error/      # Centralized Error Handling
    └── theme/     # Theme Configuration
```

## Tech Stack
- **UI**: Jetpack Compose
- **Dependency Injection**: Dagger Hilt
- **Data Processing**: Kotlin Coroutines + Flow
- **Local Storage**: DataStore
- **Networking**: Retrofit + OkHttp
- **Unit Testing**: JUnit + MockK
- **UI Testing**: Compose Testing

## Code Quality
- Test coverage exceeding 80%
- SOLID principles implementation
- Clean Code practices
- Code formatting with ktlint

## CI/CD Pipeline

### Branch Strategy
- `master`: Production-ready code
  * Protected branch
  * Requires test passing
  * Generates debug APK
- `development`: Integration branch
  * Feature branches merge here
  * Automated tests run
  * Debug APK generated

### Automated Pipeline
1. **Test Stage**
   - Runs City Input module tests
   - Generates test reports
   - Uploads test results as artifacts
   ```bash
   ./gradlew :features:city-input:test
   ```

2. **Build Stage**
   - Requires successful test stage
   - Configures JDK 17 environment
   - Sets up Weather API key securely
   - Builds debug APK
   ```bash
   ./gradlew assembleDebug
   ```

### Artifacts
- Test Results
  * Available for 7 days
  * Located in `features/city-input/build/reports/tests/`
- Build Artifacts
  * Debug APK
  * Build reports
  * Feature module reports
  * 7-day retention period

### Environment Setup
- Ubuntu latest runner
- JDK 17 (Temurin distribution)
- Gradle build action
- Secure secrets management for API keys

## Development

### Requirements
- Android Studio Hedgehog | 2023.1.1
- JDK 17
- Kotlin 2.0.0
- Gradle 8.2.2

### Getting Started
1. Get an API key from OpenWeatherMap
2. Add the key to `local.properties`:
   ```properties
   WEATHER_API_KEY=your_api_key_here
   ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

### Custom Library (weather-utils)
A custom library for error handling and theming:
- Centralized error handling
- Arabic error messages
- Network error handling
- Location permission handling
- Theme configuration
- Dark mode support

Publishing the library:
```bash
./gradlew :weather-utils:publishToMavenLocal
```

## Contributing
1. Fork the repository
2. Create your feature branch
3. Make your changes
4. Ensure tests pass and code is linted
5. Submit a pull request

## Error Handling
### Centralized Error System (weather-utils)
1. **HttpError**
   - Custom exception class for API errors
   - Handles common HTTP status codes (401, 404, 429, 500, 502, 503, 504)
   - Independent of Retrofit implementation

2. **NetworkError**
   - Sealed class hierarchy for network-related errors
   - NoInternet: For connectivity issues
   - Timeout: For request timeouts
   - ServerError: For server-side issues
   - ApiError: For HTTP-specific errors
   - Unknown: For unhandled cases

3. **ErrorHandler**
   - Provides user-friendly Arabic error messages
   - Handles location, weather, and forecast-specific errors
   - Includes validation error handling
   - Centralizes all error message strings in R.string resources
