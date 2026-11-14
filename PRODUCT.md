# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

HNB Garhwal University students on the Birla Campus and Chauras Campus (Srinagar Garhwal, Uttarakhand) as the primary audience. They use a phone on campus, often with patchy connectivity, and need their class schedule, attendance status, section notices, and study material in one place.

Secondary audiences, already represented as roles in the product:

- Class Representatives (CR) and faculty who publish section timetables and notices.
- Club admins who run campus clubs and events.
- Moderators who review community content within their scope.
- The project owner/admin, who broadcasts university-wide notices and study material to every student.

## Product Purpose

CampusOne is a single offline-first companion for HNBGU campus life. It replaces scattered WhatsApp forwards, notice boards, and spreadsheet attendance with one app for attendance tracking, timetable, section notices, study resources, exams, clubs, events, lost and found, and a campus directory.

Success means a student opens it every day to check what is next, whether they are attendance-safe, and what their section or the university has announced — and it still works when the network does not.

## Positioning

CampusOne combines four things a generic college app does not: attendance intelligence (how many classes a student can still miss and stay safe), a section-level community (course + semester + section scoped timetable and notices), university-wide broadcast from the admin, and an all-in-one offline-first campus surface.

It is an independent, student-built project. It is not an official HNBGU product and must never claim or imply university endorsement.

## Operating Context

- Students identify by course, semester (1-8), and section (A/B/C); that combination is the section key that scopes timetable, notices, and study resources.
- CRs and faculty publish timetable slots and announcements for their section.
- The admin broadcasts notices and study resources to all sections; those are synced under a global scope and downloaded by every student.
- Sign-in is Google, backed by Supabase auth; profiles are stored server-side and restored on a new device.
- Study resources are PDFs uploaded to Supabase Storage with content-hash deduplication, real upload progress, and an in-app viewer.
- Attendance logs, timetable, and notices are cached in Room so the app opens and works offline; background sync runs via WorkManager.
- The app supports an English/Hindi language toggle and a light/dark "Forest & Ivory" visual identity.
- Only Birla and Chauras campuses are in scope for now; affiliated colleges and the BGR/SRT campuses are intentionally out of scope.
- The project runs on free tiers only: no paid APIs, no paid infrastructure, no secret keys embedded in the client.

## Capabilities and Constraints

Confirmed capabilities:

- Attendance marking with per-subject safety insights against a target percentage.
- Section timetable with next-class and reminder support.
- Notice board with section-scoped posts plus admin global broadcasts; attachments supported.
- Study hub: notes, papers, and syllabus by subject, with upload, dedupe, and in-app PDF viewing.
- Exams, GPA tools, clubs, campus events, lost and found, and a campus directory.
- Roles: Student, CR, Teacher, Club Admin, Moderator, Admin.
- Server-side role enforcement: the owner account is always admin; no other account can self-assign admin.

Technical constraints:

- Native Android: Kotlin, Jetpack Compose, Hilt, Room, WorkManager, DataStore; min SDK 26, target/compile SDK 35.
- Application ID `in.campusone.app` and the pinned debug/release signing identity are permanent; changing either breaks Google Sign-In registration and existing installs.
- Backend is Supabase (REST, auth, storage) with `supabase/schema.sql` as the authoritative schema.
- The build is verified in GitHub Actions; the local environment cannot run Gradle or the JVM.

Explicitly undecided or pending:

- Whether to expand beyond Birla and Chauras campuses later.
- Migration of study-resource storage from Supabase Storage to a Cloudflare R2 proxy (credentials not yet provided).
- Cross-device sync for moderator appointments, clubs, events, lost-and-found, and directory content.
- Production hardening of release builds (R8/minification is currently disabled).

## Brand Commitments

- Name: CampusOne.
- Independent student-built project; no official HNBGU endorsement claims, logos, or implied affiliation.
- Fixed public identity: package `in.campusone.app` and the permanent signing certificate.
- Practical, student-first voice; the UI is bilingual (English with a Hindi toggle).

## Evidence on Hand

- Working Android app at `CampusOne/` with the feature set above.
- Authoritative database schema and RLS policies at `supabase/schema.sql`.
- Real seed content for HNBGU sections, subjects, and timetable at `CampusOne/app/src/main/java/in/campusone/app/data/local/HnbguSeedData.kt`.
- CI build and APK verification at `.github/workflows/build.yml`.

Absent, and must not be fabricated: official university data feeds, testimonials, user counts, press coverage, partnerships, or benchmarks.

## Product Principles

1. The section is the unit of trust: section-scoped content stays local and precise, and only the admin broadcasts globally.
2. Offline first, sync when possible: every core screen must open and work without a network.
3. Attendance clarity over gamification: tell students plainly how many classes they can still miss.
4. Honest independence: never imply official university endorsement.
5. Free-tier discipline: no paid APIs or infrastructure, and no secrets in the client.

## Accessibility & Inclusion

- Bilingual interface with an English/Hindi toggle.
- No formal accessibility standard has been confirmed yet; this is an open decision.
