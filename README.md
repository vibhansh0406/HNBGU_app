# CampusOne — HNB Garhwal University Full-Stack Monorepo

Production-ready, offline-first Android application (`CampusOne/`), Supabase PostgreSQL + Row Level Security backend (`supabase/`), and Next.js TypeScript Admin Portal (`admin-portal/`) designed for ~4,000 students at HNB Garhwal University on free-tier infrastructure.

## Repository Structure

| Path | Description |
| :--- | :--- |
| `CampusOne/` | Native Kotlin 2.0.20 + Jetpack Compose Material 3 Android app (`com.campusone.app`) with Room v3 offline database, Google Credential Manager sign-in, Supabase Auth & PostgREST sync, and Role-Based Access Control. |
| `supabase/migrations/` | Versioned PostgreSQL schema (`20261027000001_initial_campusone_schema.sql`) and Row Level Security + RPC policies (`20261027000002_rls_policies_and_security.sql`). |
| `supabase/bootstrap_super_admin.sql` | One-time SQL template to promote your initial Google account to `super_admin`. |
| `supabase/functions/send-fcm-campaign/` | Authenticated Supabase Edge Function (`index.ts`) that verifies caller role/department scope and dispatches FCM HTTP v1 push notifications. |
| `admin-portal/` | Next.js 14 (App Router) + TypeScript + Tailwind CSS Web Admin Portal ready for Vercel deployment. |
| `SETUP_SUPABASE.md` | Complete step-by-step setup guide for Supabase, Google Cloud OAuth, Firebase FCM, and Vercel. |
| `SECURITY.md` | Security architecture, key handling, RLS guarantees, and privilege-escalation prevention. |

## Entity Relationship Summary

```text
[departments] 1 ──── * [profiles] (id = auth.users.id, role defaults to 'student')
     │                     ├── 1..1 [student_enrollments] (verified by department_admin/super_admin)
     │                     ├── 1..* [attendance_records] (read-only for owning student; written by admin)
     │                     ├── 1..* [device_tokens] (private per user; queried by Edge Function)
     │                     └── 1..* [audit_logs] (immutable administrative trail)
     ├── 1..* [notices] (draft/published/archived; students only see published)
     ├── 1..* [timetable_entries] (department + semester + section + weekday)
     ├── 1..* [assignments] (department + semester + due_at)
     ├── 1..* [exam_events] (department + semester + exam_date)
     └── 1..* [notification_campaigns] (dispatched via send-fcm-campaign Edge Function)
```
