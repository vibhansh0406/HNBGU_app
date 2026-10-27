# CampusOne — Complete Supabase, Google OAuth, FCM & Vercel Setup Guide

## 1. Supabase Project & SQL Migrations (Exact Execution Order)

1. Create a new project in the [Supabase Dashboard](https://supabase.com/dashboard).
2. Open **SQL Editor** -> **New Query** and execute the migrations in this exact order:
   1. `supabase/migrations/20261027000001_initial_campusone_schema.sql`
   2. `supabase/migrations/20261027000002_rls_policies_and_security.sql`
3. Verify in **Table Editor** that all 11 tables (`departments`, `profiles`, `student_enrollments`, `notices`, `timetable_entries`, `assignments`, `exam_events`, `attendance_records`, `device_tokens`, `notification_campaigns`, `audit_logs`) have **RLS Enabled**.

## 2. Google Cloud Console / Google Auth Platform Setup

1. Open [Google Cloud Console](https://console.cloud.google.com/) -> **APIs & Services** -> **OAuth consent screen** and configure **External** (or Internal workspace) consent for `CampusOne`.
2. Go to **Credentials** -> **Create Credentials** -> **OAuth client ID**:
   - **Client 1 — Web application (Required for both Supabase Auth, Next.js Portal, and Android `setServerClientId`)**:
     - Authorized JavaScript origins: `http://localhost:3000` and your Vercel domain `https://your-portal.vercel.app`
     - Authorized redirect URIs: `https://<YOUR_PROJECT_REF>.supabase.co/auth/v1/callback`
     - Copy the generated **Web Client ID** (`*.apps.googleusercontent.com`) and **Client Secret**.
   - **Client 2 — Android application (Required for native Android Google Credential Manager)**:
     - Package name: `com.campusone.app`
     - SHA-1 certificate fingerprint: run `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android` in Codespaces (or your release keystore SHA-1).
3. In **Supabase Dashboard** -> **Authentication** -> **Providers** -> **Google**:
   - Enable **Sign in with Google**.
   - Paste the **Web Client ID** and **Web Client Secret**.
   - Under **Authorized Client IDs**, paste both the **Web Client ID** and the **Android Client ID** so Supabase accepts native Android ID tokens.
   - In **Authentication** -> **URL Configuration**, add `http://localhost:3000/auth/callback` and `https://your-portal.vercel.app/auth/callback` to **Redirect URLs**.

## 3. Bootstrapping Your First Super Admin

1. Sign in once with your personal Google account via the Android app or `/admin-portal/login`. The database trigger `handle_new_auth_user()` will create your row in `public.profiles` with `role = 'student'`.
2. Open `supabase/bootstrap_super_admin.sql`, replace `REPLACE_WITH_YOUR_GOOGLE_EMAIL@gmail.com` with your Google email, and run it in the **Supabase SQL Editor** (or use the one-time **Bootstrap RPC** form in `/settings` inside the portal).
3. Once one `super_admin` exists, both `bootstrap_super_admin.sql` and `bootstrap_first_super_admin()` automatically lock themselves. Subsequent admins are promoted from `/admins` via the `admin_assign_user_role` RPC.

## 4. Configuring the Android App (`CampusOne/local.properties`)

Copy `CampusOne/local.properties.example` to `CampusOne/local.properties` (untracked by Git) and fill in:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_SUPABASE_ANON_PUBLISHABLE_KEY
GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
FCM_ENABLED=false
```

You can also enter or update these three values directly inside the Android app under **Settings -> Cloud & Supabase Setup**.

## 5. Configuring & Deploying the Next.js Admin Portal (`/admin-portal`)

### Local Development
```bash
cd admin-portal
cp .env.example .env.local
# Edit .env.local with NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY
npm install
npm run dev
```

### Vercel Deployment
1. Import the GitHub repository into [Vercel](https://vercel.com/new).
2. Set **Root Directory** to `admin-portal`.
3. Add Environment Variables:
   - `NEXT_PUBLIC_SUPABASE_URL` = `https://YOUR_PROJECT_REF.supabase.co`
   - `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` = `YOUR_SUPABASE_ANON_PUBLISHABLE_KEY`
   - `NEXT_PUBLIC_SITE_URL` = `https://your-portal.vercel.app`
4. Deploy.

## 6. Firebase Cloud Messaging (FCM) Push Setup (Optional / Configurable)

1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/) -> **Project Settings** -> **Service accounts** -> **Generate new private key** (JSON).
2. Store the service account JSON exclusively in **Supabase Edge Function Secrets** (never in Android or Next.js):
   ```bash
   supabase secrets set FCM_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}'
   supabase functions deploy send-fcm-campaign
   ```
3. Until `FCM_SERVICE_ACCOUNT_JSON` is set, creating a campaign in `/notifications` saves and queues the campaign in PostgreSQL and reports `fcm_configured: false` without breaking any build or page.
