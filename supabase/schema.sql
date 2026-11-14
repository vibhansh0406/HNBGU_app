-- CampusOne — Supabase schema
-- Run this in the Supabase SQL editor: Dashboard -> SQL Editor -> New query -> paste -> Run.
-- Safe to re-run (uses IF NOT EXISTS / DO blocks).
--
-- SECURITY MODEL (read before changing anything below)
-- ---------------------------------------------------
-- * The owner is `vvibhansh@gmail.com`. Owner identity is derived from the
--   Supabase JWT (`auth.jwt() -> 'email'`), never from a client value.
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
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

alter table public.profiles add column if not exists is_suspended boolean not null default false;

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
  'MANAGE_ASSIGNMENTS','MODERATE_REPORTS','MANAGE_EVENTS'
)
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
  teacher_name text not null default '',
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
  teacher_name text not null default '',
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
security definer
set search_path = public
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
security definer
set search_path = public
as $$
  select lower(public.campusone_caller_email()) = 'vvibhansh@gmail.com';
$$;

-- Current role of the caller, read from the database (never from the client).
create or replace function public.campusone_caller_role()
returns text
language sql
stable
security definer
set search_path = public
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
as $$
  select public.campusone_is_owner()
     or public.campusone_caller_role() in ('ADMIN', 'TEACHER', 'CR');
$$;

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
  if lower(coalesce(new.email, '')) = 'vvibhansh@gmail.com' then
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
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_role text;
  v_email text;
begin
  if v_uid is null then
    return jsonb_build_object('ok', false, 'error', 'Not signed in');
  end if;

  v_email := lower(coalesce(nullif(p_email, ''), public.campusone_caller_email()));
  v_role := case when v_email = 'vvibhansh@gmail.com' then 'SUPER_ADMIN' else 'STUDENT' end;

  insert into public.profiles (id, email, full_name, role, created_at, updated_at)
  values (v_uid, v_email, coalesce(p_name, ''), v_role,
          (extract(epoch from now()) * 1000)::bigint,
          (extract(epoch from now()) * 1000)::bigint)
  on conflict (id) do update
    set full_name = coalesce(nullif(excluded.full_name, ''), public.profiles.full_name),
        email = case
          when lower(public.profiles.email) = 'vvibhansh@gmail.com' then public.profiles.email
          else excluded.email
        end,
        updated_at = (extract(epoch from now()) * 1000)::bigint;

  return jsonb_build_object('ok', true, 'role', (select role from public.profiles where id = v_uid));
end;
$$;

-- Session info for the signed-in caller. The client trusts this, not its cache.
create or replace function public.campusone_my_admin_state()
returns jsonb
language plpgsql
security definer
set search_path = public
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

  if p_role not in ('STUDENT','CR','TEACHER','CLUB_ADMIN','MODERATOR','ADMIN') then
    return jsonb_build_object('ok', false, 'error', 'Unknown role');
  end if;

  select * into v_target from public.profiles where id = p_uid;
  if v_target.id is null then
    return jsonb_build_object('ok', false, 'error', 'User not found');
  end if;

  if lower(v_target.email) = 'vvibhansh@gmail.com' then
    return jsonb_build_object('ok', false, 'error', 'Owner role is protected');
  end if;

  update public.profiles
     set role = p_role,
         is_moderator_approved = case when p_role in ('MODERATOR','ADMIN') then true else is_moderator_approved end,
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
  if lower(v_target.email) = 'vvibhansh@gmail.com' then
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
  if lower(v_target.email) = 'vvibhansh@gmail.com' then
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
  if v_uid is null or lower(v_email) <> 'vvibhansh@gmail.com' then
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
  for select using (true);
create policy "campusone_read_syllabus_topics" on public.syllabus_topics
  for select using (true);
create policy "campusone_read_clubs" on public.clubs
  for select using (true);
create policy "campusone_read_campus_events" on public.campus_events
  for select using (true);
create policy "campusone_read_lost_found_items" on public.lost_found_items
  for select using (true);
create policy "campusone_read_directory_contacts" on public.directory_contacts
  for select using (true);
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
    and requested_role in ('CR','TEACHER','MODERATOR','CLUB_ADMIN')
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