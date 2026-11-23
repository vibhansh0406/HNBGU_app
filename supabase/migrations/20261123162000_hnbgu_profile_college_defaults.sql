-- The CampusOne deployment is dedicated to HNBGU. Older bootstrap paths
-- inserted profiles without college metadata, leaving college_id as ''.
-- Backfill existing users and make the correct college the server default.
update public.profiles
   set college_id = 'hnbgu',
       updated_at = (extract(epoch from now()) * 1000)::bigint
 where coalesce(btrim(college_id), '') = '';

update public.profiles
   set college_name = 'HNB Garhwal University (A Central University)',
       updated_at = (extract(epoch from now()) * 1000)::bigint
 where coalesce(btrim(college_name), '') = '';

alter table public.profiles
  alter column college_id set default 'hnbgu';

alter table public.profiles
  alter column college_name set default 'HNB Garhwal University (A Central University)';
