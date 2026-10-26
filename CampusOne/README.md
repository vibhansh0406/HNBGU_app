# CampusOne — Offline-First Android Companion for Indian College Students

**CampusOne** is an open-source (MIT), native Android app built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room**, **DataStore**, **Hilt**, **WorkManager**, and **Jetpack Glance**. Designed for undergraduate students at Indian universities (pilot: **HNB Garhwal University — Chauras, Birla & BGR/SRT Campuses**), its campus tools work offline and it supports **English (`en`)** and **Hindi (`hi`)** switching.

**Authentication and collaboration are local demos only.** The profile is stored on the device; the app does not verify identity, provide secure server authentication, sync accounts, send live messages, or share local changes with other students. Selecting a CR/Teacher role only changes the local demo UI and is not authorization. Picked study files are copied into app-local storage; remote upload is not available.

## Complete Phased Feature Matrix (Phases 1, 2 & 3 Unified)

| Phase | Feature | Highlights |
| :--- | :--- | :--- |
| **Phase 1: Core** | **1. Onboarding & Profile** | Local/demo profile stored with DataStore; HNBGU campus/course/semester/section selector and demo roles (`STUDENT`, `CR`, `TEACHER`, `CLUB_ADMIN`). No identity verification or secure server authentication. |
| **Phase 1: Core** | **2. Smart Attendance Tracker** | Per-subject attended/total/cancelled logs, animated color-coded ring, "safe to miss" & "must attend to recover" calculator (configurable 60%–90% target), weekly/monthly history, personal-tracker disclaimer. |
| **Phase 1: Core** | **3. Timetable & Glance Widget** | Today view with `ONGOING NOW` & `UP NEXT` highlighting, 6-day weekly grid, local demo timetable edits, configurable lead-time reminders, Jetpack Glance Home-Screen Widget. |
| **Phase 1: Core** | **4. Notice Board** | Room-seeded and locally authored notices, pinned announcements, attachments/links, local read/unread badges. No remote notices or push delivery. |
| **Phase 2: Study** | **5. Notes & PYQ Vault** | Pick PDFs/images using Android's document picker and save supported files on-device, tagged by subject, semester, type (`NOTES`, `PYQ`, `ASSIGNMENT`, `OTHER`), and year; 10 MB size limit; bookmark & download for offline access; viewer clearly labels its sample preview instead of pretending to render file contents; uploader privacy toggle; local demo moderation. No remote uploads or shared vault. |
| **Phase 2: Study** | **6. Assignments & Deadlines** | Views for `Today`, `Upcoming`, `Overdue`, and `Completed` + `Group by Subject` toggle; priority tags (`HIGH`, `MEDIUM`, `LOW`); reboot-safe WorkManager reminders (`1 day before` & `2 hours before` via `CampusBootReceiver`); CR/Teacher shared section assignments with 1-tap **"Add to My List"**; top 3 upcoming deadlines on Home screen. |
| **Phase 2: Study** | **7. Exam Calendar & Syllabus** | Exam schedule & countdown on Home and Study Hub; per-subject unit & topic checklists with private local progress bars + overall syllabus completion; CR/Teacher section exam & syllabus publishing; exam reminders. |
| **Phase 2: Study** | **8. GPA / CGPA & Target Calculator** | Configurable grading scales (`10-Point UGC/NEP`, `4.0-Point`, `Percentage`); semester-wise credit & grade entry; multi-semester `Canvas` trend chart; **"What Do I Need?"** target CGPA calculator; local Room history persistence + one-tap printable **PDF Grade Sheet Export** (`PdfDocument`). |
| **Phase 3: Campus Life** | **9. Events & Clubs** | Local clubs/events directory and demo editing; event filters and search; **3-State RSVP (`Going` / `Interested` / `Not Going`)** with local counts; 1-tap **Add to Phone Calendar** (`CalendarContract`); Android Share Sheet event card; local demo moderation. No live campus feed or push notifications. |
| **Phase 3: Campus Life** | **10. Privacy-First Lost & Found** | Store local `LOST` or `FOUND` demo entries with category, place, date, and `OPEN`/`RESOLVED` status; claim requests and contact details remain on this device and are not delivered to another student; automatic **30-day auto-archive**; local demo reporting and moderation. |
| **Phase 3: Campus Life** | **11. Campus Directory & Map** | 100% offline directory of HNBGU departments, faculty (designation, email, office location, office hours), and 24×7 emergency/hostel/medical/library numbers; **Tap-to-Call** (`ACTION_DIAL`), **Tap-to-Email** (`ACTION_SENDTO`), **Tap-to-Copy** (`ClipboardManager`); student **Suggest Correction** workflow with CR/Teacher moderation queue; **Campus Map** tab listing key campus landmarks with 1-tap **"Open in Google Maps"** using free `geo:` URIs (zero paid API keys). |

## Unit Test Suite (`app/src/test/`)

1. `AttendanceCalculatorTest.kt`: Zero-class `NO_DATA` states, cancelled class exclusion, exact 75% `ON_EDGE` boundary, safe misses, and consecutive recovery math.
2. `TimetableEngineTest.kt`: `COMPLETED`, `ONGOING`, and single `UP_NEXT` transitions + multi-day rollover.
3. `GpaCalculatorTest.kt`: 10-point and 4-point weighted SGPA/CGPA calculations, percentage multipliers, and **"What Do I Need?"** target CGPA achievability math.
4. `ReminderAndSyncTest.kt`: 1-day (1440m) and 2-hour (120m) reminder lead-time scheduling, past-window filtering, LWW assignment conflict resolution with `sharedSourceId` deduplication, and offline bookmark preservation.
5. `CampusLifeEngineTest.kt`: 3-state RSVP counter transitions (`GOING` / `INTERESTED` / `NOT_GOING` / `NONE`) and 30-day Lost & Found auto-archive boundaries.

## Local-only behavior

Profiles and edits are persisted on-device with DataStore; notices and the rest of the seeded campus data are stored in Room. There is no chat UI in this project. No local/demo profile, role, notice, event, or file is a secure account, live message, verified university record, or remote upload.
