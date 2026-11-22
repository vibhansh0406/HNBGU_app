-- =========================================================================
-- MISSION A — Add onboarding_completed to public.profiles
--
-- Adds the onboarding_completed flag so the server is the source of truth
-- for whether a user has finished student onboarding. Idempotent and safe
-- to re-run. Backfills existing rows to FALSE (they have not completed
-- the new onboarding flow).
-- =========================================================================

-- 1. Add the column with a safe default.
alter table public.profiles add column if not exists onboarding_completed boolean not null default false;

-- 2. Backfill any existing rows (should already be false from the default,
--    but make it explicit for clarity and safety).
update public.profiles
   set onboarding_completed = false
 where onboarding_completed is null;

-- 3. Ensure the auth.users trigger exists so new accounts get a profile
--    row automatically (idempotent, matches 20261121 migration).
do $$
begin
  execute 'drop trigger if exists campusone_on_auth_user_created on auth.users';
  execute 'create trigger campusone_on_auth_user_created after insert on auth.users for each row execute function public.campusone_handle_new_user()';
exception
  when insufficient_privilege then
    raise notice 'campusone: unable to install auth.users trigger (%)', sqlerrm;
end $$;
