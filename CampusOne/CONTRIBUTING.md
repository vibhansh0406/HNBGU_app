# Contributing to CampusOne

Thank you for helping build **CampusOne** for Indian university students!

## Development Setup

1. Clone the repository and open in **Android Studio Ladybug (2024.2.1)+** with **JDK 17**.
2. CampusOne runs in local/demo mode. Profiles are not authenticated or verified; updates and section roles are local to the device.
3. Room contains the seeded campus notices and data. Picked study files are stored in app-local storage; remote uploads, account sync, and live messaging are not available.

## Code Quality & Testing Rules

- **Offline-First Guarantee**: Persist app data in Room or DataStore and expose it to Compose through a `@HiltViewModel`.
- **Unit Tests**: Run `./gradlew testDebugUnitTest` to verify `AttendanceCalculatorTest`, `TimetableEngineTest`, and `GpaCalculatorTest` before opening a Pull Request.
- **Bilingual Support**: Add every new user-facing string to both `app/src/main/res/values/strings.xml` (English) and `app/src/main/res/values-hi/strings.xml` (Hindi).
