-- ============================================================================
-- CampusOne — One-Time First Super Admin Bootstrap Script
-- ============================================================================
-- INSTRUCTIONS:
-- 1. First sign in once to CampusOne (Android app or /admin-portal) using your
--    personal Google account so that a row is created in `public.profiles`.
-- 2. Replace 'REPLACE_WITH_YOUR_GOOGLE_EMAIL@gmail.com' below with the exact
--    Google email address you signed in with.
-- 3. Run this script in the Supabase Dashboard -> SQL Editor.
-- ============================================================================

do $$
declare
    v_target_email constant text := 'REPLACE_WITH_YOUR_GOOGLE_EMAIL@gmail.com';
    v_super_count int;
    v_profile public.profiles;
begin
    if v_target_email = 'REPLACE_WITH_YOUR_GOOGLE_EMAIL@gmail.com' then
        raise exception 'Please replace REPLACE_WITH_YOUR_GOOGLE_EMAIL@gmail.com with your actual Google email before running.';
    end if;

    select count(*) into v_super_count
    from public.profiles
    where role = 'super_admin';

    if v_super_count > 0 then
        raise exception 'Aborting: at least one super_admin already exists in public.profiles.';
    end if;

    perform set_config('campusone.allow_privileged_profile_mutation', 'true', true);

    update public.profiles
    set
        role = 'super_admin',
        account_status = 'active',
        updated_at = now()
    where lower(email) = lower(trim(v_target_email))
    returning * into v_profile;

    if v_profile.id is null then
        raise exception 'No profile found for email %. Sign in once with Google first, then re-run.', v_target_email;
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
        v_profile.id,
        v_profile.email,
        'super_admin',
        'SQL_BOOTSTRAP_SUPER_ADMIN',
        'profiles',
        v_profile.id::text,
        jsonb_build_object('bootstrapped_email', v_profile.email)
    );
end;
$$;

-- Verification query:
select id, full_name, email, role, account_status, updated_at
from public.profiles
where role = 'super_admin';
