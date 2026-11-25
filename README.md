# CampusOne

CampusOne is a campus management application for HNB Garhwal University (HNBGU) students, faculty, and administrators. It provides a unified platform for academic resources, attendance tracking, timetables, notices, assignments, exams, syllabus tracking, campus events, clubs, lost & found, and campus directory services.

## Core Features

### Student Features
- **Dashboard**: Personalized greeting, year/semester info, next class preview, attendance summary, today's schedule, announcements, upcoming deadlines
- **Attendance**: Overall and subject-level attendance with percentages, 4-week rhythm heatmap, attendance simulator, subject-level marking
- **Timetable**: Today's view and weekly view with current/upcoming/completed class states, subject, timing, room details
- **Notices**: Institution-wide announcements with categories, priorities, pinned/official flags, search and filter
- **Notes & PYQ Vault**: Academic resource library with category/subject/semester filtering, search, offline download, upvoting, bookmarking
- **Assignments & Submissions**: Deadline tracking with pending/overdue/completed states, priority indicators, completion tracking
- **Exams & Syllabus**: Exam schedule with dates/venues/rooms, syllabus topic tracking with completion progress
- **Community**: Campus feed with posts, comments, likes, reporting, and moderation
- **Events & Clubs**: Verified campus events with RSVP, approved societies with follow/unfollow
- **Lost & Found**: Item reporting with category/location, claim requests with private contact protocol
- **Campus Directory**: University offices, faculty, and support contacts with search, category filtering, emergency contacts
- **Notifications**: Dedicated notifications page with unread/read states, time-based grouping
- **Profile & Settings**: Academic profile editing, language preference (English/Hindi), notification controls, no theme switch (light theme only)

### Super Admin Features (Owner-only)
- **Overview Dashboard**: Live metrics from backend data (users, notices, resources, events, reports)
- **User Management**: View all users, search, filter by role/status, suspend/reactivate, role changes, audit trail
- **Role & Permission Management**: Capability matrix, pending role requests, approve/reject, backend-enforced RBAC
- **Academic Structure**: Subject and timetable management
- **Content Management**: Notice create/edit/publish, study resource management, moderation queue
- **Reports & Moderation**: Flagged content review, content reports, hide/restore/delete actions
- **Platform Settings**: Feature flags, registration settings, maintenance mode, announcement banner
- **Audit Logs**: Append-only record of all administrative actions
- **Analytics & Reporting**: 7-day trend charts from real backend data

## Technology Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose, Material 3
- **Architecture**: MVVM with Hilt dependency injection
- **Local Database**: Room 2.6.1
- **Backend**: Supabase (REST + Auth, Row Level Security)
- **WorkManager**: Background sync
- **DataStore**: Preferences
- **Glance**: App widget
- **Min SDK**: 26
- **Target/Compile SDK**: 35
- **JVM**: 17

## Build Instructions

### Prerequisites
- Android Studio or command-line Android SDK
- JDK 17
- Supabase project (for backend)
- Google OAuth credentials (for sign-in)

### Configuration
1. Copy `CampusOne/local.properties` template and fill in your values:
   ```
   supabase.url=YOUR_SUPABASE_URL
   supabase.anon.key=YOUR_SUPABASE_ANON_KEY
   google.oauth.client_id=YOUR_GOOGLE_CLIENT_ID
   google.oauth.web_client_id=YOUR_GOOGLE_WEB_CLIENT_ID
   google.oauth.web_client_secret=YOUR_GOOGLE_WEB_CLIENT_SECRET
   google.oauth.callback_url=YOUR_SUPABASE_AUTH_CALLBACK_URL
   sdk.dir=/path/to/your/android/sdk
   ```

2. Apply Supabase schema migrations from `supabase/migrations/` to your Supabase project.

3. Build the debug APK:
   ```
   cd CampusOne
   ./gradlew assembleDebug
   ```

### Release Builds
Release builds use the permanent release keystore configured in GitHub Actions secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). The release signing identity is fixed and must not be changed.

## Architecture

- **UI Layer**: Jetpack Compose screens with state hoisting, Navigation Compose
- **ViewModel Layer**: Hilt ViewModels with StateFlow UI state
- **Data Layer**: Repository pattern with Room (local) and Supabase Sync (remote)
- **Security**: Server-side role authorization via Supabase SECURITY DEFINER functions, RLS policies, append-only audit logs

## License

MIT License. See LICENSE file for details.
