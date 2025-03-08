pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WeatherTask"
include(":app")
include(":core")
include(":data")
include(":weather-utils")
include(":features:city-input")
include(":features:current-weather")
include(":features:forecast")
