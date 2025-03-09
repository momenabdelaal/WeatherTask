# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-03-09

### Added
- Initial release with core features:
  * City search with location support
  * Current weather display with Arabic localization
  * 7-day weather forecast using MVI pattern
  * Dark mode support
  * Offline caching of last searched city

### Architecture
- Clean Architecture implementation
- MVVM for City Input and Current Weather
- MVI for Forecast feature
- Centralized error handling in weather-utils
- Modular project structure:
  * :app - Main application
  * :core - Shared components
  * :data - Data layer
  * :weather-utils - Weather utilities
  * :features - Feature modules

### Technical
- Kotlin 2.0.0
- Jetpack Compose for UI
- Hilt for dependency injection
- Retrofit for networking
- Room for local storage
- Coroutines and Flow
- GitHub Actions CI/CD

### Testing
- Unit tests for ViewModels
- UI tests for critical flows
- Error handling tests
- Repository tests

### Documentation
- README.md with project overview
- Development guide
- Module-specific documentation
- API documentation

### Localization
- Full Arabic language support
- RTL layout support
- Arabic error messages
- Arabic weather descriptions

### CI/CD
- Automated testing
- Code quality checks
- Build verification
- Release management
