# TomatoScan Android App - Agent Guide

## Build & Test Commands
- `./gradlew build` - Build the app
- `./gradlew test` - Run unit tests
- `./gradlew connectedAndroidTest` - Run instrumented tests
- `./gradlew testDebugUnitTest --tests "ClassName.testMethodName"` - Run single test

## Architecture
- **Main Module**: `app/` - Single module Android app
- **Package**: `com.ml.tomatoscan` - Kotlin + Jetpack Compose
- **Backend**: Firebase (Auth, Firestore, Storage) + Google Gemini AI
- **Camera**: CameraX integration for tomato scanning
- **Navigation**: Compose Navigation with screens in `ui/screens/`

## Code Style
- **Language**: Kotlin with Compose
- **Package Structure**: `data/`, `ui/`, `viewmodels/`, `models/`
- **Naming**: PascalCase for classes, camelCase for functions/variables
- **Imports**: Group Android, then third-party, then local
- **Compose**: Use `@Composable` functions, follow Material 3 design
- **Error Handling**: Use sealed classes for states, Firebase exceptions
- **Data**: Repository pattern with Firebase integration
