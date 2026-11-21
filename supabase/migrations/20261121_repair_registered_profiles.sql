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
