# CampusOne Web Admin Portal (`/admin-portal`)

Next.js 14 (App Router) + TypeScript + Tailwind CSS administration portal for HNB Garhwal University.

## Routes Implemented
- `/login` — Google OAuth sign-in via Supabase Auth
- `/dashboard` — Live counts and role-scoped overview
- `/departments` — University department registry (`super_admin` write access)
- `/notices` — Create, draft, publish, pin, and archive circulars with department targeting
- `/timetable` — Publish section class schedules with `HH:mm` time range validation
- `/assignments` — Manage department & semester assignments
- `/exams` — Publish mid-sem, end-sem, practical, and back-paper exam schedules
- `/students` — Approve/reject student enrollments and record official attendance
- `/admins` — Super-Admin-only role & account status management via `admin_assign_user_role` RPC
- `/notifications` — Compose push notification campaigns and trigger `send-fcm-campaign` Edge Function
- `/audit-logs` — Immutable administrative audit trail (`super_admin` only)
- `/settings` — Environment diagnostics and one-time first-super-admin bootstrap helper
