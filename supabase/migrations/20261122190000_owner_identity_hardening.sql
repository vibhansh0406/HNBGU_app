-- CampusOne owner identity hardening.
-- Owner identity is the fixed Supabase Auth UUID, never a caller-supplied email.
-- Applied to the live CampusOne project on 2026-11-22.
create or replace function public.campusone_is_owner()
returns boolean
language sql
stable
set search_path = ''
as $function$
  select auth.uid() = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid;
$function$;

create or replace function public.campusone_enforce_role()
returns trigger
language plpgsql
security definer
set search_path = 'public'
as $function$
begin
  new.is_suspended := coalesce(new.is_suspended, false);

  if new.id = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    new.role := 'SUPER_ADMIN';
    new.is_suspended := false;
    new.is_moderator_approved := true;
    return new;
  end if;

  if tg_op = 'INSERT' then
    new.role := 'STUDENT';
  elsif new.role is distinct from old.role then
    if not public.campusone_has_permission('MANAGE_ROLES')
       or not public.campusone_role_allowed(new.role, new.id) then
      new.role := old.role;
    end if;
  end if;

  if tg_op = 'UPDATE'
     and new.is_suspended is distinct from old.is_suspended
     and not public.campusone_has_permission('MANAGE_USERS') then
    new.is_suspended := old.is_suspended;
  end if;

  if new.is_suspended and new.role not in ('STUDENT','SUPER_ADMIN') then
    new.role := 'STUDENT';
  end if;
  return new;
end;
$function$;

create or replace function public.campusone_bootstrap_profile(
  p_name text default '',
  p_email text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_uid uuid := auth.uid();
  v_email text;
  v_role text;
begin
  if v_uid is null then
    return jsonb_build_object('ok', false, 'error', 'Not signed in');
  end if;

  select lower(coalesce(u.email, '')) into v_email
  from auth.users u where u.id = v_uid;

  if not found or v_email = '' then
    return jsonb_build_object('ok', false, 'error', 'Authenticated user email unavailable');
  end if;

  v_role := case
    when v_uid = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then 'SUPER_ADMIN'
    else 'STUDENT'
  end;

  insert into public.profiles (id,email,full_name,role,created_at,updated_at)
  values (
    v_uid,v_email,coalesce(p_name,''),v_role,
    (extract(epoch from now())*1000)::bigint,
    (extract(epoch from now())*1000)::bigint
  )
  on conflict (id) do update
    set full_name = coalesce(nullif(excluded.full_name,''),public.profiles.full_name),
        email = excluded.email,
        updated_at = (extract(epoch from now())*1000)::bigint;

  return jsonb_build_object(
    'ok',true,
    'role',(select p.role from public.profiles p where p.id=v_uid)
  );
end;
$function$;

create or replace function public.owner_recover_admin()
returns jsonb
language plpgsql
security definer
set search_path = 'public'
as $function$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null or v_uid <> 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    return jsonb_build_object('ok',false,'error','Owner identity required');
  end if;

  insert into public.profiles (
    id,email,role,is_suspended,is_moderator_approved,created_at,updated_at
  )
  select v_uid,lower(u.email),'SUPER_ADMIN',false,true,
    (extract(epoch from now())*1000)::bigint,
    (extract(epoch from now())*1000)::bigint
  from auth.users u where u.id=v_uid
  on conflict (id) do update
    set email=lower((select u.email from auth.users u where u.id=v_uid)),
        role='SUPER_ADMIN',is_suspended=false,is_moderator_approved=true,
        updated_at=(extract(epoch from now())*1000)::bigint;

  perform public.campusone_audit('owner_recover_admin','profile',v_uid::text,'{}'::jsonb);
  return jsonb_build_object('ok',true,'role','SUPER_ADMIN');
end;
$function$;

-- Remove the old first-user-wins promotion path from the legacy roster.
create or replace function public.promote_first_user()
returns trigger
language plpgsql
security definer
set search_path = 'public'
as $function$
begin
  if new.id = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    new.role := 'super_admin';
  else
    new.role := 'student';
  end if;
  return new;
end;
$function$;

create or replace function public.campusone_guard_student_role()
returns trigger
language plpgsql
security definer
set search_path = 'public'
as $function$
begin
  if new.id = 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    new.role := 'super_admin';
    return new;
  end if;
  if tg_op = 'INSERT' then
    new.role := 'student';
  elsif new.role is distinct from old.role
        and auth.uid() is not null
        and auth.uid() <> 'b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid then
    new.role := old.role;
  end if;
  return new;
end;
$function$;

do $block$
begin
  if to_regclass('public.students') is not null then
    execute 'drop trigger if exists campusone_guard_student_role on public.students';
    execute 'create trigger campusone_guard_student_role before insert or update of role on public.students for each row execute function public.campusone_guard_student_role()';
    execute 'update public.students set role = ''super_admin'' where id = ''b2b52ade-0fcc-4e1d-9c39-ca284789c960''::uuid';
  end if;
end;
$block$;

-- Protect owner actions by immutable UUID rather than profile.email.
do $block$
declare v_def text;
begin
  v_def := pg_get_functiondef('public.admin_delete_user(uuid)'::regprocedure);
  v_def := replace(v_def,
    'if lower(v_target.email) = ''vvibhansh@gmail.com'' then',
    'if v_target.id = ''b2b52ade-0fcc-4e1d-9c39-ca284789c960''::uuid then');
  execute v_def;

  v_def := pg_get_functiondef('public.admin_set_role(uuid,text)'::regprocedure);
  v_def := replace(v_def,
    'if lower(v_target.email) = ''vvibhansh@gmail.com'' then',
    'if v_target.id = ''b2b52ade-0fcc-4e1d-9c39-ca284789c960''::uuid then');
  execute v_def;

  v_def := pg_get_functiondef('public.admin_set_suspended(uuid,boolean)'::regprocedure);
  v_def := replace(v_def,
    'if lower(v_target.email) = ''vvibhansh@gmail.com'' then',
    'if v_target.id = ''b2b52ade-0fcc-4e1d-9c39-ca284789c960''::uuid then');
  execute v_def;
end;
$block$;

update public.profiles
set role='SUPER_ADMIN',is_suspended=false,is_moderator_approved=true,
    updated_at=(extract(epoch from now())*1000)::bigint
where id='b2b52ade-0fcc-4e1d-9c39-ca284789c960'::uuid;

revoke execute on function public.campusone_enforce_role() from public,anon,authenticated;
revoke execute on function public.promote_first_user() from public,anon,authenticated;
revoke execute on function public.campusone_guard_student_role() from public,anon,authenticated;
revoke execute on function public.campusone_role_allowed(text,uuid) from public,anon,authenticated;
revoke execute on function public.campusone_audit(text,text,text,jsonb,text) from public,anon,authenticated;
revoke execute on function public.campusone_bootstrap_profile(text,text) from public,anon;
grant execute on function public.campusone_bootstrap_profile(text,text) to authenticated;
revoke execute on function public.owner_recover_admin() from public,anon;
grant execute on function public.owner_recover_admin() to authenticated;
