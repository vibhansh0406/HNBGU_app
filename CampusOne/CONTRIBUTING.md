# Contributing to CampusOne

Thank you for helping build **CampusOne** for Indian university students!

## Development Setup

1. Clone the repository and open in **Android Studio Ladybug (2024.2.1)+** with **JDK 17**.
2. **Offline Seed Mode**: CampusOne runs out of the box without a `google-services.json` file using the built-in `HnbguSeedData` Room initializer.
3. **Supabase Setup (Cloud Sync)**:
   - Create a free Supabase project.
   - Run `supabase/schema.sql` in the Supabase SQL Editor to create tables, roles, permissions, and RLS policies.
   - Add `SUPABASE_URL` and `SUPABASE_ANON_KEY` to `local.properties` (or CI secrets) — these are public client config only.
   - Configure Google Sign-In in Supabase Authentication and add your Android client (package `in.campusone.app` + the pinned debug keystore SHA-1) to the Google Cloud OAuth client.
   - No `google-services.json` is required; the app runs in offline seed mode via the built-in `HnbguSeedData` Room initializer.

## Code Quality & Testing Rules

- **Offline-First Guarantee**: Every UI screen must observe Room `Flow`s via `StateFlow` in a `@HiltViewModel`. Never make direct blocking Firestore calls from UI composables.
- **Unit Tests**: Run `./gradlew testDebugUnitTest` to verify `AttendanceCalculatorTest`, `TimetableEngineTest`, and `GpaCalculatorTest` before opening a Pull Request.
- **Bilingual Support**: Add every new user-facing string to both `app/src/main/res/values/strings.xml` (English) and `app/src/main/res/values-hi/strings.xml` (Hindi).
