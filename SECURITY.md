# CampusOne — Security Architecture & Access Control Policy

## 1. Key Separation & Secret Hygiene
- **Allowed in Client Apps (Android & Next.js)**:
  - `SUPABASE_URL` / `NEXT_PUBLIC_SUPABASE_URL`
  - `SUPABASE_PUBLISHABLE_KEY` / `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` (Supabase `anon` publishable key)
  - `GOOGLE_WEB_CLIENT_ID` (public OAuth 2.0 Web Client ID)
- **Strictly Forbidden in Client Apps & Git**:
  - `SUPABASE_SERVICE_ROLE_KEY` (only available inside Supabase Edge Functions runtime)
  - Google OAuth Client Secret (stored only in Supabase Dashboard Auth settings)
  - Firebase Service Account private key JSON (`FCM_SERVICE_ACCOUNT_JSON`, stored only in Supabase Edge Function secrets)

## 2. Server-Side Row Level Security (RLS)
- Every table in `public` has `ENABLE ROW LEVEL SECURITY` and `FORCE ROW LEVEL SECURITY`.
- Helper functions (`current_user_role()`, `current_user_department_id()`, `is_super_admin()`, `is_department_admin_for()`, `can_manage_notices_for()`, `can_manage_exams_for()`) use `SECURITY DEFINER SET search_path = public, pg_temp` to prevent search-path injection and infinite policy recursion.
- **Trigger-Level Self-Escalation Prevention**:
  - `on_auth_user_created` (`handle_new_auth_user`) ignores client metadata for `role` and always initializes new users with `role = 'student'` and `account_status = 'active'`.
  - `trg_guard_profile_escalation` (`prevent_unauthorized_profile_escalation`) blocks any `UPDATE` on `public.profiles` that attempts to change `role`, `account_status`, or `department_id` unless executed by a `super_admin` or via the audited `admin_assign_user_role` RPC.

## 3. Login Path vs. Trusted Authorization
- Choosing **Admin Login** on Android or signing into `/admin-portal` only selects the intended UI destination.
- Both Android (`RoleAuthorizationEngine.evaluateLoginGate`) and Next.js (`AdminLayoutShell`) query `public.profiles` after authentication and deny access if `role == 'student'` or `account_status != 'active'`.
- Even if a malicious client modifies the Android APK or browser bundle, PostgreSQL RLS rejects unauthorized reads, inserts, updates, and deletes at the database layer.
