-- ============================================================================
-- CampusOne (HNBGU Pilot) — Migration 001: Core Normalized Schema & Auth Trigger
-- Target: Supabase PostgreSQL 15+
-- ============================================================================

create extension if not exists "pgcrypto";

-- Generic updated_at timestamp trigger function
create or replace function public.set_updated_at_timestamp()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

-- 1. DEPARTMENTS
create table if not exists public.departments (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    code text not null unique check (char_length(trim(code)) between 2 and 20),
    campus_name text not null default 'Chauras Campus, HNBGU',
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_departments_code on public.departments(code);
create index if not exists idx_departments_active on public.departments(is_active);

drop trigger if exists trg_departments_updated_at on public.departments;
create trigger trg_departments_updated_at
before update on public.departments
for each row execute function public.set_updated_at_timestamp();

-- Seed HNBGU Pilot Departments
insert into public.departments (name, code, campus_name, is_active)
values
    ('Computer Science & Engineering', 'CSE', 'Chauras Campus, HNBGU', true),
    ('Information Technology', 'IT', 'Chauras Campus, HNBGU', true),
    ('Electronics & Communication Engineering', 'ECE', 'Chauras Campus, HNBGU', true),
    ('Mechanical Engineering', 'ME', 'Chauras Campus, HNBGU', true),
    ('Instrumentation Engineering', 'IE', 'Chauras Campus, HNBGU', true)
on conflict (code) do nothing;

-- 2. PROFILES (1-to-1 with auth.users)
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    full_name text not null default '',
    email text not null,
    avatar_url text,
    role text not null default 'student' check (
        role in ('student', 'department_admin', 'notice_admin', 'exam_admin', 'super_admin')
    ),
    department_id uuid references public.departments(id) on delete set null,
    account_status text not null default 'active' check (
        account_status in ('active', 'suspended', 'pending')
    ),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_profiles_email on public.profiles(lower(email));
create index if not exists idx_profiles_role_status on public.profiles(role, account_status);
create index if not exists idx_profiles_department on public.profiles(department_id);

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at
before update on public.profiles
for each row execute function public.set_updated_at_timestamp();

-- Secure Database Trigger on auth.users creation:
-- ALWAYS assigns role = 'student' and account_status = 'active'.
-- Ignores any client-provided role metadata to prevent privilege escalation.
create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_full_name text;
    v_avatar text;
    v_default_dept uuid;
begin
    v_full_name := coalesce(
        nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''),
        nullif(trim(new.raw_user_meta_data ->> 'name'), ''),
        split_part(coalesce(new.email, 'student@hnbgu.ac.in'), '@', 1)
    );
    v_avatar := nullif(trim(coalesce(
        new.raw_user_meta_data ->> 'avatar_url',
        new.raw_user_meta_data ->> 'picture'
    )), '');

    select id into v_default_dept
    from public.departments
    where code = 'CSE'
    limit 1;

    insert into public.profiles (
        id,
        full_name,
        email,
        avatar_url,
        role,
        department_id,
        account_status
    )
    values (
        new.id,
        v_full_name,
        coalesce(new.email, ''),
        v_avatar,
        'student',
        v_default_dept,
        'active'
    )
    on conflict (id) do update
    set
        email = excluded.email,
        full_name = case
            when public.profiles.full_name = '' then excluded.full_name
            else public.profiles.full_name
        end,
        avatar_url = coalesce(excluded.avatar_url, public.profiles.avatar_url),
        updated_at = now();

    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

-- 3. STUDENT ENROLLMENTS
create table if not exists public.student_enrollments (
    id uuid primary key default gen_random_uuid(),
    student_profile_id uuid not null unique references public.profiles(id) on delete cascade,
    student_identifier text not null unique check (char_length(trim(student_identifier)) >= 3),
    department_id uuid not null references public.departments(id) on delete restrict,
    semester int not null check (semester between 1 and 10),
    section text not null default 'A' check (char_length(trim(section)) between 1 and 10),
    verification_status text not null default 'pending' check (
        verification_status in ('pending', 'verified', 'rejected')
    ),
    verified_by uuid references public.profiles(id) on delete set null,
    verified_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_enrollments_dept_sem_sec
    on public.student_enrollments(department_id, semester, section);
create index if not exists idx_enrollments_status
    on public.student_enrollments(verification_status);

drop trigger if exists trg_student_enrollments_updated_at on public.student_enrollments;
create trigger trg_student_enrollments_updated_at
before update on public.student_enrollments
for each row execute function public.set_updated_at_timestamp();

-- 4. NOTICES
create table if not exists public.notices (
    id uuid primary key default gen_random_uuid(),
    title text not null check (char_length(trim(title)) >= 2),
    content text not null,
    department_id uuid references public.departments(id) on delete cascade,
    target_semesters int[] not null default '{}',
    target_sections text[] not null default '{}',
    priority text not null default 'MEDIUM' check (
        priority in ('LOW', 'MEDIUM', 'HIGH', 'URGENT')
    ),
    is_pinned boolean not null default false,
    attachment_path text,
    status text not null default 'draft' check (
        status in ('draft', 'published', 'archived')
    ),
    created_by uuid references public.profiles(id) on delete set null,
    published_at timestamptz,
    expires_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_notices_status_dept_published
    on public.notices(status, department_id, is_pinned desc, published_at desc);

drop trigger if exists trg_notices_updated_at on public.notices;
create trigger trg_notices_updated_at
before update on public.notices
for each row execute function public.set_updated_at_timestamp();

-- 5. TIMETABLE ENTRIES
create table if not exists public.timetable_entries (
    id uuid primary key default gen_random_uuid(),
    department_id uuid not null references public.departments(id) on delete cascade,
    semester int not null check (semester between 1 and 10),
    section text not null default 'A',
    subject_code text not null,
    subject_name text not null,
    faculty_name text not null,
    room text not null,
    weekday int not null check (weekday between 1 and 7),
    start_time text not null check (start_time ~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'),
    end_time text not null check (
        end_time ~ '^([01][0-9]|2[0-3]):[0-5][0-9]$' and end_time > start_time
    ),
    color_hex text not null default '#0F4C81',
    effective_from date not null default current_date,
    effective_until date,
    is_published boolean not null default true,
    created_by uuid references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_timetable_dept_sem_sec_day
    on public.timetable_entries(department_id, semester, section, weekday, start_time);

drop trigger if exists trg_timetable_entries_updated_at on public.timetable_entries;
create trigger trg_timetable_entries_updated_at
before update on public.timetable_entries
for each row execute function public.set_updated_at_timestamp();

-- 6. ASSIGNMENTS
create table if not exists public.assignments (
    id uuid primary key default gen_random_uuid(),
    title text not null check (char_length(trim(title)) >= 2),
    description text not null default '',
    department_id uuid not null references public.departments(id) on delete cascade,
    semester int not null check (semester between 1 and 10),
    section text not null default 'A',
    subject_code text not null default '',
    subject_name text not null,
    due_at timestamptz not null,
    priority text not null default 'MEDIUM' check (priority in ('LOW', 'MEDIUM', 'HIGH')),
    attachment_path text,
    status text not null default 'draft' check (
        status in ('draft', 'published', 'archived')
    ),
    created_by uuid references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_assignments_dept_sem_status_due
    on public.assignments(department_id, semester, status, due_at);

drop trigger if exists trg_assignments_updated_at on public.assignments;
create trigger trg_assignments_updated_at
before update on public.assignments
for each row execute function public.set_updated_at_timestamp();

-- 7. EXAM EVENTS
create table if not exists public.exam_events (
    id uuid primary key default gen_random_uuid(),
    exam_title text not null,
    exam_type text not null default 'END_SEM' check (
        exam_type in ('MID_SEM', 'END_SEM', 'PRACTICAL', 'BACK_PAPER', 'QUIZ', 'OTHER')
    ),
    department_id uuid not null references public.departments(id) on delete cascade,
    semester int not null check (semester between 1 and 10),
    subject_code text not null,
    subject_name text not null,
    exam_date date not null,
    start_time text not null,
    end_time text not null,
    venue text not null,
    instructions text not null default '',
    status text not null default 'draft' check (
        status in ('draft', 'published', 'archived')
    ),
    created_by uuid references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_exam_events_dept_sem_status_date
    on public.exam_events(department_id, semester, status, exam_date);

drop trigger if exists trg_exam_events_updated_at on public.exam_events;
create trigger trg_exam_events_updated_at
before update on public.exam_events
for each row execute function public.set_updated_at_timestamp();

-- 8. ATTENDANCE RECORDS (Official University/Faculty Attendance Records)
create table if not exists public.attendance_records (
    id uuid primary key default gen_random_uuid(),
    student_profile_id uuid not null references public.profiles(id) on delete cascade,
    department_id uuid not null references public.departments(id) on delete cascade,
    semester int not null check (semester between 1 and 10),
    subject_code text not null,
    subject_name text not null,
    attendance_date date not null,
    status text not null check (status in ('PRESENT', 'ABSENT', 'CANCELLED')),
    recorded_by uuid references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (student_profile_id, subject_code, attendance_date)
);

create index if not exists idx_attendance_student_date
    on public.attendance_records(student_profile_id, attendance_date desc);
create index if not exists idx_attendance_dept_sem_date
    on public.attendance_records(department_id, semester, attendance_date desc);

drop trigger if exists trg_attendance_records_updated_at on public.attendance_records;
create trigger trg_attendance_records_updated_at
before update on public.attendance_records
for each row execute function public.set_updated_at_timestamp();

-- 9. DEVICE TOKENS (Private FCM Push Tokens)
create table if not exists public.device_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    fcm_token text not null unique,
    platform text not null default 'android' check (platform in ('android', 'web', 'ios')),
    department_id uuid references public.departments(id) on delete set null,
    semester int check (semester between 1 and 10),
    is_active boolean not null default true,
    last_seen_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_device_tokens_user on public.device_tokens(user_id);
create index if not exists idx_device_tokens_audience on public.device_tokens(is_active, department_id, semester);

drop trigger if exists trg_device_tokens_updated_at on public.device_tokens;
create trigger trg_device_tokens_updated_at
before update on public.device_tokens
for each row execute function public.set_updated_at_timestamp();

-- 10. NOTIFICATION CAMPAIGNS
create table if not exists public.notification_campaigns (
    id uuid primary key default gen_random_uuid(),
    title text not null check (char_length(trim(title)) >= 2),
    body text not null check (char_length(trim(body)) >= 2),
    target_department_id uuid references public.departments(id) on delete set null,
    target_semester int check (target_semester between 1 and 10),
    target_section text,
    related_entity_type text,
    related_entity_id uuid,
    scheduled_for timestamptz,
    delivery_status text not null default 'draft' check (
        delivery_status in ('draft', 'scheduled', 'sending', 'sent', 'failed')
    ),
    total_targeted int not null default 0,
    total_delivered int not null default 0,
    last_error text,
    sent_at timestamptz,
    created_by uuid references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_campaigns_status_created
    on public.notification_campaigns(delivery_status, created_at desc);

drop trigger if exists trg_notification_campaigns_updated_at on public.notification_campaigns;
create trigger trg_notification_campaigns_updated_at
before update on public.notification_campaigns
for each row execute function public.set_updated_at_timestamp();

-- 11. AUDIT LOGS (Immutable Administrative Audit Trail)
create table if not exists public.audit_logs (
    id uuid primary key default gen_random_uuid(),
    actor_id uuid references public.profiles(id) on delete set null,
    actor_email text,
    actor_role text,
    action text not null,
    entity_type text not null,
    entity_id text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists idx_audit_logs_created_at on public.audit_logs(created_at desc);
create index if not exists idx_audit_logs_actor on public.audit_logs(actor_id, created_at desc);
create index if not exists idx_audit_logs_entity on public.audit_logs(entity_type, entity_id);
