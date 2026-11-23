-- CampusOne's four user-facing roles:
-- Admin (the single owner/SUPER_ADMIN account) > Moderator > CR > Student.
-- Teacher and Club Admin are not application roles. No profile rows are deleted.

do $$
begin
  if exists (
    select 1 from public.profiles
    where role in ('TEACHER', 'CLUB_ADMIN')
  ) then
    raise exception 'Unsupported Teacher/Club Admin profile rows exist; review and convert them before applying the four-role migration';
  end if;
end;
$$;

-- Ensure timetable management is represented in the database permission catalogue.
insert into public.admin_permissions (code, description, is_owner_only)
values ('MANAGE_TIMETABLE', 'Create and update subject-based timetables', false)
on conflict (code) do nothing;

-- Remove legacy role grants and any stale Moderator grants before rebuilding
-- the Moderator's normal Admin Portal permission set.
delete from public.role_permissions
 where role in ('TEACHER', 'CLUB_ADMIN', 'ADMIN', 'MODERATOR');

insert into public.role_permissions (role, permission)
select 'MODERATOR', code
  from public.admin_permissions
 where not is_owner_only
   and code not in ('VIEW_USERS', 'MANAGE_USERS')
on conflict do nothing;

-- Keep the legacy ADMIN role limited to ordinary capabilities for compatibility.
-- The actual owner receives all permissions through campusone_is_owner().
insert into public.role_permissions (role, permission)
select 'ADMIN', code
  from public.admin_permissions
 where code in (
   'MANAGE_NOTICES', 'MANAGE_RESOURCES', 'MANAGE_ASSIGNMENTS',
   'MODERATE_REPORTS', 'MANAGE_EVENTS', 'MANAGE_TIMETABLE'
 )
on conflict do nothing;

create or replace function public.campusone_is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.campusone_is_owner()
     or public.campusone_caller_role() in ('ADMIN', 'MODERATOR');
$$;

-- Remove unsupported roles from the server-side role assignment allow-list.
do $$
declare
  v_def text;
begin
  v_def := pg_get_functiondef('public.admin_set_role(uuid,text)'::regprocedure);
  v_def := replace(
    v_def,
    'if p_role not in (''STUDENT'',''CR'',''TEACHER'',''CLUB_ADMIN'',''MODERATOR'',''ADMIN'') then',
    'if p_role not in (''STUDENT'',''CR'',''MODERATOR'') then'
  );
  if position('TEACHER' in v_def) > 0 or position('CLUB_ADMIN' in v_def) > 0 then
    raise exception 'Could not safely remove legacy roles from admin_set_role()';
  end if;
  v_def := regexp_replace(
    v_def,
    'update public\\.profiles set role = p_role,',
    'update public.profiles set role = p_role, verified_role = p_role in (''CR'',''MODERATOR'',''ADMIN''),',
    'g'
  );
  execute v_def;
end;
$$;

-- Keep the trusted role-verification flag aligned with Admin-approved roles.
-- Existing Moderator approval is preserved; this also activates the current
-- approved Moderator for the Firestore rules that consume verified_role.
update public.profiles
   set verified_role = true,
       updated_at = (extract(epoch from now()) * 1000)::bigint
 where role in ('CR', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN')
   and verified_role is distinct from true;
