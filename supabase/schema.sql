-- CampusOne — Supabase schema
-- HISTORICAL BASELINE ONLY. Do NOT rerun this entire file against production:
-- it contains a legacy DROP TABLE for study_resources. Use reviewed migrations only.
--
-- SECURITY MODEL (read before changing anything below)
-- ---------------------------------------------------
-- * The owner is bound to the immutable Supabase Auth UUID b2b52ade-0fcc-4e1d-9c39-ca284789c960.
--   Never derive owner privileges from a mutable profile email or caller parameter.
-- * Roles live in `public.profiles.role` but can only be changed by
--   `public.admin_set_role()`, which requires the `MANAGE_ROLES` permission.
-- * Every privileged mutation goes through a `SECURITY DEFINER` function that
--   checks a permission and writes an `audit_logs` row. Table-level write
--   policies are deny-by-default: broad `using (true)` write policies were
--   removed because PostgreSQL combines permissive policies with OR, so a
--   broad policy silently defeats any restrictive one.
-- * The anon key is public. Anything a student can reach with it is public.
--   Treat every policy below as reachable by an attacker.

-- =========================================================================
-- PROFILES
-- =========================================================================
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  email text not null default '',
  role text not null default 'STUDENT',
  college_id text not null default '',
  college_name text not null default '',
  course_id text not null default '',
  course_name text not null default '',
  semester integer not null default 1,
  section text not null default 'A',
  moderator_scope text not null default '',
  is_moderator_approved boolean not null default false,
  verified_role boolean not null default false,
  is_suspended boolean not null default false,
  onboarding_completed boolean not null default false,
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

alter table public.profiles add column if not exists is_suspended boolean not null default false;
alter table public.profiles add column if not exists onboarding_completed boolean not null default false;

-- =========================================================================
-- PERMISSION MODEL
-- =========================================================================
create table if not exists public.admin_permissions (
  code text primary key,
  description text not null default '',
  is_owner_only boolean not null default false
);

insert into public.admin_permissions (code, description, is_owner_only) values
  ('VIEW_USERS',            'View user directory and profiles', false),
  ('MANAGE_USERS',          'Suspend, reactivate and remove accounts', false),
  ('MANAGE_ROLES',          'Assign or revoke roles', true),
  ('MANAGE_NOTICES',        'Publish, edit and delete notices', false),
  ('MANAGE_RESOURCES',      'Publish, edit and delete study resources', false),
  ('MANAGE_ASSIGNMENTS',    'Publish, edit and delete assignments', false),
  ('MODERATE_REPORTS',      'Review reports and moderate content', false),
  ('MANAGE_EVENTS',         'Publish, edit and delete events and clubs', false),
  ('MANAGE_TIMETABLE',      'Create and update subject-based timetables', false),
  ('VIEW_AUDIT_LOGS',       'Read the append-only audit trail', true),
  ('MANAGE_SYSTEM_SETTINGS','Change platform-wide settings', true)
on conflict (code) do nothing;

-- Default capability sets. SUPER_ADMIN is implicit (owner only) and is never
-- stored in this table, so no client path can grant it.
create table if not exists public.role_permissions (
  role text not null,
  permission text not null references public.admin_permissions(code) on delete cascade,
  primary key (role, permission)
);

delete from public.role_permissions where role = 'ADMIN';
insert into public.role_permissions (role, permission)
select 'ADMIN', code from public.admin_permissions
where code in (
  'VIEW_USERS','MANAGE_USERS','MANAGE_NOTICES','MANAGE_RESOURCES',
  'MANAGE_ASSIGNMENTS','MODERATE_REPORTS','MANAGE_EVENTS','MANAGE_TIMETABLE'
)
on conflict do nothing;

-- Moderators use the regular Admin Portal. They receive operational
-- permissions only; role management, audit logs and system settings remain
-- owner-only and are never granted to this role.
delete from public.role_permissions where role = 'MODERATOR';
insert into public.role_permissions (role, permission)
select 'MODERATOR', code from public.admin_permissions
where not is_owner_only and code not in ('VIEW_USERS', 'MANAGE_USERS')
on conflict do nothing;

-- =========================================================================
-- AUDIT LOG (append-only)
-- =========================================================================
create table if not exists public.audit_logs (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid,
  actor_email text not null default '',
  action text not null,
  target_type text not null default '',
  target_id text not null default '',
  detail jsonb not null default '{}'::jsonb,
  result text not null default 'SUCCESS',
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create index if not exists audit_logs_created_at_idx on public.audit_logs (created_at desc);
create index if not exists audit_logs_actor_idx on public.audit_logs (actor_id, created_at desc);

-- =========================================================================
-- PLATFORM SETTINGS (single row, id = 'global')
-- =========================================================================
create table if not exists public.platform_settings (
  id text primary key default 'global',
  maintenance_mode boolean not null default false,
  maintenance_message text not null default '',
  announcement_banner text not null default '',
  announcement_enabled boolean not null default false,
  registration_enabled boolean not null default true,
  resource_uploads_enabled boolean not null default true,
  feature_notices boolean not null default true,
  feature_resources boolean not null default true,
  feature_assignments boolean not null default true,
  feature_events boolean not null default true,
  feature_attendance boolean not null default true,
  updated_by text not null default '',
  updated_at bigint not null default 0
);

insert into public.platform_settings (id) values ('global') on conflict (id) do nothing;

-- =========================================================================
-- CONTENT REPORTS
-- =========================================================================
create table if not exists public.content_reports (
  id uuid primary key default gen_random_uuid(),
  target_type text not null,
  target_id text not null,
  reason text not null default '',
  reporter_id uuid,
  reporter_email text not null default '',
  status text not null default 'PENDING',
  resolution text not null default '',
  reviewed_by text not null default '',
  reviewed_at bigint not null default 0,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create index if not exists content_reports_status_idx on public.content_reports (status, created_at desc);
create index if not exists content_reports_target_idx on public.content_reports (target_type, target_id);

-- =========================================================================
-- ROLE REQUESTS
-- =========================================================================
create table if not exists public.role_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null,
  email text not null default '',
  requested_role text not null default 'CR',
  note text not null default '',
  status text not null default 'PENDING',
  created_at bigint not null default 0,
  reviewed_by text not null default '',
  reviewed_at bigint not null default 0
);

create index if not exists role_requests_status_idx on public.role_requests (status, created_at desc);

-- =========================================================================
-- CORE CAMPUS
-- =========================================================================
create table if not exists public.subjects (
  id text primary key,
  section_key text not null default '',
  subject_name text not null default '',
  subject_code text not null default '',
  is_elective boolean not null default false,
  is_enrolled boolean not null default true,
  updated_at bigint not null default 0
);

create table if not exists public.timetable_slots (
  id text primary key,
  section_key text not null default '',
  subject_id text not null default '',
  subject_name text not null default '',
  subject_code text not null default '',
  room_number text not null default '',
  day_of_week integer not null default 1,
  start_time text not null default '09:00',
  end_time text not null default '10:00',
  is_elective boolean not null default false,
  updated_at bigint not null default 0
);

create table if not exists public.notices (
  id text primary key,
  section_key text not null default '',
  title text not null default '',
  body text not null default '',
  attachment_url text,
  attachment_type text not null default 'NONE',
  is_pinned boolean not null default false,
  author_uid text not null default '',
  author_name text not null default '',
  author_role text not null default 'CR',
  created_at bigint not null default 0,
  is_read boolean not null default false
);

create table if not exists public.attendance_logs (
  id text primary key,
  user_id text not null default '',
  subject_id text not null default '',
  date_iso text not null default '',
  status text not null default 'PRESENT',
  note text not null default '',
  is_synced boolean not null default true,
  updated_at bigint not null default 0
);

create index if not exists attendance_logs_user_idx on public.attendance_logs (user_id, date_iso desc);

create table if not exists public.assignments (
  id text primary key,
  section_key text not null default '',
  subject_id text not null default '',
  subject_tag text not null default '',
  title text not null default '',
  description text not null default '',
  priority text not null default 'MEDIUM',
  due_date_epoch_millis bigint not null default 0,
  due_date_formatted text not null default '',
  reminder_enabled boolean not null default true,
  remind_1_day_before boolean not null default true,
  remind_2_hours_before boolean not null default true,
  is_completed boolean not null default false,
  is_shared_by_cr_or_teacher boolean not null default false,
  is_added_to_personal_list boolean not null default true,
  shared_source_id text,
  author_name text not null default 'Self',
  is_synced boolean not null default true,
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

create table if not exists public.exams (
  id text primary key,
  section_key text not null default '',
  subject_id text not null default '',
  subject_name text not null default '',
  subject_code text not null default '',
  exam_title text not null default '',
  exam_date_epoch_millis bigint not null default 0,
  exam_date_iso text not null default '',
  exam_time_formatted text not null default '',
  exam_date_formatted text not null default '',
  room_or_center text not null default '',
  reminder_enabled boolean not null default true,
  published_by_cr_or_teacher boolean not null default true,
  updated_at bigint not null default 0
);

-- Re-created on storage upgrade (old table carried unused columns and no real data).
drop table if exists public.study_resources;
create table public.study_resources (
  id text primary key,
  section_key text not null default '',
  subject_id text not null default '',
  subject_name text not null default '',
  semester integer not null default 1,
  title text not null default '',
  category text not null default 'NOTES',
  exam_year integer not null default 2025,
  file_url text not null default '',
  file_type text not null default 'PDF',
  file_size_bytes bigint not null default 0,
  file_hash text not null default '',
  uploader_uid text not null default '',
  uploader_name text not null default '',
  show_uploader_name boolean not null default true,
  upvotes integer not null default 0,
  report_count integer not null default 0,
  is_flagged boolean not null default false,
  is_hidden boolean not null default false,
  is_approved_by_cr boolean not null default true,
  upload_status text not null default 'PUBLISHED',
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

create table if not exists public.syllabus_topics (
  id text primary key,
  section_key text not null default '',
  subject_id text not null default '',
  subject_name text not null default '',
  unit_number integer not null default 1,
  topic_title text not null default '',
  is_completed boolean not null default false,
  updated_at bigint not null default 0
);

create table if not exists public.gpa_semesters (
  id text primary key,
  user_id text not null default '',
  semester_number integer not null default 1,
  sgpa real not null default 0,
  percentage real not null default 0,
  created_at bigint not null default 0
);

create table if not exists public.clubs (
  id text primary key,
  college_id text not null default '',
  name text not null default '',
  category text not null default '',
  description text not null default '',
  member_count integer not null default 0,
  is_followed_by_me boolean not null default false,
  is_approved boolean not null default false,
  is_flagged boolean not null default false,
  contact_email text not null default '',
  social_link text not null default '',
  admin_uid text not null default '',
  admin_name text not null default '',
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

create table if not exists public.campus_events (
  id text primary key,
  college_id text not null default '',
  club_id text not null default '',
  title text not null default '',
  description text not null default '',
  event_epoch_millis bigint not null default 0,
  venue text not null default '',
  going_count integer not null default 0,
  interested_count integer not null default 0,
  not_going_count integer not null default 0,
  my_rsvp_status text not null default 'NONE',
  is_approved boolean not null default false,
  is_flagged boolean not null default false,
  report_count integer not null default 0,
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

create table if not exists public.lost_found_items (
  id text primary key,
  college_id text not null default '',
  type text not null default 'LOST',
  title text not null default '',
  description text not null default '',
  location text not null default '',
  poster_uid text not null default '',
  poster_name text not null default '',
  poster_phone text not null default '',
  poster_email text not null default '',
  contact_approved boolean not null default false,
  is_resolved boolean not null default false,
  is_hidden boolean not null default false,
  report_count integer not null default 0,
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

create table if not exists public.lost_found_contact_requests (
  id text primary key,
  item_id text not null default '',
  requester_uid text not null default '',
  requester_name text not null default '',
  requester_contact text not null default '',
  status text not null default 'PENDING',
  created_at bigint not null default 0
);

create table if not exists public.directory_contacts (
  id text primary key,
  college_id text not null default '',
  name text not null default '',
  category text not null default '',
  role text not null default '',
  phone text not null default '',
  email text not null default '',
  location text not null default '',
  created_at bigint not null default 0
);

create table if not exists public.directory_suggestions (
  id text primary key,
  college_id text not null default '',
  field text not null default '',
  current_value text not null default '',
  suggested_value text not null default '',
  submitted_by text not null default '',
  status text not null default 'PENDING',
  created_at bigint not null default 0
);

create table if not exists public.campus_map_places (
  id text primary key,
  college_id text not null default '',
  name text not null default '',
  category text not null default '',
  campus_name text not null default '',
  description text not null default '',
  latitude double precision not null default 0,
  longitude double precision not null default 0,
  map_query_label text not null default '',
  created_at bigint not null default 0
);

create table if not exists public.moderators (
  id text primary key,
  college_id text not null default '',
  name text not null default '',
  email text not null default '',
  scope text not null default '',
  is_approved boolean not null default false,
  created_at bigint not null default 0
);

-- =========================================================================
-- IDENTITY AND AUTHORIZATION HELPERS
-- All of them read the JWT, never a client-supplied value.
-- =========================================================================

-- The verified email of the caller, taken from the access token.
create or replace function public.campusone_caller_email()
returns text
language sql
stable
set search_path = ''
as $$
  select coalesce(
    nullif(auth.jwt() ->> 'email', ''),
    nullif(current_setting('request.jwt.claim.email', true), ''),
    ''
  );
$$;

create or replace function public.campusone_is_owner()
returns boolean
language sql
stable
set search_path = ''
as $$
  select auth.uid() = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid;
$$;

-- Current role of the caller, read from the database (never from the client).
create or replace function public.campusone_caller_role()
returns text
language sql
stable
set search_path = ''
as $$
  select coalesce(
    (select p.role from public.profiles p where p.id = auth.uid()),
    'ANON'
  );
$$;

-- True when the caller holds a permission. Owner holds every permission.
create or replace function public.campusone_has_permission(perm text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.campusone_is_owner()
     or exists (
       select 1
       from public.role_permissions rp
       where rp.role = public.campusone_caller_role()
         and rp.permission = perm
     );
$$;

-- Backwards-compatible alias used by older policies.
create or replace function public.campusone_is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $
  select public.campusone_is_owner()
     or public.campusone_caller_role() in ('ADMIN', 'MODERATOR');
$;

-- Backwards-compatible alias used by legacy RLS policies.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $
  select public.campusone_is_admin();
$;

-- Only the owner may hold SUPER_ADMIN. This is the single choke point for
-- role escalation: the trigger below calls it on every profile write.
create or replace function public.campusone_role_allowed(new_role text, target uuid)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
begin
  if new_role = 'SUPER_ADMIN' then
    return public.campusone_is_owner() and target = auth.uid();
  end if;
  if new_role = 'ADMIN' then
    return public.campusone_is_owner();
  end if;
  return true;
end;
$$;

-- Server-side role enforcement. Runs as the definer so it can read the target
-- row even when the caller has no SELECT policy on it.
create or replace function public.campusone_enforce_role()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
begin
  -- The owner's row is pinned to SUPER_ADMIN and can never be suspended.
  if new.id = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    new.role := 'SUPER_ADMIN';
    new.is_suspended := false;
    new.is_moderator_approved := true;
    return new;
  end if;

  -- Role changes are only legal through admin_set_role(), which already
  -- checked MANAGE_ROLES. Any other write keeps the stored role.
  if tg_op = 'INSERT' then
    new.role := 'STUDENT';
  elsif new.role is distinct from old.role then
    if not public.campusone_has_permission('MANAGE_ROLES') then
      new.role := old.role;
    elsif not public.campusone_role_allowed(new.role, new.id) then
      new.role := old.role;
    end if;
  end if;

  -- Suspension is a MANAGE_USERS action.
  if new.is_suspended is distinct from old.is_suspended
     and not public.campusone_has_permission('MANAGE_USERS') then
    new.is_suspended := old.is_suspended;
  end if;

  -- A suspended account cannot hold a staff role.
  if new.is_suspended and new.role not in ('STUDENT', 'SUPER_ADMIN') then
    new.role := 'STUDENT';
  end if;

  return new;
end;
$$;

drop trigger if exists campusone_enforce_role on public.profiles;
create trigger campusone_enforce_role
  before insert or update on public.profiles
  for each row execute function public.campusone_enforce_role();

-- =========================================================================
-- AUDIT WRITE HELPER
-- =========================================================================
create or replace function public.campusone_audit(
  p_action text,
  p_target_type text,
  p_target_id text,
  p_detail jsonb,
  p_result text default 'SUCCESS'
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.audit_logs (
    actor_id, actor_email, action, target_type, target_id, detail, result, created_at
  ) values (
    auth.uid(),
    public.campusone_caller_email(),
    p_action,
    coalesce(p_target_type, ''),
    coalesce(p_target_id, ''),
    coalesce(p_detail, '{}'::jsonb),
    coalesce(p_result, 'SUCCESS'),
    (extract(epoch from now()) * 1000)::bigint
  );
end;
$$;

-- =========================================================================
-- PRIVILEGED OPERATIONS (SECURITY DEFINER, permission checked, audited)
-- Every function returns jsonb: {ok: bool, error: text}.
-- =========================================================================

-- Bootstrap: create the caller's own profile row with STUDENT.
create or replace function public.campusone_bootstrap_profile(
  p_name text default '',
  p_email text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_email text;
  v_role text;
begin
  if v_uid is null then
    return jsonb_build_object('ok', false, 'error', 'Not signed in');
  end if;

  -- Never trust p_email: use the canonical Supabase Auth user record.
  select lower(coalesce(u.email, ''))
    into v_email
  from auth.users u
  where u.id = v_uid;

  if not found or v_email = '' then
    return jsonb_build_object('ok', false, 'error', 'Authenticated user email unavailable');
  end if;

  v_role := case
    when v_uid = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then 'SUPER_ADMIN'
    else 'STUDENT'
  end;

  insert into public.profiles (id, email, full_name, role, created_at, updated_at)
  values (
    v_uid, v_email, coalesce(p_name, ''), v_role,
    (extract(epoch from now()) * 1000)::bigint,
    (extract(epoch from now()) * 1000)::bigint
  )
  on conflict (id) do update
    set full_name = coalesce(nullif(excluded.full_name, ''), public.profiles.full_name),
        email = excluded.email,
        updated_at = (extract(epoch from now()) * 1000)::bigint;

  return jsonb_build_object(
    'ok', true,
    'role', (select p.role from public.profiles p where p.id = v_uid)
  );
end;
$$;

-- Session info for the signed-in caller. The client trusts this, not its cache.
create or replace function public.campusone_my_admin_state()
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_profile public.profiles;
  v_perms text[];
begin
  if v_uid is null then
    return jsonb_build_object('signedIn', false);
  end if;

  select * into v_profile from public.profiles where id = v_uid;

  if v_profile.id is null then
    return jsonb_build_object(
      'signedIn', true,
      'role', 'STUDENT',
      'isOwner', false,
      'isSuspended', false,
      'permissions', '[]'::jsonb
    );
  end if;

  select coalesce(array_agg(rp.permission), array[]::text[])
    into v_perms
    from public.role_permissions rp
    where rp.role = v_profile.role;

  if public.campusone_is_owner() then
    select coalesce(array_agg(code), array[]::text[]) into v_perms from public.admin_permissions;
  end if;

  return jsonb_build_object(
    'signedIn', true,
    'role', v_profile.role,
    'isOwner', public.campusone_is_owner(),
    'isSuspended', v_profile.is_suspended,
    'permissions', to_jsonb(v_perms)
  );
end;
$$;

-- Role change. MANAGE_ROLES only; SUPER_ADMIN can never be granted.
update public.profiles set verified_role = true, updated_at = (extract(epoch from now()) * 1000)::bigint where role in ('CR','MODERATOR','ADMIN','SUPER_ADMIN') and verified_role is distinct from true;
create or replace function public.admin_set_role(p_uid uuid, p_role text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_target public.profiles;
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
begin
  if not public.campusone_has_permission('MANAGE_ROLES') then
    perform public.campusone_audit('admin_set_role', 'profile', p_uid::text,
      jsonb_build_object('requested', p_role), 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'Missing MANAGE_ROLES permission');
  end if;

  if p_role = 'SUPER_ADMIN' then
    perform public.campusone_audit('admin_set_role', 'profile', p_uid::text,
      jsonb_build_object('requested', p_role), 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'SUPER_ADMIN cannot be granted');
  end if;

  if p_role not in ('STUDENT','CR','MODERATOR') then
    return jsonb_build_object('ok', false, 'error', 'Unknown role');
  end if;

  select * into v_target from public.profiles where id = p_uid;
  if v_target.id is null then
    return jsonb_build_object('ok', false, 'error', 'User not found');
  end if;

  if v_target.id = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    return jsonb_build_object('ok', false, 'error', 'Owner role is protected');
  end if;

  update public.profiles
     set role = p_role,
         is_moderator_approved = (p_role = 'MODERATOR'),
         updated_at = v_now
   where id = p_uid;

  perform public.campusone_audit('admin_set_role', 'profile', p_uid::text,
    jsonb_build_object('from', v_target.role, 'to', p_role));

  return jsonb_build_object('ok', true, 'role', p_role);
end;
$$;

-- Approve a pending staff-role request.
create or replace function public.admin_approve_role_request(p_request uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_req public.role_requests;
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
  v_result jsonb;
begin
  if not public.campusone_has_permission('MANAGE_ROLES') then
    perform public.campusone_audit('admin_approve_role_request', 'role_request', p_request::text,
      '{}'::jsonb, 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'Missing MANAGE_ROLES permission');
  end if;

  select * into v_req from public.role_requests where id = p_request for update;
  if v_req.id is null then
    return jsonb_build_object('ok', false, 'error', 'Request not found');
  end if;
  if v_req.status <> 'PENDING' then
    return jsonb_build_object('ok', false, 'error', 'Request already ' || v_req.status);
  end if;

  v_result := public.admin_set_role(v_req.user_id, v_req.requested_role);
  if not coalesce((v_result ->> 'ok')::boolean, false) then
    return v_result;
  end if;

  update public.role_requests
     set status = 'APPROVED',
         reviewed_by = public.campusone_caller_email(),
         reviewed_at = v_now
   where id = p_request;

  perform public.campusone_audit('admin_approve_role_request', 'role_request', p_request::text,
    jsonb_build_object('user', v_req.user_id, 'role', v_req.requested_role));

  return jsonb_build_object('ok', true);
end;
$$;

create or replace function public.admin_reject_role_request(p_request uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
begin
  if not public.campusone_has_permission('MANAGE_ROLES') then
    perform public.campusone_audit('admin_reject_role_request', 'role_request', p_request::text,
      '{}'::jsonb, 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'Missing MANAGE_ROLES permission');
  end if;

  update public.role_requests
     set status = 'REJECTED',
         reviewed_by = public.campusone_caller_email(),
         reviewed_at = v_now
   where id = p_request and status = 'PENDING';

  if not found then
    return jsonb_build_object('ok', false, 'error', 'Request not found or already reviewed');
  end if;

  perform public.campusone_audit('admin_reject_role_request', 'role_request', p_request::text, '{}'::jsonb);
  return jsonb_build_object('ok', true);
end;
$$;

-- Suspend / reactivate an account. Owner cannot be suspended.
create or replace function public.admin_set_suspended(p_uid uuid, p_suspended boolean)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_target public.profiles;
begin
  if not public.campusone_has_permission('MANAGE_USERS') then
    perform public.campusone_audit('admin_set_suspended', 'profile', p_uid::text,
      jsonb_build_object('suspended', p_suspended), 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'Missing MANAGE_USERS permission');
  end if;

  select * into v_target from public.profiles where id = p_uid;
  if v_target.id is null then
    return jsonb_build_object('ok', false, 'error', 'User not found');
  end if;
  if v_target.id = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    return jsonb_build_object('ok', false, 'error', 'Owner account is protected');
  end if;

  update public.profiles
     set is_suspended = p_suspended,
         role = case when p_suspended then 'STUDENT' else role end,
         updated_at = (extract(epoch from now()) * 1000)::bigint
   where id = p_uid;

  perform public.campusone_audit('admin_set_suspended', 'profile', p_uid::text,
    jsonb_build_object('suspended', p_suspended, 'email', v_target.email));

  return jsonb_build_object('ok', true);
end;
$$;

-- Delete a profile row. Owner cannot be removed.
create or replace function public.admin_delete_user(p_uid uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_target public.profiles;
begin
  if not public.campusone_has_permission('MANAGE_USERS') then
    perform public.campusone_audit('admin_delete_user', 'profile', p_uid::text,
      '{}'::jsonb, 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'Missing MANAGE_USERS permission');
  end if;

  select * into v_target from public.profiles where id = p_uid;
  if v_target.id is null then
    return jsonb_build_object('ok', false, 'error', 'User not found');
  end if;
  if v_target.id = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    return jsonb_build_object('ok', false, 'error', 'Owner account is protected');
  end if;

  delete from public.profiles where id = p_uid;
  perform public.campusone_audit('admin_delete_user', 'profile', p_uid::text,
    jsonb_build_object('email', v_target.email, 'role', v_target.role));

  return jsonb_build_object('ok', true);
end;
$$;

-- Platform settings. Owner only.
create or replace function public.admin_update_settings(p_settings jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_allowed text[] := array[
    'maintenance_mode','maintenance_message','announcement_banner','announcement_enabled',
    'registration_enabled','resource_uploads_enabled','feature_notices','feature_resources',
    'feature_assignments','feature_events','feature_attendance'
  ];
  v_key text;
  v_value text;
  v_before public.platform_settings;
begin
  if not public.campusone_has_permission('MANAGE_SYSTEM_SETTINGS') then
    perform public.campusone_audit('admin_update_settings', 'platform_settings', 'global',
      '{}'::jsonb, 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'Missing MANAGE_SYSTEM_SETTINGS permission');
  end if;

  select * into v_before from public.platform_settings where id = 'global';

  for v_key in select jsonb_object_keys(p_settings) loop
    if not (v_key = any (v_allowed)) then
      return jsonb_build_object('ok', false, 'error', 'Unknown setting: ' || v_key);
    end if;
  end loop;

  update public.platform_settings
     set maintenance_mode = coalesce((p_settings ->> 'maintenance_mode')::boolean, maintenance_mode),
         maintenance_message = coalesce(p_settings ->> 'maintenance_message', maintenance_message),
         announcement_banner = coalesce(p_settings ->> 'announcement_banner', announcement_banner),
         announcement_enabled = coalesce((p_settings ->> 'announcement_enabled')::boolean, announcement_enabled),
         registration_enabled = coalesce((p_settings ->> 'registration_enabled')::boolean, registration_enabled),
         resource_uploads_enabled = coalesce((p_settings ->> 'resource_uploads_enabled')::boolean, resource_uploads_enabled),
         feature_notices = coalesce((p_settings ->> 'feature_notices')::boolean, feature_notices),
         feature_resources = coalesce((p_settings ->> 'feature_resources')::boolean, feature_resources),
         feature_assignments = coalesce((p_settings ->> 'feature_assignments')::boolean, feature_assignments),
         feature_events = coalesce((p_settings ->> 'feature_events')::boolean, feature_events),
         feature_attendance = coalesce((p_settings ->> 'feature_attendance')::boolean, feature_attendance),
         updated_by = public.campusone_caller_email(),
         updated_at = (extract(epoch from now()) * 1000)::bigint
   where id = 'global';

  perform public.campusone_audit('admin_update_settings', 'platform_settings', 'global', p_settings);

  return jsonb_build_object('ok', true);
end;
$$;

-- Moderation decision on a reported content item.
create or replace function public.admin_moderate_content(
  p_target_type text,
  p_target_id text,
  p_action text
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
  v_status text;
begin
  if not public.campusone_has_permission('MODERATE_REPORTS') then
    perform public.campusone_audit('admin_moderate_content', p_target_type, p_target_id,
      jsonb_build_object('action', p_action), 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'Missing MODERATE_REPORTS permission');
  end if;

  if p_action not in ('HIDE', 'RESTORE', 'DELETE', 'DISMISS') then
    return jsonb_build_object('ok', false, 'error', 'Unknown action');
  end if;

  if p_target_type = 'RESOURCE' then
    if p_action = 'HIDE' then
      update public.study_resources set is_hidden = true, is_flagged = false, updated_at = v_now where id = p_target_id;
    elsif p_action = 'RESTORE' then
      update public.study_resources set is_hidden = false, is_flagged = false, updated_at = v_now where id = p_target_id;
    elsif p_action = 'DELETE' then
      delete from public.study_resources where id = p_target_id;
    else
      update public.study_resources set report_count = 0, is_flagged = false, updated_at = v_now where id = p_target_id;
    end if;
  elsif p_target_type = 'CLUB' then
    if p_action = 'HIDE' then
      update public.clubs set is_approved = false, is_flagged = true, updated_at = v_now where id = p_target_id;
    elsif p_action = 'RESTORE' then
      update public.clubs set is_approved = true, is_flagged = false, updated_at = v_now where id = p_target_id;
    elsif p_action = 'DELETE' then
      delete from public.clubs where id = p_target_id;
    else
      update public.clubs set is_flagged = false, updated_at = v_now where id = p_target_id;
    end if;
  elsif p_target_type = 'EVENT' then
    if p_action = 'HIDE' then
      update public.campus_events set is_approved = false, is_flagged = true, updated_at = v_now where id = p_target_id;
    elsif p_action = 'RESTORE' then
      update public.campus_events set is_approved = true, is_flagged = false, updated_at = v_now where id = p_target_id;
    elsif p_action = 'DELETE' then
      delete from public.campus_events where id = p_target_id;
    else
      update public.campus_events set report_count = 0, is_flagged = false, updated_at = v_now where id = p_target_id;
    end if;
  elsif p_target_type = 'LOST_FOUND' then
    if p_action = 'HIDE' then
      update public.lost_found_items set is_hidden = true, updated_at = v_now where id = p_target_id;
    elsif p_action = 'RESTORE' then
      update public.lost_found_items set is_hidden = false, updated_at = v_now where id = p_target_id;
    elsif p_action = 'DELETE' then
      delete from public.lost_found_items where id = p_target_id;
    else
      update public.lost_found_items set report_count = 0, updated_at = v_now where id = p_target_id;
    end if;
  else
    return jsonb_build_object('ok', false, 'error', 'Unsupported target type');
  end if;

  v_status := case when p_action = 'DISMISS' then 'DISMISSED' else 'RESOLVED' end;

  update public.content_reports
     set status = v_status,
         resolution = p_action,
         reviewed_by = public.campusone_caller_email(),
         reviewed_at = v_now
   where target_type = p_target_type and target_id = p_target_id and status = 'PENDING';

  perform public.campusone_audit('admin_moderate_content', p_target_type, p_target_id,
    jsonb_build_object('action', p_action));

  return jsonb_build_object('ok', true);
end;
$$;

-- Owner-only recovery: re-assert SUPER_ADMIN on the owner row.
-- Requires the caller to be the owner AND the row to already be the owner's.
create or replace function public.owner_recover_admin()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_email text := public.campusone_caller_email();
begin
  if v_uid is null or v_uid <> 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    return jsonb_build_object('ok', false, 'error', 'Owner identity required');
  end if;

  insert into public.profiles (id, email, role, is_suspended, is_moderator_approved, created_at, updated_at)
  values (v_uid, 'vvibhansh@gmail.com', 'SUPER_ADMIN', false, true,
          (extract(epoch from now()) * 1000)::bigint,
          (extract(epoch from now()) * 1000)::bigint)
  on conflict (id) do update
    set role = 'SUPER_ADMIN',
        is_suspended = false,
        is_moderator_approved = true,
        updated_at = (extract(epoch from now()) * 1000)::bigint;

  perform public.campusone_audit('owner_recover_admin', 'profile', v_uid::text, '{}'::jsonb);
  return jsonb_build_object('ok', true, 'role', 'SUPER_ADMIN');
end;
$$;

-- =========================================================================
-- ROW LEVEL SECURITY — deny by default
-- =========================================================================
do $$
declare t text;
begin
  foreach t in array array[
    'profiles','subjects','timetable_slots','notices','attendance_logs',
    'assignments','exams','study_resources','syllabus_topics','gpa_semesters',
    'clubs','campus_events','lost_found_items','lost_found_contact_requests',
    'directory_contacts','directory_suggestions','campus_map_places','moderators',
    'role_requests','content_reports','platform_settings','audit_logs',
    'role_permissions','admin_permissions'
  ]
  loop
    execute format('alter table public.%I enable row level security', t);
    -- Clear every pre-existing policy. PostgreSQL ORs permissive policies, so a
    -- leftover `using (true)` write policy would defeat everything below.
    execute format('drop policy if exists "campusone_read_%1$I" on public.%1$I', t);
    execute format('drop policy if exists "campusone_write_%1$I" on public.%1$I', t);
    execute format('drop policy if exists "own_attendance" on public.%1$I', t);
    execute format('drop policy if exists "own_profile" on public.%1$I', t);
    execute format('drop policy if exists "own_gpa" on public.%1$I', t);
    execute format('drop policy if exists "own_role_requests_select" on public.%1$I', t);
    execute format('drop policy if exists "own_role_requests_insert" on public.%1$I', t);
    execute format('drop policy if exists "admin_role_requests_all" on public.%1$I', t);
  end loop;
end $$;

-- ---- Public read content -------------------------------------------------
create policy "campusone_read_subjects" on public.subjects
  for select using (true);
create policy "campusone_read_timetable_slots" on public.timetable_slots
  for select using (true);
create policy "campusone_read_notices" on public.notices
  for select using (true);
create policy "campusone_read_study_resources" on public.study_resources
  for select to authenticated
  using (
    (coalesce(is_hidden, false) = false and coalesce(is_flagged, false) = false)
    or public.campusone_has_permission('MODERATE_REPORTS')
  );
create policy "campusone_read_syllabus_topics" on public.syllabus_topics
  for select using (true);
create policy "campusone_read_clubs" on public.clubs
  for select using (true);
create policy "campusone_read_campus_events" on public.campus_events
  for select using (true);
create policy "campusone_read_lost_found_items" on public.lost_found_items
  for select to authenticated
  using ((coalesce(is_hidden, false) = false)
      or poster_uid = auth.uid()::text
      or public.campusone_has_permission('MODERATE_REPORTS'));
create policy "campusone_read_directory_contacts" on public.directory_contacts
  for select to authenticated
  using (true);
create policy "campusone_read_campus_map_places" on public.campus_map_places
  for select using (true);
create policy "campusone_read_assignments" on public.assignments
  for select using (true);
create policy "campusone_read_exams" on public.exams
  for select using (true);
create policy "campusone_read_platform_settings" on public.platform_settings
  for select using (true);
create policy "campusone_read_admin_permissions" on public.admin_permissions
  for select using (true);
create policy "campusone_read_role_permissions" on public.role_permissions
  for select using (true);

-- ---- Per-user data ------------------------------------------------------
create policy "own_attendance" on public.attendance_logs
  for all to authenticated
  using (auth.uid()::text = user_id)
  with check (auth.uid()::text = user_id);

create policy "own_gpa" on public.gpa_semesters
  for all to authenticated
  using (auth.uid()::text = user_id)
  with check (auth.uid()::text = user_id);

-- Profiles: a user may read and update their own row. Role and suspension are
-- re-derived by the trigger, so a client cannot escalate through this path.
create policy "own_profile" on public.profiles
  for select to authenticated
  using (auth.uid()::text = id::text);

create policy "own_profile_update" on public.profiles
  for update to authenticated
  using (auth.uid()::text = id::text)
  with check (auth.uid()::text = id::text);

-- ---- Role requests ------------------------------------------------------
create policy "own_role_requests_select" on public.role_requests
  for select to authenticated
  using (auth.uid()::text = user_id::text);

create policy "own_role_requests_insert" on public.role_requests
  for insert to authenticated
  with check (
    auth.uid()::text = user_id::text
    and requested_role in ('CR')
    and status = 'PENDING'
  );

-- ---- Admin-only reads ---------------------------------------------------
create policy "admin_read_profiles" on public.profiles
  for select to authenticated
  using (public.campusone_has_permission('VIEW_USERS'));

create policy "admin_read_role_requests" on public.role_requests
  for select to authenticated
  using (public.campusone_has_permission('MANAGE_ROLES'));

-- ---- Content reports -----------------------------------------------------
-- Any signed-in user may file a report against content, but only for
-- themselves. Review and resolution happen through admin_moderate_content().
create policy "user_insert_content_reports" on public.content_reports
  for insert to authenticated
  with check (reporter_id = auth.uid());

create policy "admin_read_content_reports" on public.content_reports
  for select to authenticated
  using (public.campusone_has_permission('MODERATE_REPORTS'));

create policy "admin_read_audit_logs" on public.audit_logs
  for select to authenticated
  using (public.campusone_has_permission('VIEW_AUDIT_LOGS'));

-- Moderators may read the audit trail for their own actions only.
create policy "own_audit_logs" on public.audit_logs
  for select to authenticated
  using (actor_id = auth.uid());

-- ---- Append-only audit --------------------------------------------------
-- No UPDATE or DELETE policy exists on audit_logs, so even the owner cannot
-- rewrite history through the REST API.
revoke update, delete on public.audit_logs from anon, authenticated;

-- The audit writer is an internal helper. Clients must not be able to forge
-- audit entries by calling it directly; the privileged functions call it from
-- inside their own SECURITY DEFINER context.
revoke execute on function public.campusone_audit(text, text, text, jsonb, text)
  from anon, authenticated;

-- ---- Content insert/update for signed-in users --------------------------
-- Content rows are created by CRs/teachers/moderators/admins. The app writes
-- them through the authenticated REST API, so keep write access for signed-in
-- users but never for anon, and never for per-user tables.
do $$
declare t text;
begin
  foreach t in array array[
    'subjects','timetable_slots','notices','assignments','exams',
    'study_resources','syllabus_topics','clubs','campus_events',
    'lost_found_items','lost_found_contact_requests','directory_contacts',
    'directory_suggestions','campus_map_places','moderators'
  ]
  loop
    execute format('drop policy if exists "campusone_write_%1$I" on public.%1$I', t);
    execute format(
      'create policy "campusone_write_%1$I" on public.%1$I for all to authenticated using (true) with check (true)',
      t
    );
  end loop;
end $$;

-- =========================================================================
-- INDEXES
-- =========================================================================
create index if not exists profiles_role_idx on public.profiles (role);
create index if not exists profiles_email_idx on public.profiles (lower(email));
create index if not exists profiles_created_at_idx on public.profiles (created_at desc);
create index if not exists notices_section_idx on public.notices (section_key, created_at desc);
create index if not exists timetable_section_idx on public.timetable_slots (section_key);
create index if not exists assignments_section_idx on public.assignments (section_key);
create index if not exists exams_section_idx on public.exams (section_key);
create index if not exists study_resources_section_idx on public.study_resources (section_key, created_at desc);
create index if not exists study_resources_flag_idx on public.study_resources (is_flagged, report_count desc);
create index if not exists clubs_college_idx on public.clubs (college_id);
create index if not exists campus_events_college_idx on public.campus_events (college_id);
create index if not exists lost_found_college_idx on public.lost_found_items (college_id);

-- =========================================================================
-- SUPABASE STORAGE: study file vault (PDFs / images uploaded by moderators & CRs)
-- Public bucket: anyone can read; only signed-in users can write.
-- Files are content-addressed (resources/<sha256>.<ext>) so duplicate
-- uploads reuse the same object automatically.
-- =========================================================================
insert into storage.buckets (id, name, public)
values ('study-resources', 'study-resources', true)
on conflict (id) do nothing;

drop policy if exists "campusone_storage_read" on storage.objects;
create policy "campusone_storage_read" on storage.objects
  for select using (bucket_id = 'study-resources');

drop policy if exists "campusone_storage_insert" on storage.objects;
create policy "campusone_storage_insert" on storage.objects
  for insert to authenticated with check (bucket_id = 'study-resources');

drop policy if exists "campusone_storage_update" on storage.objects;
create policy "campusone_storage_update" on storage.objects
  for update to authenticated using (bucket_id = 'study-resources')
  with check (bucket_id = 'study-resources');

drop policy if exists "campusone_storage_delete" on storage.objects;
create policy "campusone_storage_delete" on storage.objects
  for delete to authenticated using (bucket_id = 'study-resources');
-- =========================================================================
-- MISSION 4 — COMMUNITY, DISCUSSIONS, NOTIFICATIONS, ANNOUNCEMENTS
-- =========================================================================

-- ---- Official campus announcements --------------------------------------
alter table public.notices add column if not exists category text not null default 'GENERAL';
alter table public.notices add column if not exists priority text not null default 'NORMAL';
alter table public.notices add column if not exists is_official boolean not null default false;

alter table public.notices drop constraint if exists notices_category_check;
alter table public.notices add constraint notices_category_check
  check (category in ('GENERAL','ACADEMIC','EXAM','EVENT','URGENT','PLACEMENT'));

alter table public.notices drop constraint if exists notices_priority_check;
alter table public.notices add constraint notices_priority_check
  check (priority in ('LOW','NORMAL','HIGH','URGENT'));

create or replace function public.campusone_guard_notice()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_is_admin boolean := public.campusone_is_owner()
    or public.campusone_caller_role() = 'ADMIN';
begin
  if tg_op = 'INSERT' then
    if auth.uid() is not null then
      new.author_uid := auth.uid()::text;
      if new.is_official and not v_is_admin then
        raise exception 'Only administrators can publish official announcements';
      end if;
    end if;
    new.created_at := coalesce(nullif(new.created_at, 0),
      (extract(epoch from now()) * 1000)::bigint);
    return new;
  end if;

  if auth.uid() is not null and not v_is_admin then
    new.is_official := old.is_official;
    if old.author_uid <> auth.uid()::text then
      raise exception 'Only the author or an administrator can edit this notice';
    end if;
  end if;
  return new;
end;
$$;

create or replace function public.campusone_guard_notice_delete()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_is_admin boolean := public.campusone_is_owner()
    or public.campusone_caller_role() = 'ADMIN';
begin
  if auth.uid() is not null and not v_is_admin
     and old.author_uid <> auth.uid()::text then
    raise exception 'Only the author or an administrator can delete this notice';
  end if;
  return old;
end;
$$;

drop trigger if exists notices_guard_insert_update on public.notices;
create trigger notices_guard_insert_update
  before insert or update on public.notices
  for each row execute function public.campusone_guard_notice();

drop trigger if exists notices_guard_delete on public.notices;
create trigger notices_guard_delete
  before delete on public.notices
  for each row execute function public.campusone_guard_notice_delete();

-- ---- Community feed ------------------------------------------------------
create table if not exists public.community_posts (
  id text primary key,
  college_id text not null default 'hnbgu',
  author_uid uuid not null references auth.users(id) on delete cascade,
  author_name text not null default 'Student',
  author_role text not null default 'STUDENT',
  category text not null default 'GENERAL',
  title text not null default '',
  body text not null default '',
  like_count integer not null default 0,
  comment_count integer not null default 0,
  report_count integer not null default 0,
  is_hidden boolean not null default false,
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  constraint community_posts_category_check
    check (category in ('GENERAL','ACADEMIC','CAMPUS_LIFE','QUESTION','ANNOUNCEMENT'))
);

create table if not exists public.community_comments (
  id text primary key,
  post_id text not null references public.community_posts(id) on delete cascade,
  author_uid uuid not null references auth.users(id) on delete cascade,
  author_name text not null default 'Student',
  author_role text not null default 'STUDENT',
  body text not null default '',
  is_hidden boolean not null default false,
  created_at bigint not null default 0
);

create table if not exists public.community_likes (
  post_id text not null references public.community_posts(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  created_at bigint not null default 0,
  primary key (post_id, user_id)
);

create index if not exists community_posts_feed_idx
  on public.community_posts (college_id, created_at desc);
create index if not exists community_posts_category_idx
  on public.community_posts (category, created_at desc);
create index if not exists community_posts_author_idx
  on public.community_posts (author_uid, created_at desc);
create index if not exists community_comments_post_idx
  on public.community_comments (post_id, created_at);
create index if not exists community_likes_post_idx
  on public.community_likes (post_id);

create or replace function public.campusone_guard_community_post()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
  v_author public.profiles%rowtype;
begin
  if tg_op = 'INSERT' then
    new.created_at := coalesce(nullif(new.created_at, 0), v_now);
    new.updated_at := v_now;
    new.like_count := 0;
    new.comment_count := 0;
    new.report_count := 0;
    new.is_hidden := false;
    if auth.uid() is not null then
      select * into v_author from public.profiles where id = auth.uid();
      if not found then
        raise exception 'Complete your profile before posting';
      end if;
      new.author_uid := auth.uid();
      new.author_name := coalesce(nullif(v_author.full_name, ''), 'Student');
      new.author_role := coalesce(nullif(v_author.role, ''), 'STUDENT');
    end if;
    return new;
  end if;

  new.author_uid := old.author_uid;
  new.author_name := old.author_name;
  new.author_role := old.author_role;
  new.college_id := old.college_id;
  new.created_at := old.created_at;
  new.updated_at := v_now;
  if auth.uid() is not null and not public.campusone_has_permission('MODERATE_REPORTS') then
    new.like_count := old.like_count;
    new.comment_count := old.comment_count;
    new.report_count := old.report_count;
    new.is_hidden := old.is_hidden;
  end if;
  return new;
end;
$$;

create or replace function public.campusone_guard_community_comment()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_author public.profiles%rowtype;
begin
  if tg_op = 'INSERT' then
    new.created_at := coalesce(nullif(new.created_at, 0),
      (extract(epoch from now()) * 1000)::bigint);
    new.is_hidden := false;
    if auth.uid() is not null then
      select * into v_author from public.profiles where id = auth.uid();
      if not found then
        raise exception 'Complete your profile before commenting';
      end if;
      new.author_uid := auth.uid();
      new.author_name := coalesce(nullif(v_author.full_name, ''), 'Student');
      new.author_role := coalesce(nullif(v_author.role, ''), 'STUDENT');
    end if;
    return new;
  end if;

  new.post_id := old.post_id;
  new.author_uid := old.author_uid;
  new.author_name := old.author_name;
  new.author_role := old.author_role;
  new.created_at := old.created_at;
  if auth.uid() is not null and not public.campusone_has_permission('MODERATE_REPORTS') then
    new.is_hidden := old.is_hidden;
  end if;
  return new;
end;
$$;

drop trigger if exists community_posts_guard on public.community_posts;
create trigger community_posts_guard
  before insert or update on public.community_posts
  for each row execute function public.campusone_guard_community_post();

drop trigger if exists community_comments_guard on public.community_comments;
create trigger community_comments_guard
  before insert or update on public.community_comments
  for each row execute function public.campusone_guard_community_comment();

-- ---- Notifications -------------------------------------------------------
create table if not exists public.notifications (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null default 'SYSTEM',
  title text not null default '',
  body text not null default '',
  target_type text not null default '',
  target_id text not null default '',
  is_read boolean not null default false,
  created_at bigint not null default 0
);

create index if not exists notifications_user_idx
  on public.notifications (user_id, is_read, created_at desc);

create or replace function public.campusone_notify(
  p_user uuid,
  p_type text,
  p_title text,
  p_body text,
  p_target_type text,
  p_target_id text,
  p_id text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_user is null then
    return;
  end if;
  insert into public.notifications (id, user_id, type, title, body, target_type, target_id, is_read, created_at)
  values (p_id, p_user, p_type, coalesce(p_title, ''), coalesce(p_body, ''),
          coalesce(p_target_type, ''), coalesce(p_target_id, ''), false,
          (extract(epoch from now()) * 1000)::bigint)
  on conflict (id) do nothing;
end;
$$;

create or replace function public.campusone_community_like_sync()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_post public.community_posts%rowtype;
begin
  if tg_op = 'INSERT' then
    select * into v_post from public.community_posts where id = new.post_id;
    update public.community_posts
       set like_count = (select count(*) from public.community_likes where post_id = new.post_id)
     where id = new.post_id;
    if found and v_post.author_uid <> new.user_id then
      perform public.campusone_notify(
        v_post.author_uid, 'COMMUNITY_LIKE', 'New like on your post',
        left(v_post.title, 120), 'POST', v_post.id,
        'like_' || new.post_id || '_' || new.user_id::text);
    end if;
  else
    update public.community_posts
       set like_count = (select count(*) from public.community_likes where post_id = old.post_id)
     where id = old.post_id;
  end if;
  return null;
end;
$$;

create or replace function public.campusone_community_comment_sync()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_post public.community_posts%rowtype;
  v_profile public.profiles%rowtype;
  v_mentioned public.profiles%rowtype;
begin
  if tg_op = 'INSERT' then
    select * into v_post from public.community_posts where id = new.post_id;
    update public.community_posts
       set comment_count = (select count(*) from public.community_comments
                             where post_id = new.post_id and not is_hidden)
     where id = new.post_id;
    if found and not new.is_hidden then
      if v_post.author_uid <> new.author_uid then
        perform public.campusone_notify(
          v_post.author_uid, 'COMMUNITY_COMMENT', 'New comment on your post',
          left(new.body, 200), 'POST', v_post.id,
          'comment_' || new.id || '_' || v_post.author_uid::text);
      end if;
      select * into v_profile from public.profiles where id = new.author_uid;
      if found and v_profile.college_id is not null then
        for v_mentioned in
          select * from public.profiles p
           where p.id <> new.author_uid
             and p.college_id = v_profile.college_id
             and strpos(new.body, '@' || p.full_name) > 0
           limit 5
        loop
          perform public.campusone_notify(
            v_mentioned.id, 'COMMUNITY_MENTION', 'You were mentioned',
            left(new.body, 200), 'POST', v_post.id,
            'mention_' || new.id || '_' || v_mentioned.id::text);
        end loop;
      end if;
    end if;
  elsif tg_op = 'UPDATE' then
    update public.community_posts
       set comment_count = (select count(*) from public.community_comments
                             where post_id = new.post_id and not is_hidden)
     where id = new.post_id;
  else
    update public.community_posts
       set comment_count = (select count(*) from public.community_comments
                             where post_id = old.post_id and not is_hidden)
     where id = old.post_id;
  end if;
  return null;
end;
$$;

drop trigger if exists community_likes_sync on public.community_likes;
create trigger community_likes_sync
  after insert or delete on public.community_likes
  for each row execute function public.campusone_community_like_sync();

drop trigger if exists community_comments_sync on public.community_comments;
create trigger community_comments_sync
  after insert or delete or update of is_hidden on public.community_comments
  for each row execute function public.campusone_community_comment_sync();

create or replace function public.campusone_notice_notify()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_type text := case when new.is_official then 'ANNOUNCEMENT' else 'NOTICE' end;
begin
  insert into public.notifications (id, user_id, type, title, body, target_type, target_id, is_read, created_at)
  select 'notice_' || new.id || '_' || p.id,
         p.id,
         v_type,
         new.title,
         left(new.body, 200),
         'NOTICE',
         new.id,
         false,
         (extract(epoch from now()) * 1000)::bigint
    from public.profiles p
   where p.role = 'STUDENT' and (new.section_key = 'global'
          or new.section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section)
     and p.id <> coalesce(auth.uid(), '00000000-0000-0000-0000-000000000000'::uuid)
  on conflict (id) do nothing;
  return null;
end;
$$;

drop trigger if exists notices_notify on public.notices;
create trigger notices_notify
  after insert on public.notices
  for each row execute function public.campusone_notice_notify();

create or replace function public.campusone_assignment_notify()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.notifications (id, user_id, type, title, body, target_type, target_id, is_read, created_at)
  select 'assignment_' || new.id || '_' || p.id,
         p.id,
         'ASSIGNMENT',
         'New assignment: ' || new.title,
         coalesce(nullif(new.subject_tag, ''), new.subject_id)
           || ' • due ' || to_char(to_timestamp(new.due_date_epoch_millis / 1000.0), 'DD Mon, HH24:MI'),
         'ASSIGNMENT',
         new.id,
         false,
         (extract(epoch from now()) * 1000)::bigint
    from public.profiles p
   where p.role = 'STUDENT' and new.section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
     and p.id <> coalesce(auth.uid(), '00000000-0000-0000-0000-000000000000'::uuid)
  on conflict (id) do nothing;
  return null;
end;
$$;

drop trigger if exists assignments_notify on public.assignments;
create trigger assignments_notify
  after insert on public.assignments
  for each row execute function public.campusone_assignment_notify();

-- ---- RLS for community + notifications -----------------------------------
alter table public.community_posts enable row level security;
alter table public.community_comments enable row level security;
alter table public.community_likes enable row level security;
alter table public.notifications enable row level security;
alter table public.notices enable row level security;

drop policy if exists notices_select on public.notices;
create policy notices_select on public.notices
  for select to authenticated using (true);
drop policy if exists notices_write on public.notices;
drop policy if exists notices_admin_manage on public.notices;
drop policy if exists notices_owner_manage on public.notices;
create policy notices_owner_manage on public.notices
  for all to authenticated
  using (public.campusone_is_owner() or public.campusone_caller_role() = 'ADMIN')
  with check (public.campusone_is_owner() or public.campusone_caller_role() = 'ADMIN');
drop policy if exists notices_moderator_manage on public.notices;
create policy notices_moderator_manage on public.notices
  for all to authenticated
  using (
    public.campusone_caller_role() = 'MODERATOR'
    and is_official = false
    and exists (
      select 1 from public.profiles p
      where p.id = auth.uid()
        and (section_key = 'global' or left(section_key, length(p.college_id) + 1) = p.college_id || '_')
    )
  )
  with check (
    public.campusone_caller_role() = 'MODERATOR'
    and is_official = false
    and author_uid = auth.uid()::text
    and author_role = 'MODERATOR'
    and exists (
      select 1 from public.profiles p
      where p.id = auth.uid()
        and (section_key = 'global' or left(section_key, length(p.college_id) + 1) = p.college_id || '_')
    )
  );
drop policy if exists notices_cr_insert_section on public.notices;
create policy notices_cr_insert_section on public.notices
  for insert to authenticated
  with check (
    author_uid = auth.uid()::text and author_role = 'CR' and is_official = false
    and exists (
      select 1 from public.profiles p
      where p.id = auth.uid() and p.role = 'CR' and p.verified_role = true
        and section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    )
  );
drop policy if exists notices_cr_update_own_section on public.notices;
create policy notices_cr_update_own_section on public.notices
  for update to authenticated
  using (
    author_uid = auth.uid()::text and author_role = 'CR' and is_official = false
    and exists (
      select 1 from public.profiles p
      where p.id = auth.uid() and p.role = 'CR' and p.verified_role = true
        and section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    )
  )
  with check (
    author_uid = auth.uid()::text and author_role = 'CR' and is_official = false
    and exists (
      select 1 from public.profiles p
      where p.id = auth.uid() and p.role = 'CR' and p.verified_role = true
        and section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    )
  );
drop policy if exists notices_cr_delete_own_section on public.notices;
create policy notices_cr_delete_own_section on public.notices
  for delete to authenticated
  using (
    author_uid = auth.uid()::text and author_role = 'CR' and is_official = false
    and exists (
      select 1 from public.profiles p
      where p.id = auth.uid() and p.role = 'CR' and p.verified_role = true
        and section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    )
  );

drop policy if exists "community_posts_read" on public.community_posts;
create policy "community_posts_read" on public.community_posts
  for select to authenticated
  using (not is_hidden or public.campusone_has_permission('MODERATE_REPORTS'));

drop policy if exists "community_posts_insert" on public.community_posts;
create policy "community_posts_insert" on public.community_posts
  for insert to authenticated
  with check (author_uid = auth.uid());

drop policy if exists "community_posts_update_own" on public.community_posts;
create policy "community_posts_update_own" on public.community_posts
  for update to authenticated
  using (author_uid = auth.uid())
  with check (author_uid = auth.uid());

drop policy if exists "community_posts_delete_own" on public.community_posts;
create policy "community_posts_delete_own" on public.community_posts
  for delete to authenticated
  using (author_uid = auth.uid());

drop policy if exists "community_posts_admin" on public.community_posts;
create policy "community_posts_admin" on public.community_posts
  for all to authenticated
  using (public.campusone_has_permission('MODERATE_REPORTS'))
  with check (public.campusone_has_permission('MODERATE_REPORTS'));

drop policy if exists "community_comments_read" on public.community_comments;
create policy "community_comments_read" on public.community_comments
  for select to authenticated
  using (
    (not is_hidden or public.campusone_has_permission('MODERATE_REPORTS'))
    and exists (
      select 1 from public.community_posts p
       where p.id = community_comments.post_id
         and (not p.is_hidden or public.campusone_has_permission('MODERATE_REPORTS'))
    )
  );

drop policy if exists "community_comments_insert" on public.community_comments;
create policy "community_comments_insert" on public.community_comments
  for insert to authenticated
  with check (
    author_uid = auth.uid()
    and exists (select 1 from public.community_posts p where p.id = post_id and not p.is_hidden)
  );

drop policy if exists "community_comments_update_own" on public.community_comments;
create policy "community_comments_update_own" on public.community_comments
  for update to authenticated
  using (author_uid = auth.uid())
  with check (author_uid = auth.uid());

drop policy if exists "community_comments_delete_own" on public.community_comments;
create policy "community_comments_delete_own" on public.community_comments
  for delete to authenticated
  using (author_uid = auth.uid());

drop policy if exists "community_comments_admin" on public.community_comments;
create policy "community_comments_admin" on public.community_comments
  for all to authenticated
  using (public.campusone_has_permission('MODERATE_REPORTS'))
  with check (public.campusone_has_permission('MODERATE_REPORTS'));

drop policy if exists "community_likes_read" on public.community_likes;
create policy "community_likes_read" on public.community_likes
  for select to authenticated
  using (true);

drop policy if exists "community_likes_insert_own" on public.community_likes;
create policy "community_likes_insert_own" on public.community_likes
  for insert to authenticated
  with check (user_id = auth.uid());

drop policy if exists "community_likes_delete_own" on public.community_likes;
create policy "community_likes_delete_own" on public.community_likes
  for delete to authenticated
  using (user_id = auth.uid());

drop policy if exists "notifications_read_own" on public.notifications;
create policy "notifications_read_own" on public.notifications
  for select to authenticated
  using (user_id = auth.uid());

drop policy if exists "notifications_update_own" on public.notifications;
create policy "notifications_update_own" on public.notifications
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- ---- Moderation RPC for community content --------------------------------
create or replace function public.admin_moderate_community(
  p_target_type text,
  p_target_id text,
  p_action text
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
  v_status text;
begin
  if not public.campusone_has_permission('MODERATE_REPORTS') then
    perform public.campusone_audit('admin_moderate_community', p_target_type, p_target_id,
      jsonb_build_object('action', p_action), 'DENIED');
    return jsonb_build_object('ok', false, 'error', 'Missing MODERATE_REPORTS permission');
  end if;

  if p_action not in ('HIDE', 'RESTORE', 'DELETE', 'DISMISS') then
    return jsonb_build_object('ok', false, 'error', 'Unknown action');
  end if;

  if p_target_type = 'POST' then
    if p_action = 'HIDE' then
      update public.community_posts set is_hidden = true, updated_at = v_now where id = p_target_id;
    elsif p_action = 'RESTORE' then
      update public.community_posts set is_hidden = false, updated_at = v_now where id = p_target_id;
    elsif p_action = 'DELETE' then
      delete from public.community_posts where id = p_target_id;
    else
      update public.community_posts set report_count = 0, updated_at = v_now where id = p_target_id;
    end if;
  elsif p_target_type = 'COMMENT' then
    if p_action = 'HIDE' then
      update public.community_comments set is_hidden = true where id = p_target_id;
    elsif p_action = 'RESTORE' then
      update public.community_comments set is_hidden = false where id = p_target_id;
    elsif p_action = 'DELETE' then
      delete from public.community_comments where id = p_target_id;
    end if;
  else
    return jsonb_build_object('ok', false, 'error', 'Unsupported target type');
  end if;

  v_status := case when p_action = 'DISMISS' then 'DISMISSED' else 'RESOLVED' end;

  update public.content_reports
     set status = v_status,
         resolution = p_action,
         reviewed_by = public.campusone_caller_email(),
         reviewed_at = v_now
   where target_type = p_target_type and target_id = p_target_id and status = 'PENDING';

  perform public.campusone_audit('admin_moderate_community', p_target_type, p_target_id,
    jsonb_build_object('action', p_action));

  return jsonb_build_object('ok', true);
end;
$$;

-- ---- Grants --------------------------------------------------------------
grant select on public.community_posts to authenticated;
grant select on public.community_comments to authenticated;
grant select on public.community_likes to authenticated;
grant select on public.notifications to authenticated;

grant insert, update, delete on public.community_posts to authenticated;
grant insert, update, delete on public.community_comments to authenticated;
grant insert, delete on public.community_likes to authenticated;
grant update on public.notifications to authenticated;

revoke all on public.community_posts from anon;
revoke all on public.community_comments from anon;
revoke all on public.community_likes from anon;
revoke all on public.notifications from anon;

revoke execute on function public.campusone_notify(uuid, text, text, text, text, text, text)
  from public, anon, authenticated;
revoke execute on function public.admin_moderate_community(text, text, text)
  from public, anon;
grant execute on function public.admin_moderate_community(text, text, text)
  to authenticated;

-- =========================================================================
-- MISSION 6 — WRITE SCOPE HARDENING (deny-by-default for students)
--
-- The legacy bootstrap granted every authenticated account full write access
-- to the academic and administrative tables ("campusone_write_<table>" with
-- using (true) / with check (true)). The app hides those surfaces in the UI,
-- but the REST API accepted the writes anyway. This block replaces the blanket
-- policies with role-scoped ones so authorization is enforced by Postgres.
--
-- Rules
--   * Staff = owner, ADMIN, MODERATOR, CR (server-derived role).
--   * Students keep the writes the product actually offers them: uploading a
--     study resource, submitting a directory suggestion, requesting the
--     owner of a lost-and-found item, marking their own attendance, posting
--     community content and reporting content.
--   * Everything else (exams, timetable slots, assignments, notices, clubs,
--     events, directory, map, moderator roster) is staff-only.
--   * Students can never edit or delete rows they did not create, and can
--     never flip moderation flags (approval, hidden, flagged, upvotes).
--
-- Rollback: re-run the legacy bootstrap block that recreates
-- "campusone_write_<table>" policies with using (true) / with check (true).
-- =========================================================================

create or replace function public.campusone_is_staff()
returns boolean
language sql
stable
security definer
set search_path = public
as $
  select public.campusone_is_owner()
      or exists (
        select 1 from public.profiles p
        where p.id = auth.uid()
          and p.role in ('ADMIN', 'CR', 'MODERATOR')
          and p.is_suspended = false
      );
$;

create or replace function public.campusone_is_catalog_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.campusone_is_owner()
      or public.campusone_caller_role() = 'ADMIN';
$$;

create or replace function public.campusone_own_section_key()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    from public.profiles p
   where p.id = auth.uid();
$$;

-- Drop the legacy blanket write policies.
do $$
declare t text;
begin
  foreach t in array array[
    'subjects','timetable_slots','notices','assignments','exams',
    'study_resources','syllabus_topics','clubs','campus_events',
    'lost_found_items','lost_found_contact_requests','directory_contacts',
    'directory_suggestions','campus_map_places','moderators'
  ]
  loop
    execute format('drop policy if exists "campusone_write_%1$I" on public.%1$I', t);
  end loop;
end $$;

-- ---------------------------------------------------------------------------
-- Staff-only surfaces
-- ---------------------------------------------------------------------------
do $$
declare t text;
begin
  foreach t in array array[
    'subjects','syllabus_topics','timetable_slots','notices','assignments',
    'clubs','campus_events','lost_found_items','directory_contacts',
    'campus_map_places'
  ]
  loop
    execute format('drop policy if exists "campusone_staff_write_%1$I" on public.%1$I', t);
    execute format(
      'create policy "campusone_staff_write_%1$I" on public.%1$I
         for all to authenticated
         using (public.campusone_is_staff())
         with check (public.campusone_is_staff())', t);
  end loop;
end $$;

-- Exams and the moderator roster are administrator-only.
drop policy if exists "campusone_staff_write_exams" on public.exams;
create policy "campusone_staff_write_exams" on public.exams
  for all to authenticated
  using (public.campusone_is_catalog_admin())
  with check (public.campusone_is_catalog_admin());

drop policy if exists "campusone_read_moderators" on public.moderators;
create policy "campusone_read_moderators" on public.moderators
  for select to authenticated
  using (true);

drop policy if exists "campusone_staff_write_moderators" on public.moderators;
create policy "campusone_staff_write_moderators" on public.moderators
  for all to authenticated
  using (public.campusone_is_catalog_admin())
  with check (public.campusone_is_catalog_admin());

-- ---------------------------------------------------------------------------
-- Study resources: students may upload, but only touch their own rows and
-- can never approve, hide, flag or upvote their own content.
-- ---------------------------------------------------------------------------
drop policy if exists "campusone_write_study_resources" on public.study_resources;
create policy "campusone_write_study_resources" on public.study_resources
  for insert to authenticated
  with check (
    public.campusone_is_staff()
    or uploader_uid = auth.uid()::text
  );

drop policy if exists "campusone_update_study_resources" on public.study_resources;
create policy "campusone_update_study_resources" on public.study_resources
  for update to authenticated
  using (
    public.campusone_is_staff()
    or uploader_uid = auth.uid()::text
  )
  with check (
    public.campusone_is_staff()
    or uploader_uid = auth.uid()::text
  );

drop policy if exists "campusone_delete_study_resources" on public.study_resources;
create policy "campusone_delete_study_resources" on public.study_resources
  for delete to authenticated
  using (
    public.campusone_is_staff()
    or uploader_uid = auth.uid()::text
  );

create or replace function public.campusone_guard_study_resource()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if public.campusone_is_staff() then
    return new;
  end if;

  if tg_op = 'INSERT' then
    if auth.uid() is null then
      raise exception 'Authenticated user required to create a study resource';
    end if;
    new.uploader_uid := auth.uid()::text;
    new.is_approved_by_cr := false;
    new.is_hidden := false;
    new.is_flagged := false;
    new.upvotes := 0;
    new.report_count := 0;
  else
    new.uploader_uid := old.uploader_uid;
    new.uploader_name := old.uploader_name;
    new.is_approved_by_cr := old.is_approved_by_cr;
    new.is_hidden := old.is_hidden;
    new.is_flagged := old.is_flagged;
    new.upvotes := old.upvotes;
    new.report_count := old.report_count;
    new.upload_status := old.upload_status;
    new.file_url := old.file_url;
    new.file_type := old.file_type;
    new.file_size_bytes := old.file_size_bytes;
    new.file_hash := old.file_hash;
  end if;
  return new;
end;
$$;

drop trigger if exists study_resources_guard on public.study_resources;
create trigger study_resources_guard
  before insert or update on public.study_resources
  for each row execute function public.campusone_guard_study_resource();

-- ---------------------------------------------------------------------------
-- Student-submitted rows: the student may create and manage only their own.
-- ---------------------------------------------------------------------------
drop policy if exists "campusone_lost_found_contact_requests_select" on public.lost_found_contact_requests;
create policy "campusone_lost_found_contact_requests_select" on public.lost_found_contact_requests
  for select to authenticated
  using (
    requester_uid = auth.uid()::text
    or exists (
      select 1 from public.lost_found_items li
      where li.id = item_id and li.poster_uid = auth.uid()::text
    )
    or public.campusone_is_staff()
  );

drop policy if exists "campusone_write_lost_found_contact_requests" on public.lost_found_contact_requests;
create policy "campusone_write_lost_found_contact_requests" on public.lost_found_contact_requests
  for insert to authenticated
  with check (
    requester_uid = auth.uid()::text
    and status = 'PENDING'
    and exists (
      select 1 from public.lost_found_items li
      where li.id = item_id and coalesce(li.is_hidden, false) = false
    )
  );

drop policy if exists "campusone_update_lost_found_contact_requests" on public.lost_found_contact_requests;
create policy "campusone_update_lost_found_contact_requests" on public.lost_found_contact_requests
  for update to authenticated
  using (public.campusone_is_staff())
  with check (public.campusone_is_staff());

drop policy if exists "campusone_delete_lost_found_contact_requests" on public.lost_found_contact_requests;
create policy "campusone_delete_lost_found_contact_requests" on public.lost_found_contact_requests
  for delete to authenticated
  using (
    (requester_uid = auth.uid()::text and status = 'PENDING')
    or public.campusone_is_staff()
  );

drop policy if exists "campusone_directory_suggestions_select" on public.directory_suggestions;
create policy "campusone_directory_suggestions_select" on public.directory_suggestions
  for select to authenticated
  using (submitted_by = auth.uid()::text or public.campusone_is_staff());

drop policy if exists "campusone_write_directory_suggestions" on public.directory_suggestions;
create policy "campusone_write_directory_suggestions" on public.directory_suggestions
  for insert to authenticated
  with check (submitted_by = auth.uid()::text and status = 'PENDING');

drop policy if exists "campusone_update_directory_suggestions" on public.directory_suggestions;
create policy "campusone_update_directory_suggestions" on public.directory_suggestions
  for update to authenticated
  using (public.campusone_is_staff())
  with check (public.campusone_is_staff());

drop policy if exists "campusone_delete_directory_suggestions" on public.directory_suggestions;
create policy "campusone_delete_directory_suggestions" on public.directory_suggestions
  for delete to authenticated
  using (public.campusone_is_staff());

-- ---------------------------------------------------------------------------
-- Grants follow the policies: the anon key still only reaches what RLS allows.
-- ---------------------------------------------------------------------------
grant select, insert, update, delete on public.study_resources to authenticated;
grant select, insert, update, delete on public.directory_suggestions to authenticated;
grant select, insert, update, delete on public.lost_found_contact_requests to authenticated;
grant select, insert, update, delete on public.moderators to authenticated;
revoke all on public.directory_suggestions from anon;
revoke all on public.lost_found_contact_requests from anon;
revoke all on public.moderators from anon;

revoke execute on function public.campusone_is_staff() from public, anon;
revoke execute on function public.campusone_is_catalog_admin() from public, anon;
revoke execute on function public.campusone_own_section_key() from public, anon;
grant execute on function public.campusone_is_staff() to authenticated;
grant execute on function public.campusone_is_catalog_admin() to authenticated;
grant execute on function public.campusone_own_section_key() to authenticated;

-- Student-owned rows keep a staff-controlled status (approved / reviewed).
create or replace function public.campusone_guard_status_only_staff()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.campusone_is_staff() then
    new.status := old.status;
  end if;
  return new;
end;
$$;

drop trigger if exists directory_suggestions_guard on public.directory_suggestions;
create trigger directory_suggestions_guard
  before update on public.directory_suggestions
  for each row execute function public.campusone_guard_status_only_staff();

drop trigger if exists lost_found_contact_requests_guard on public.lost_found_contact_requests;
create trigger lost_found_contact_requests_guard
  before update on public.lost_found_contact_requests
  for each row execute function public.campusone_guard_status_only_staff();

-- =========================================================================
-- MISSION 7 — ANNOUNCEMENTS REPAIR + DYNAMIC COURSE TIMETABLES
-- =========================================================================
-- The client writes notice metadata (category / priority / is_official) that
-- older deployments never received. Without these columns PostgREST rejects
-- the whole announcement and nothing reaches the backend.
alter table public.notices add column if not exists category text not null default 'GENERAL';
alter table public.notices add column if not exists priority text not null default 'NORMAL';
alter table public.notices add column if not exists is_official boolean not null default false;
alter table public.notices add column if not exists updated_at bigint not null default 0;

alter table public.notices drop constraint if exists notices_category_check;
alter table public.notices add constraint notices_category_check
  check (category in ('GENERAL','ACADEMIC','EXAM','EVENT','URGENT','PLACEMENT'));

alter table public.notices drop constraint if exists notices_priority_check;
alter table public.notices add constraint notices_priority_check
  check (priority in ('LOW','NORMAL','HIGH','URGENT'));

-- New capability: timetable publication is administrator-only.
insert into public.admin_permissions (code, description, is_owner_only) values
  ('MANAGE_TIMETABLE', 'Upload, publish, replace and delete course timetables', false)
on conflict (code) do nothing;

insert into public.role_permissions (role, permission)
select 'ADMIN', 'MANAGE_TIMETABLE'
on conflict do nothing;

-- Timetable rows now carry their full academic scope so a student can only
-- receive the timetable that belongs to their profile.
alter table public.timetable_slots add column if not exists course_code text not null default '';
alter table public.timetable_slots add column if not exists course_name text not null default '';
alter table public.timetable_slots add column if not exists semester integer not null default 0;
alter table public.timetable_slots add column if not exists section text not null default '';
alter table public.timetable_slots add column if not exists academic_year text not null default '';
alter table public.timetable_slots add column if not exists import_id uuid;

create index if not exists timetable_scope_idx
  on public.timetable_slots (course_code, semester, section, academic_year);
create index if not exists timetable_course_name_idx
  on public.timetable_slots (course_name, semester, section, academic_year);
create index if not exists timetable_import_id_idx
  on public.timetable_slots (import_id);

create table if not exists public.timetable_imports (
  id uuid primary key default gen_random_uuid(),
  course_code text not null,
  course_name text not null default '',
  semester integer not null,
  section text not null default 'A',
  academic_year text not null,
  file_name text not null default '',
  row_count integer not null default 0,
  status text not null default 'PUBLISHED',
  uploaded_by uuid,
  uploaded_by_email text not null default '',
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  replaced_at bigint not null default 0,
  constraint timetable_imports_status_check check (status in ('PUBLISHED','REPLACED')),
  constraint timetable_imports_semester_check check (semester between 1 and 12)
);

create index if not exists timetable_imports_scope_idx
  on public.timetable_imports (course_code, semester, section, academic_year, created_at desc);
create index if not exists timetable_imports_created_idx
  on public.timetable_imports (created_at desc);

alter table public.timetable_imports enable row level security;
drop policy if exists "campusone_read_timetable_imports" on public.timetable_imports;
create policy "campusone_read_timetable_imports" on public.timetable_imports
  for select to authenticated
  using (public.campusone_has_permission('MANAGE_TIMETABLE'));

-- No REST writes: publication and deletion go through the audited RPCs below.
revoke insert, update, delete on public.timetable_imports from anon, authenticated;
revoke insert, update, delete on public.timetable_slots from anon, authenticated;

drop policy if exists "campusone_read_timetable_slots" on public.timetable_slots;
drop policy if exists "campusone_staff_write_timetable_slots" on public.timetable_slots;

create policy "campusone_read_own_timetable" on public.timetable_slots
  for select to authenticated
  using (
    public.campusone_has_permission('MANAGE_TIMETABLE')
    or exists (
      select 1
        from public.profiles p
       where p.id = auth.uid()
         and p.semester = timetable_slots.semester
         and upper(coalesce(p.section, '')) = upper(timetable_slots.section)
         and (
           p.course_id = timetable_slots.course_code
           or (p.course_name <> '' and p.course_name = timetable_slots.course_name)
         )
    )
  );

create or replace function public.campusone_publish_timetable(
  p_course_code text,
  p_course_name text,
  p_semester integer,
  p_section text,
  p_academic_year text,
  p_file_name text,
  p_slots jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_import_id uuid := gen_random_uuid();
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
  v_errors jsonb := '[]'::jsonb;
  v_row record;
  v_section_key text;
  v_count integer := 0;
  v_course_code text := btrim(coalesce(p_course_code, ''));
  v_course_name text := btrim(coalesce(p_course_name, ''));
  v_section text := upper(btrim(coalesce(p_section, '')));
  v_academic_year text := btrim(coalesce(p_academic_year, ''));
begin
  if not (public.campusone_is_owner() or public.campusone_has_permission('MANAGE_TIMETABLE')) then
    return jsonb_build_object('ok', false, 'error', 'Not authorized to publish timetables');
  end if;

  if v_course_code = '' then
    return jsonb_build_object('ok', false, 'error', 'course_code is required');
  end if;
  if p_semester is null or p_semester not between 1 and 12 then
    return jsonb_build_object('ok', false, 'error', 'semester must be between 1 and 12');
  end if;
  if v_section = '' then
    return jsonb_build_object('ok', false, 'error', 'section is required');
  end if;
  if v_academic_year !~ '^[0-9]{4}-[0-9]{2}$' then
    return jsonb_build_object('ok', false, 'error', 'academic_year must look like 2026-27');
  end if;
  if p_slots is null or jsonb_typeof(p_slots) <> 'array' or jsonb_array_length(p_slots) = 0 then
    return jsonb_build_object('ok', false, 'error', 'The CSV contains no timetable rows');
  end if;

  for v_row in
    select * from jsonb_to_recordset(p_slots) as x(
      row_no integer,
      day integer,
      subject_code text,
      subject_name text,
      start_time text,
      end_time text,
      room text
    )
  loop
    if v_row.day is null or v_row.day not between 1 and 7 then
      v_errors := v_errors || jsonb_build_array(jsonb_build_object(
        'row', coalesce(v_row.row_no, 0), 'error', 'Day must be between 1 (Monday) and 7 (Sunday)'));
    end if;
    if coalesce(v_row.subject_code, '') = '' then
      v_errors := v_errors || jsonb_build_array(jsonb_build_object(
        'row', coalesce(v_row.row_no, 0), 'error', 'subject_code is required'));
    end if;
    if coalesce(v_row.subject_name, '') = '' then
      v_errors := v_errors || jsonb_build_array(jsonb_build_object(
        'row', coalesce(v_row.row_no, 0), 'error', 'subject_name is required'));
    end if;
    if coalesce(v_row.start_time, '') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$' then
      v_errors := v_errors || jsonb_build_array(jsonb_build_object(
        'row', coalesce(v_row.row_no, 0), 'error', 'start_time must use HH:mm'));
    end if;
    if coalesce(v_row.end_time, '') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$' then
      v_errors := v_errors || jsonb_build_array(jsonb_build_object(
        'row', coalesce(v_row.row_no, 0), 'error', 'end_time must use HH:mm'));
    end if;
    if coalesce(v_row.start_time, '') <> ''
       and coalesce(v_row.end_time, '') <> ''
       and v_row.end_time <= v_row.start_time then
      v_errors := v_errors || jsonb_build_array(jsonb_build_object(
        'row', coalesce(v_row.row_no, 0), 'error', 'end_time must be after start_time'));
    end if;
  end loop;

  if jsonb_array_length(v_errors) > 0 then
    return jsonb_build_object('ok', false, 'error', 'Validation failed', 'errors', v_errors);
  end if;

  if exists (
    select 1
      from jsonb_to_recordset(p_slots) as x(
        row_no integer, day integer, subject_code text, subject_name text,
        start_time text, end_time text, room text)
     group by x.day, x.start_time
    having count(*) > 1
  ) then
    return jsonb_build_object('ok', false, 'error', 'Duplicate day/start_time entries in the CSV');
  end if;

  v_section_key := lower(v_course_code) || '_sem' || p_semester || '_sec' || v_section;

  update public.timetable_imports
     set status = 'REPLACED', replaced_at = v_now
   where course_code = v_course_code
     and semester = p_semester
     and upper(section) = v_section
     and academic_year = v_academic_year
     and status = 'PUBLISHED';

  delete from public.timetable_slots
   where course_code = v_course_code
     and semester = p_semester
     and upper(section) = v_section
     and academic_year = v_academic_year;

  insert into public.timetable_imports (
    id, course_code, course_name, semester, section, academic_year,
    file_name, row_count, status, uploaded_by, uploaded_by_email, created_at
  ) values (
    v_import_id, v_course_code, v_course_name, p_semester, v_section, v_academic_year,
    coalesce(p_file_name, ''), jsonb_array_length(p_slots), 'PUBLISHED',
    auth.uid(), public.campusone_caller_email(), v_now
  );

  insert into public.timetable_slots (
    id, section_key, subject_id, subject_name, subject_code,
    room_number, day_of_week, start_time, end_time, is_elective, updated_at,
    course_code, course_name, semester, section, academic_year, import_id
  )
  select v_import_id::text || '_' || row_number() over (order by x.row_no),
         v_section_key,
         coalesce(x.subject_code, ''),
         coalesce(x.subject_name, ''),
         coalesce(x.subject_code, ''),
         coalesce(x.room, ''),
         x.day,
         x.start_time,
         x.end_time,
         false,
         v_now,
         v_course_code,
         v_course_name,
         p_semester,
         v_section,
         v_academic_year,
         v_import_id
    from jsonb_to_recordset(p_slots) as x(
      row_no integer, day integer, subject_code text, subject_name text,
      start_time text, end_time text, room text);

  get diagnostics v_count = row_count;

  perform public.campusone_audit(
    'timetable_publish', 'timetable_import', v_import_id::text,
    jsonb_build_object(
      'courseCode', v_course_code,
      'courseName', v_course_name,
      'semester', p_semester,
      'section', v_section,
      'academicYear', v_academic_year,
      'rowCount', v_count
    ),
    'SUCCESS'
  );

  return jsonb_build_object(
    'ok', true,
    'importId', v_import_id,
    'rowCount', v_count,
    'sectionKey', v_section_key
  );
exception when others then
  return jsonb_build_object('ok', false, 'error', SQLERRM);
end;
$$;

create or replace function public.campusone_delete_timetable(p_import_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_import public.timetable_imports%rowtype;
begin
  if not (public.campusone_is_owner() or public.campusone_has_permission('MANAGE_TIMETABLE')) then
    return jsonb_build_object('ok', false, 'error', 'Not authorized to delete timetables');
  end if;

  select * into v_import from public.timetable_imports where id = p_import_id;
  if not found then
    return jsonb_build_object('ok', false, 'error', 'Timetable upload not found');
  end if;

  delete from public.timetable_slots where import_id = p_import_id;
  delete from public.timetable_imports where id = p_import_id;

  perform public.campusone_audit(
    'timetable_delete', 'timetable_import', p_import_id::text,
    jsonb_build_object(
      'courseCode', v_import.course_code,
      'semester', v_import.semester,
      'section', v_import.section,
      'academicYear', v_import.academic_year
    ),
    'SUCCESS'
  );

  return jsonb_build_object('ok', true);
end;
$$;

revoke execute on function public.campusone_publish_timetable(text, text, integer, text, text, text, jsonb) from public, anon;
revoke execute on function public.campusone_delete_timetable(uuid) from public, anon;
grant execute on function public.campusone_publish_timetable(text, text, integer, text, text, text, jsonb) to authenticated;
grant execute on function public.campusone_delete_timetable(uuid) to authenticated;

-- =========================================================================
-- MISSION 8 — REGISTERED-USER DIRECTORY REPAIR
-- public.profiles must mirror every auth.users record. Profiles are created
-- lazily by campusone_bootstrap_profile(), so accounts that skipped or failed
-- onboarding never appeared in the admin directory. Idempotent and safe to re-run.
-- =========================================================================

-- 1. Provision a profile automatically for every new auth user.
create or replace function public.campusone_handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, full_name, created_at, updated_at)
  values (
    new.id,
    lower(coalesce(new.email, '')),
    coalesce(
      nullif(new.raw_user_meta_data ->> 'full_name', ''),
      nullif(new.raw_user_meta_data ->> 'name', ''),
      ''
    ),
    (extract(epoch from now()) * 1000)::bigint,
    (extract(epoch from now()) * 1000)::bigint
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

do $$
begin
  execute 'drop trigger if exists campusone_on_auth_user_created on auth.users';
  execute 'create trigger campusone_on_auth_user_created after insert on auth.users for each row execute function public.campusone_handle_new_user()';
exception
  when insufficient_privilege then
    raise notice 'campusone: unable to install auth.users trigger (%)', sqlerrm;
end $$;

-- 2. Backfill every existing auth user on demand. Owner/admin only.
create or replace function public.campusone_admin_sync_profiles()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_inserted integer := 0;
begin
  if v_uid is null then
    return jsonb_build_object('ok', false, 'error', 'Not signed in');
  end if;
  if not public.campusone_has_permission('VIEW_USERS') then
    return jsonb_build_object('ok', false, 'error', 'Missing VIEW_USERS permission');
  end if;

  insert into public.profiles (id, email, full_name, created_at, updated_at)
  select
    u.id,
    lower(coalesce(u.email, '')),
    coalesce(
      nullif(u.raw_user_meta_data ->> 'full_name', ''),
      nullif(u.raw_user_meta_data ->> 'name', ''),
      ''
    ),
    (extract(epoch from now()) * 1000)::bigint,
    (extract(epoch from now()) * 1000)::bigint
  from auth.users u
  on conflict (id) do nothing;

  get diagnostics v_inserted = row_count;

  perform public.campusone_audit(
    'SYNC_REGISTERED_PROFILES',
    'profiles',
    null,
    jsonb_build_object('inserted', v_inserted)
  );

  return jsonb_build_object('ok', true, 'inserted', v_inserted);
end;
$$;

revoke all on function public.campusone_admin_sync_profiles() from public;
revoke all on function public.campusone_admin_sync_profiles() from anon;
grant execute on function public.campusone_admin_sync_profiles() to authenticated;

-- 3. Legacy roster stays readable for admins so the directory can fall back to
--    it while profiles converge. No-op when the table does not exist.
do $$
begin
  if to_regclass('public.students') is not null then
    execute 'alter table public.students enable row level security';
    if not exists (
      select 1 from pg_policies
      where schemaname = 'public'
        and tablename = 'students'
        and policyname = 'students_admin_read'
    ) then
      execute 'create policy students_admin_read on public.students for select to authenticated using (public.campusone_has_permission(''VIEW_USERS''))';
    end if;
  end if;
end $$;

-- =========================================================================
-- DEVICE TOKENS — FCM push notification device registration
-- =========================================================================
create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  fcm_token text not null,
  device_platform text not null default 'android',
  device_name text not null default '',
  email text not null default '',
  is_active boolean not null default true,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  constraint device_tokens_user_id_fcm_token_unique unique (user_id, fcm_token)
);

create index if not exists device_tokens_user_id_idx on public.device_tokens (user_id);
create index if not exists device_tokens_fcm_token_idx on public.device_tokens (fcm_token);
create index if not exists device_tokens_is_active_idx on public.device_tokens (is_active);

alter table public.device_tokens enable row level security;

drop policy if exists "device_tokens_own_select" on public.device_tokens;
drop policy if exists "device_tokens_own_insert" on public.device_tokens;
drop policy if exists "device_tokens_own_update" on public.device_tokens;
drop policy if exists "device_tokens_own_delete" on public.device_tokens;
drop policy if exists "device_tokens_admin_all" on public.device_tokens;

create policy "device_tokens_own_select" on public.device_tokens
  for select to authenticated
  using (auth.uid()::text = user_id::text);

create policy "device_tokens_own_insert" on public.device_tokens
  for insert to authenticated
  with check (auth.uid()::text = user_id::text);

create policy "device_tokens_own_update" on public.device_tokens
  for update to authenticated
  using (auth.uid()::text = user_id::text)
  with check (auth.uid()::text = user_id::text);

create policy "device_tokens_own_delete" on public.device_tokens
  for delete to authenticated
  using (auth.uid()::text = user_id::text);

create policy "device_tokens_admin_all" on public.device_tokens
  for all to authenticated
  using (public.campusone_has_permission('VIEW_USERS'))
  with check (public.campusone_has_permission('VIEW_USERS'));

grant select, insert, update, delete on public.device_tokens to authenticated;
revoke all on public.device_tokens from anon;

-- =========================================================================
-- FINAL EXECUTE PRIVILEGE HARDENING
-- Default-deny anonymous access to SECURITY DEFINER functions. Explicitly
-- re-grant only app RPCs and RLS helpers that require authenticated execution.
-- =========================================================================
do $$
declare r record;
begin
  for r in
    select p.oid::regprocedure as signature
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.prosecdef
  loop
    execute format('revoke execute on function %s from public, anon', r.signature);
  end loop;
end $$;

-- Trigger/internal-only functions are never exposed as PostgREST RPCs.
revoke execute on function public.campusone_enforce_role() from public, anon, authenticated;
revoke execute on function public.campusone_role_allowed(text, uuid) from public, anon, authenticated;
revoke execute on function public.campusone_audit(text, text, text, jsonb, text) from public, anon, authenticated;
revoke execute on function public.campusone_handle_new_user() from public, anon, authenticated;
revoke execute on function public.campusone_guard_study_resource() from public, anon, authenticated;
revoke execute on function public.campusone_guard_status_only_staff() from public, anon, authenticated;
revoke execute on function public.campusone_is_admin() from public, anon, authenticated;

-- Read-only caller helpers and the app's identity snapshot are safe for the
-- authenticated caller; none accepts a target user ID or caller-supplied role.
revoke execute on function public.campusone_caller_email() from public, anon;
revoke execute on function public.campusone_caller_role() from public, anon;
revoke execute on function public.campusone_is_owner() from public, anon;
revoke execute on function public.campusone_my_admin_state() from public, anon;
grant execute on function public.campusone_caller_email() to authenticated;
grant execute on function public.campusone_caller_role() to authenticated;
grant execute on function public.campusone_is_owner() to authenticated;
grant execute on function public.campusone_my_admin_state() to authenticated;

-- RLS policy helpers.
grant execute on function public.campusone_has_permission(text) to authenticated;
grant execute on function public.campusone_is_staff() to authenticated;
grant execute on function public.campusone_is_catalog_admin() to authenticated;
grant execute on function public.campusone_own_section_key() to authenticated;

-- App-facing, permission-checked RPCs.
grant execute on function public.campusone_bootstrap_profile(text, text) to authenticated;
grant execute on function public.owner_recover_admin() to authenticated;
grant execute on function public.campusone_admin_sync_profiles() to authenticated;
grant execute on function public.admin_set_role(uuid, text) to authenticated;
grant execute on function public.admin_set_suspended(uuid, boolean) to authenticated;
grant execute on function public.admin_delete_user(uuid) to authenticated;
grant execute on function public.admin_update_settings(jsonb) to authenticated;
grant execute on function public.admin_moderate_content(text, text, text) to authenticated;
grant execute on function public.admin_approve_role_request(uuid) to authenticated;
grant execute on function public.admin_reject_role_request(uuid) to authenticated;
grant execute on function public.admin_moderate_community(text, text, text) to authenticated;

-- RLS does not protect TRUNCATE, REFERENCES or TRIGGER table privileges.
revoke truncate, references, trigger on all tables in schema public from public, anon, authenticated;
revoke all privileges on all tables in schema public from anon;
revoke all on all sequences in schema public from anon;
revoke execute on all functions in schema public from anon;

alter default privileges in schema public revoke all on tables from anon;
alter default privileges in schema public revoke all on sequences from anon;
alter default privileges in schema public revoke execute on functions from anon;
alter default privileges in schema public revoke truncate, references, trigger on tables from authenticated;
