-- CampusOne — Supabase schema
-- Run this in the Supabase SQL editor: Dashboard -> SQL Editor -> New query -> paste -> Run.
-- Safe to re-run (uses IF NOT EXISTS / DO blocks).

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
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

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

create table if not exists public.study_resources (
  id text primary key,
  section_key text not null default '',
  title text not null default '',
  subject_tag text not null default '',
  category text not null default 'NOTES',
  semester text not null default '',
  file_name text not null default '',
  file_size_mb real not null default 0,
  uploader_uid text not null default '',
  uploader_name text not null default '',
  upvotes integer not null default 0,
  downloads integer not null default 0,
  is_bookmarked boolean not null default false,
  is_hidden boolean not null default false,
  report_count integer not null default 0,
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
-- ROW LEVEL SECURITY
-- =========================================================================
do $$
declare t text;
begin
  foreach t in array array[
    'profiles','subjects','timetable_slots','notices','attendance_logs',
    'assignments','exams','study_resources','syllabus_topics','gpa_semesters',
    'clubs','campus_events','lost_found_items','lost_found_contact_requests',
    'directory_contacts','directory_suggestions','campus_map_places','moderators'
  ]
  loop
    execute format('alter table public.%I enable row level security', t);

    -- Public content is readable by anyone (anon key + JWT).
    execute format('drop policy if exists "campusone_read_%1$I" on public.%1$I', t);
    execute format('create policy "campusone_read_%1$I" on public.%1$I for select using (true)', t);

    -- Authenticated users can create/update/delete app-managed content.
    execute format('drop policy if exists "campusone_write_%1$I" on public.%1$I', t);
    execute format(
      'create policy "campusone_write_%1$I" on public.%1$I for all to authenticated using (true) with check (true)',
      t
    );
  end loop;
end $$;

-- Per-user data is additionally restricted to the owner.
drop policy if exists "own_attendance" on public.attendance_logs;
create policy "own_attendance" on public.attendance_logs
  for all to authenticated
  using (auth.uid()::text = user_id)
  with check (auth.uid()::text = user_id);

drop policy if exists "own_profile" on public.profiles;
create policy "own_profile" on public.profiles
  for all to authenticated
  using (auth.uid()::text = id::text)
  with check (auth.uid()::text = id::text);

drop policy if exists "own_gpa" on public.gpa_semesters;
create policy "own_gpa" on public.gpa_semesters
  for all to authenticated
  using (auth.uid()::text = user_id)
  with check (auth.uid()::text = user_id);