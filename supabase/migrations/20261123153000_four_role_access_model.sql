-- CampusOne role hierarchy: Admin (owner/Super Admin) > Moderator > CR > Student.
-- Teacher and Club Admin are not supported application roles.
-- Existing Teacher/Club Admin permission grants are removed; no account data is deleted.

delete from public.role_permissions
 where role in ('TEACHER', 'CLUB_ADMIN', 'ADMIN');

-- Normal Admin Portal permissions for Moderators. Owner-only permissions
-- (MANAGE_ROLES, VIEW_AUDIT_LOGS, MANAGE_SYSTEM_SETTINGS) are excluded.
insert into public.role_permissions (role, permission)
select 'MODERATOR', code
  from public.admin_permissions
 where not is_owner_only
on conflict do nothing;

-- Keep the legacy ADMIN permission set for compatibility, but it is not
-- enough to enter the Super Admin Portal; that portal checks is_owner only.
insert into public.role_permissions (role, permission)
select 'ADMIN', code
  from public.admin_permissions
 where code in (
   'VIEW_USERS','MANAGE_USERS','MANAGE_NOTICES','MANAGE_RESOURCES',
   'MANAGE_ASSIGNMENTS','MODERATE_REPORTS','MANAGE_EVENTS','MANAGE_TIMETABLE'
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
     or public.campusone_caller_role() in ('ADMIN', 'MODERATOR', 'CR');
$$;

-- Remove obsolete roles from the server-side role assignment allow-list.
do $$
declare
  v_def text;
begin
  v_def := pg_get_functiondef('public.admin_set_role(uuid,text)'::regprocedure);
  v_def := replace(
    v_def,
    'if p_role not in (''STUDENT'',''CR'',''TEACHER'',''CLUB_ADMIN'',''MODERATOR'',''ADMIN'') then',
    'if p_role not in (''STUDENT'',''CR'',''MODERATOR'',''ADMIN'') then'
  );
  if v_def = pg_get_functiondef('public.admin_set_role(uuid,text)'::regprocedure) then
    raise exception 'Could not locate legacy role allow-list in admin_set_role';
  end if;
  execute v_def;
end;
$$;
