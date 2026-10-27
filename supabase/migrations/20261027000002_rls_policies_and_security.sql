-- ============================================================================
-- CampusOne (HNBGU Pilot) — Migration 002: Row Level Security & Admin RPCs
-- ============================================================================

-- 1. Non-Recursive SECURITY DEFINER Helper Functions
create or replace function public.current_user_role()
returns text
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select role
    from public.profiles
    where id = auth.uid()
      and account_status = 'active'
    limit 1;
$$;

create or replace function public.current_user_status()
returns text
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select account_status
    from public.profiles
    where id = auth.uid()
    limit 1;
$$;

create or replace function public.current_user_department_id()
returns uuid
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select department_id
    from public.profiles
    where id = auth.uid()
      and account_status = 'active'
    limit 1;
$$;

create or replace function public.is_active_user()
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select exists (
        select 1
        from public.profiles
        where id = auth.uid()
          and account_status = 'active'
    );
$$;

create or replace function public.is_super_admin()
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select coalesce(public.current_user_role() = 'super_admin', false);
$$;

create or replace function public.is_department_admin_for(target_dept uuid)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select coalesce(
        public.current_user_role() = 'super_admin'
        or (
            public.current_user_role() = 'department_admin'
            and target_dept is not null
            and public.current_user_department_id() = target_dept
        ),
        false
    );
$$;

create or replace function public.can_manage_notices_for(target_dept uuid)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select coalesce(
        public.current_user_role() = 'super_admin'
        or (
            public.current_user_role() in ('department_admin', 'notice_admin')
            and target_dept is not null
            and public.current_user_department_id() = target_dept
        ),
        false
    );
$$;

create or replace function public.can_manage_exams_for(target_dept uuid)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select coalesce(
        public.current_user_role() = 'super_admin'
        or (
            public.current_user_role() in ('department_admin', 'exam_admin')
            and target_dept is not null
            and public.current_user_department_id() = target_dept
        ),
        false
    );
$$;

-- 2. Column-Level Privilege Escalation Guard on public.profiles
-- Prevents any non-super-admin (or direct client update) from mutating role, department_id, or account_status.
create or replace function public.prevent_unauthorized_profile_escalation()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    if current_setting('campusone.allow_privileged_profile_mutation', true) = 'true' then
        return new;
    end if;

    if public.is_super_admin() then
        return new;
    end if;

    if new.role is distinct from old.role then
        raise exception 'Privilege escalation denied: users cannot modify their own role.';
    end if;

    if new.account_status is distinct from old.account_status then
        raise exception 'Access denied: users cannot modify their own account_status.';
    end if;

    if new.department_id is distinct from old.department_id and old.department_id is not null then
        raise exception 'Access denied: department assignment can only be changed by an administrator.';
    end if;

    return new;
end;
$$;

drop trigger if exists trg_guard_profile_escalation on public.profiles;
create trigger trg_guard_profile_escalation
before update on public.profiles
for each row execute function public.prevent_unauthorized_profile_escalation();

-- 3. Enable and Force Row Level Security on All 11 Tables
alter table public.departments enable row level security;
alter table public.departments force row level security;

alter table public.profiles enable row level security;
alter table public.profiles force row level security;

alter table public.student_enrollments enable row level security;
alter table public.student_enrollments force row level security;

alter table public.notices enable row level security;
alter table public.notices force row level security;

alter table public.timetable_entries enable row level security;
alter table public.timetable_entries force row level security;

alter table public.assignments enable row level security;
alter table public.assignments force row level security;

alter table public.exam_events enable row level security;
alter table public.exam_events force row level security;

alter table public.attendance_records enable row level security;
alter table public.attendance_records force row level security;

alter table public.device_tokens enable row level security;
alter table public.device_tokens force row level security;

alter table public.notification_campaigns enable row level security;
alter table public.notification_campaigns force row level security;

alter table public.audit_logs enable row level security;
alter table public.audit_logs force row level security;

-- 4. RLS Policies: DEPARTMENTS
drop policy if exists "departments_select_authenticated" on public.departments;
create policy "departments_select_authenticated"
on public.departments for select
to authenticated
using (is_active = true or public.is_super_admin());

drop policy if exists "departments_manage_super_admin" on public.departments;
create policy "departments_manage_super_admin"
on public.departments for all
to authenticated
using (public.is_super_admin())
with check (public.is_super_admin());

-- 5. RLS Policies: PROFILES
drop policy if exists "profiles_select_self_or_admin" on public.profiles;
create policy "profiles_select_self_or_admin"
on public.profiles for select
to authenticated
using (
    id = auth.uid()
    or public.is_super_admin()
    or (
        public.current_user_role() in ('department_admin', 'notice_admin', 'exam_admin')
        and department_id = public.current_user_department_id()
    )
);

drop policy if exists "profiles_update_self" on public.profiles;
create policy "profiles_update_self"
on public.profiles for update
to authenticated
using (id = auth.uid() and account_status = 'active')
with check (id = auth.uid() and account_status = 'active');

drop policy if exists "profiles_update_super_admin" on public.profiles;
create policy "profiles_update_super_admin"
on public.profiles for update
to authenticated
using (public.is_super_admin())
with check (public.is_super_admin());

-- 6. RLS Policies: STUDENT ENROLLMENTS
drop policy if exists "enrollments_select_self_or_admin" on public.student_enrollments;
create policy "enrollments_select_self_or_admin"
on public.student_enrollments for select
to authenticated
using (
    student_profile_id = auth.uid()
    or public.is_department_admin_for(department_id)
);

drop policy if exists "enrollments_insert_self_pending" on public.student_enrollments;
create policy "enrollments_insert_self_pending"
on public.student_enrollments for insert
to authenticated
with check (
    student_profile_id = auth.uid()
    and verification_status = 'pending'
    and verified_by is null
);

drop policy if exists "enrollments_manage_dept_or_super_admin" on public.student_enrollments;
create policy "enrollments_manage_dept_or_super_admin"
on public.student_enrollments for all
to authenticated
using (public.is_department_admin_for(department_id))
with check (public.is_department_admin_for(department_id));

-- 7. RLS Policies: NOTICES (Students only see published, non-expired notices)
drop policy if exists "notices_select_published_or_scoped_admin" on public.notices;
create policy "notices_select_published_or_scoped_admin"
on public.notices for select
to authenticated
using (
    (
        public.is_active_user()
        and status = 'published'
        and (expires_at is null or expires_at > now())
        and (
            department_id is null
            or department_id = public.current_user_department_id()
        )
    )
    or public.can_manage_notices_for(department_id)
    or public.is_super_admin()
);

drop policy if exists "notices_insert_scoped_admin" on public.notices;
create policy "notices_insert_scoped_admin"
on public.notices for insert
to authenticated
with check (
    public.is_super_admin()
    or public.can_manage_notices_for(department_id)
);

drop policy if exists "notices_update_scoped_admin" on public.notices;
create policy "notices_update_scoped_admin"
on public.notices for update
to authenticated
using (
    public.is_super_admin()
    or public.can_manage_notices_for(department_id)
)
with check (
    public.is_super_admin()
    or public.can_manage_notices_for(department_id)
);

drop policy if exists "notices_delete_scoped_admin" on public.notices;
create policy "notices_delete_scoped_admin"
on public.notices for delete
to authenticated
using (
    public.is_super_admin()
    or public.can_manage_notices_for(department_id)
);

-- 8. RLS Policies: TIMETABLE ENTRIES
drop policy if exists "timetable_select_published_or_admin" on public.timetable_entries;
create policy "timetable_select_published_or_admin"
on public.timetable_entries for select
to authenticated
using (
    (
        public.is_active_user()
        and is_published = true
        and (
            department_id = public.current_user_department_id()
            or public.current_user_department_id() is null
        )
    )
    or public.is_department_admin_for(department_id)
);

drop policy if exists "timetable_manage_dept_or_super_admin" on public.timetable_entries;
create policy "timetable_manage_dept_or_super_admin"
on public.timetable_entries for all
to authenticated
using (public.is_department_admin_for(department_id))
with check (public.is_department_admin_for(department_id));

-- 9. RLS Policies: ASSIGNMENTS
drop policy if exists "assignments_select_published_or_admin" on public.assignments;
create policy "assignments_select_published_or_admin"
on public.assignments for select
to authenticated
using (
    (
        public.is_active_user()
        and status = 'published'
        and (
            department_id = public.current_user_department_id()
            or public.current_user_department_id() is null
        )
    )
    or public.is_department_admin_for(department_id)
);

drop policy if exists "assignments_manage_dept_or_super_admin" on public.assignments;
create policy "assignments_manage_dept_or_super_admin"
on public.assignments for all
to authenticated
using (public.is_department_admin_for(department_id))
with check (public.is_department_admin_for(department_id));

-- 10. RLS Policies: EXAM EVENTS
drop policy if exists "exams_select_published_or_admin" on public.exam_events;
create policy "exams_select_published_or_admin"
on public.exam_events for select
to authenticated
using (
    (
        public.is_active_user()
        and status = 'published'
        and (
            department_id = public.current_user_department_id()
            or public.current_user_department_id() is null
        )
    )
    or public.can_manage_exams_for(department_id)
);

drop policy if exists "exams_manage_exam_or_dept_or_super_admin" on public.exam_events;
create policy "exams_manage_exam_or_dept_or_super_admin"
on public.exam_events for all
to authenticated
using (public.can_manage_exams_for(department_id))
with check (public.can_manage_exams_for(department_id));

-- 11. RLS Policies: ATTENDANCE RECORDS (Students can read ONLY their own verified records; cannot edit)
drop policy if exists "attendance_select_own_or_dept_admin" on public.attendance_records;
create policy "attendance_select_own_or_dept_admin"
on public.attendance_records for select
to authenticated
using (
    (
        student_profile_id = auth.uid()
        and public.is_active_user()
    )
    or public.is_department_admin_for(department_id)
);

drop policy if exists "attendance_manage_dept_or_super_admin" on public.attendance_records;
create policy "attendance_manage_dept_or_super_admin"
on public.attendance_records for all
to authenticated
using (public.is_department_admin_for(department_id))
with check (public.is_department_admin_for(department_id));

-- 12. RLS Policies: DEVICE TOKENS (Never publicly readable; users manage only their own token)
drop policy if exists "device_tokens_select_own" on public.device_tokens;
create policy "device_tokens_select_own"
on public.device_tokens for select
to authenticated
using (user_id = auth.uid());

drop policy if exists "device_tokens_insert_own" on public.device_tokens;
create policy "device_tokens_insert_own"
on public.device_tokens for insert
to authenticated
with check (user_id = auth.uid() and public.is_active_user());

drop policy if exists "device_tokens_update_own" on public.device_tokens;
create policy "device_tokens_update_own"
on public.device_tokens for update
to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());

drop policy if exists "device_tokens_delete_own" on public.device_tokens;
create policy "device_tokens_delete_own"
on public.device_tokens for delete
to authenticated
using (user_id = auth.uid());

-- 13. RLS Policies: NOTIFICATION CAMPAIGNS
drop policy if exists "campaigns_select_admin" on public.notification_campaigns;
create policy "campaigns_select_admin"
on public.notification_campaigns for select
to authenticated
using (
    public.is_super_admin()
    or public.can_manage_notices_for(target_department_id)
);

drop policy if exists "campaigns_manage_admin" on public.notification_campaigns;
create policy "campaigns_manage_admin"
on public.notification_campaigns for all
to authenticated
using (
    public.is_super_admin()
    or public.can_manage_notices_for(target_department_id)
)
with check (
    public.is_super_admin()
    or public.can_manage_notices_for(target_department_id)
);

-- 14. RLS Policies: AUDIT LOGS (Read-only for Super Admin; append-only via server functions; never editable/deletable)
drop policy if exists "audit_logs_select_super_admin" on public.audit_logs;
create policy "audit_logs_select_super_admin"
on public.audit_logs for select
to authenticated
using (public.is_super_admin());

drop policy if exists "audit_logs_insert_authenticated_admin" on public.audit_logs;
create policy "audit_logs_insert_authenticated_admin"
on public.audit_logs for insert
to authenticated
with check (
    actor_id = auth.uid()
    and public.current_user_role() in ('department_admin', 'notice_admin', 'exam_admin', 'super_admin')
);

-- 15. Secure Server-Side RPC: Super-Admin Role & Department Assignment
create or replace function public.admin_assign_user_role(
    target_user_id uuid,
    new_role text,
    new_department_id uuid default null,
    new_account_status text default 'active'
)
returns public.profiles
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_caller_role text;
    v_caller_email text;
    v_updated public.profiles;
begin
    select role, email into v_caller_role, v_caller_email
    from public.profiles
    where id = auth.uid() and account_status = 'active';

    if v_caller_role is distinct from 'super_admin' then
        raise exception 'Access denied: only an active super_admin can assign roles or account statuses.';
    end if;

    if new_role not in ('student', 'department_admin', 'notice_admin', 'exam_admin', 'super_admin') then
        raise exception 'Invalid role specified: %', new_role;
    end if;

    if new_account_status not in ('active', 'suspended', 'pending') then
        raise exception 'Invalid account_status specified: %', new_account_status;
    end if;

    perform set_config('campusone.allow_privileged_profile_mutation', 'true', true);

    update public.profiles
    set
        role = new_role,
        department_id = new_department_id,
        account_status = new_account_status,
        updated_at = now()
    where id = target_user_id
    returning * into v_updated;

    if v_updated.id is null then
        raise exception 'Target profile % not found.', target_user_id;
    end if;

    insert into public.audit_logs (
        actor_id,
        actor_email,
        actor_role,
        action,
        entity_type,
        entity_id,
        metadata
    )
    values (
        auth.uid(),
        v_caller_email,
        v_caller_role,
        'ASSIGN_ROLE_AND_STATUS',
        'profiles',
        target_user_id::text,
        jsonb_build_object(
            'new_role', new_role,
            'new_department_id', new_department_id,
            'new_account_status', new_account_status,
            'target_email', v_updated.email
        )
    );

    return v_updated;
end;
$$;

-- 16. Safe First-Super-Admin Bootstrap RPC
-- Can ONLY succeed if zero super_admins exist in the entire database AND caller email matches expected_email.
create or replace function public.bootstrap_first_super_admin(expected_email text)
returns public.profiles
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_existing_super_count int;
    v_caller public.profiles;
begin
    if auth.uid() is null then
        raise exception 'Must be authenticated to invoke bootstrap_first_super_admin.';
    end if;

    select count(*) into v_existing_super_count
    from public.profiles
    where role = 'super_admin';

    if v_existing_super_count > 0 then
        raise exception 'Bootstrap locked: a super_admin already exists. Use admin_assign_user_role as super_admin.';
    end if;

    select * into v_caller
    from public.profiles
    where id = auth.uid();

    if v_caller.id is null or lower(trim(v_caller.email)) <> lower(trim(expected_email)) then
        raise exception 'Bootstrap denied: authenticated account email does not match expected_email.';
    end if;

    perform set_config('campusone.allow_privileged_profile_mutation', 'true', true);

    update public.profiles
    set
        role = 'super_admin',
        account_status = 'active',
        updated_at = now()
    where id = auth.uid()
    returning * into v_caller;

    insert into public.audit_logs (
        actor_id,
        actor_email,
        actor_role,
        action,
        entity_type,
        entity_id,
        metadata
    )
    values (
        v_caller.id,
        v_caller.email,
        'super_admin',
        'BOOTSTRAP_FIRST_SUPER_ADMIN',
        'profiles',
        v_caller.id::text,
        jsonb_build_object('email', v_caller.email)
    );

    return v_caller;
end;
$$;

-- 17. Storage Bucket Setup & Policies for Attachments (campus-attachments)
insert into storage.buckets (id, name, public)
values ('campus-attachments', 'campus-attachments', false)
on conflict (id) do nothing;

drop policy if exists "campus_attachments_read_authenticated" on storage.objects;
create policy "campus_attachments_read_authenticated"
on storage.objects for select
to authenticated
using (bucket_id = 'campus-attachments' and public.is_active_user());

drop policy if exists "campus_attachments_write_admin" on storage.objects;
create policy "campus_attachments_write_admin"
on storage.objects for insert
to authenticated
with check (
    bucket_id = 'campus-attachments'
    and public.current_user_role() in ('department_admin', 'notice_admin', 'exam_admin', 'super_admin')
);

drop policy if exists "campus_attachments_delete_admin" on storage.objects;
create policy "campus_attachments_delete_admin"
on storage.objects for delete
to authenticated
using (
    bucket_id = 'campus-attachments'
    and public.current_user_role() in ('department_admin', 'notice_admin', 'exam_admin', 'super_admin')
);
