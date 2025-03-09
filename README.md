# Weather Now & Later
An Android application that provides current weather information and future forecasts using clean architecture and modern architectural patterns.

## Features
- Search cities and view current weather conditions
- Display 7-day weather forecast
- Save last searched city
- Dark mode support
- Full Arabic localization

## Architecture

### Architectural Patterns
- **City Input Screen**: MVVM
- **Current Weather Display**: MVVM
- **Forecast List**: MVI

### Clean Architecture
```
app/                    # Main Application
├── core/              # Shared Components
│   ├── common/       # Common Utilities
│   ├── ui/          # UI Components
│   └── network/     # Network Components
│
├── data/             # Data Layer
│   ├── remote/      # Remote Data Source
│   ├── local/       # Local Storage
│   └── repository/  # Repositories
│
├── features/         # Features
│   ├── city-input/   # City Input (MVVM)
│   ├── current-weather/ # Current Weather (MVVM)
│   └── forecast/    # Forecast (MVI)
│
└── weather-utils/   # Custom Weather Formatting Library
```

## Tech Stack
- **UI**: Jetpack Compose
- **Dependency Injection**: Dagger Hilt
- **Data Processing**: Kotlin Coroutines + Flow
- **Local Storage**: Room Database
- **Networking**: Retrofit + OkHttp
- **Unit Testing**: JUnit + Mockito/MockK
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
  * Requires PR approval
  * Auto-generates release APK
  * Tagged for releases
- `development`: Integration branch
  * Feature branches merge here
  * Automated tests run
  * Debug APK generated

### Automated Checks
1. **Code Quality**
   ```bash
   ./gradlew ktlintCheck     # Code style
   ./gradlew lint           # Android lint
   ```

2. **Testing**
   ```bash
   ./gradlew test          # Unit tests
   ```

3. **Build Verification**
   - Debug APK for development
   - Release APK for master

### CI/CD Steps
1. **Code Verification**
   - Kotlin style checks
   - Android lint analysis
   - Build validation

2. **Automated Tests**
   - Unit tests
   - Test reports generated
   - Coverage reports

3. **Build Process**
   - Debug APK (development)
   - Release APK (master)
   - Build artifacts stored

4. **Release Process**
   - Automatic versioning
   - GitHub release creation
   - APK artifact upload

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
A custom library for weather data processing:
- Temperature formatting
- Weather code to icon conversion
- Error handling
- Arabic localization support

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
