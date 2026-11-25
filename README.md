# CampusOne

CampusOne is an independent, student-built Android app for students at HNBGU's Birla and Chauras campuses in Srinagar Garhwal, Uttarakhand. It brings academic routines and useful campus services into one place, with local caching for unreliable connectivity.

CampusOne is not developed, operated, approved, or endorsed by Hemvati Nandan Bahuguna Garhwal University.

> **Status:** Active development. The repository contains the Android app and backend source. A successful CI build is not proof that every feature has been tested on every device or that the service is ready for a university-wide rollout.

## Features

- Subject-wise attendance tracking and attendance target guidance.
- Section-based class timetables, next-class information, and reminders.
- Campus and section notices, admin broadcasts, and supported attachments.
- Notes, previous-year papers, syllabus resources, PDF upload, and in-app viewing.
- Exam information, assignments, and GPA calculation.
- Clubs, events, lost and found, and a campus directory.
- English and Hindi interface options.
- Local caching and background synchronization for core academic data.

Feature availability depends on backend configuration, permissions, and the user's assigned access. Sign-in, fresh data, publishing, and uploads require a network connection.

## Technology

| Area | Implementation |
|---|---|
| Platform | Native Android |
| Language | Kotlin |
| UI | Jetpack Compose and Material 3 |
| Dependency injection | Hilt |
| Local database | Room |
| Preferences | DataStore |
| Background work | WorkManager |
| Home screen widget | Glance |
| Authentication and backend | Supabase |
| Database access control | PostgreSQL Row Level Security |
| File storage | Supabase Storage |
| Push notifications | Firebase Cloud Messaging |
| Build | Android Gradle Plugin 8.7.2, Gradle 8.9, JDK 17 |

## Repository layout

| Path | Purpose |
|---|---|
| `CampusOne/` | Android application and Gradle build |
| `supabase/schema.sql` | Database schema reference |
| `supabase/migrations/` | Incremental database changes |
| `supabase/functions/` | Supabase Edge Functions |
| `.github/workflows/` | Android build and verification workflow |
| `PRODUCT.md` | Product scope and constraints |
| `DESIGN.md` | Visual system and interface guidelines |
| `STORAGE-WORKER-SETUP.md` | Optional storage worker setup notes |

## Development setup

### Requirements

- Android Studio with Android SDK 35
- JDK 17
- The Gradle Wrapper included in the project
- A configured Supabase project
- Google OAuth and Firebase configuration for sign-in and push notifications

### Build and test

1. Clone the repository.
2. Open the `CampusOne` directory in Android Studio.
3. Add the required local values to `CampusOne/local.properties`. The Android build file documents the expected Supabase, Google OAuth, and optional service URL properties.
4. Sync the Gradle project.
5. Run these commands from the `CampusOne` directory:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Run the Android database migration tests on an emulator with:

\`\`\`bash
./gradlew connectedDebugAndroidTest
\`\`\`

Build a release APK with:

```bash
./gradlew assembleRelease
```

Release builds require the dedicated signing keystore and credentials. Keep those values outside the repository and do not replace the release signing identity.

## Backend setup

CampusOne uses Supabase for authentication, database access, and study-resource storage.

- Configure Google sign-in in Supabase and in the Android app.
- Review `supabase/schema.sql` for the database structure and access policies.
- Use `supabase/migrations/` to track incremental changes. Review migration order and effects before applying changes to an existing database.
- Deploy the Edge Functions in `supabase/functions/` when the related server-side features are enabled.
- Keep service-role credentials and other server secrets on the server. Never include them in the Android app.

Do not run a full schema snapshot against an existing production database without reviewing its effect first. Use migrations for controlled changes.

## Continuous integration

The GitHub Actions workflow builds a signed release APK and checks the package name, SDK levels, signing schemes, release certificate, APK alignment, and install-sensitive permissions. It also runs the Android unit test task.

The application ID is `in.campusone.app`. It is a permanent project identifier because sign-in configuration and existing installations depend on it.

CI validates the build. It does not replace device testing, backend verification, or release review.

## Security

- Never commit `local.properties`, keystores, passwords, service-role keys, OAuth client secrets, or access tokens.
- The Supabase anonymous key is a client identifier, not an authorization boundary. Database access must remain protected by Row Level Security and server-side checks.
- Do not rely on hidden buttons or client-side role checks to protect privileged operations.
- Keep release signing credentials in GitHub Actions secrets.
- Do not share real student records, account details, or tokens in public issues.

See [SECURITY.md](SECURITY.md) for reporting security concerns.

## Product and design

Read [PRODUCT.md](PRODUCT.md) for product scope and [DESIGN.md](DESIGN.md) for the interface system, layout rules, typography, and colour usage. Changes should follow those decisions rather than introduce a separate visual language.

## Contributions

Before making a large change, open an issue to discuss the problem and proposed approach. Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## Licence

This repository does not currently include an open-source licence. Do not assume that reuse, redistribution, or publication of modified copies is permitted.
