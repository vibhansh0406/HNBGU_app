# Contributing to CampusOne

Thank you for helping build **CampusOne** for Indian university students!

## Development Setup

1. Clone the repository and open in **Android Studio Ladybug (2024.2.1)+** with **JDK 17**.
2. **Offline Seed Mode**: CampusOne runs out of the box without a `google-services.json` file using the built-in `HnbguSeedData` Room initializer.
3. **Firebase Setup (Optional for Cloud Sync)**:
   - Create a free Firebase Spark project.
   - Enable Google Sign-In in Firebase Authentication and create a Cloud Firestore database.
   - Deploy `firebase/firestore.rules` to your Firestore security rules tab.
   - Place your `google-services.json` inside `app/`.

## Code Quality & Testing Rules

- **Offline-First Guarantee**: Every UI screen must observe Room `Flow`s via `StateFlow` in a `@HiltViewModel`. Never make direct blocking Firestore calls from UI composables.
- **Unit Tests**: Run `./gradlew testDebugUnitTest` to verify `AttendanceCalculatorTest`, `TimetableEngineTest`, and `GpaCalculatorTest` before opening a Pull Request.
- **Bilingual Support**: Add every new user-facing string to both `app/src/main/res/values/strings.xml` (English) and `app/src/main/res/values-hi/strings.xml` (Hindi).
