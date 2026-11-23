-- Restrict notification trigger functions to trigger execution only.
-- Triggers continue to work; direct RPC execution is not part of the app API.

create or replace function public.campusone_assignment_notify()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
begin
  -- Personal assignments must not broadcast to the whole section.
  if not coalesce(new.is_shared_by_cr_or_teacher, false) then
    return null;
  end if;

  insert into public.notifications (
    id, user_id, type, title, body, target_type, target_id, is_read, created_at
  )
  select
    'assignment_' || new.id::text || '_' || p.id::text,
    p.id,
    'ASSIGNMENT',
    'New assignment: ' || coalesce(new.title, ''),
    coalesce(nullif(new.subject_tag, ''), new.subject_id, '')
      || ' • due ' || to_char(to_timestamp(new.due_date_epoch_millis / 1000.0), 'DD Mon, HH24:MI'),
    'ASSIGNMENT',
    new.id::text,
    false,
    v_now
  from public.profiles p
  where coalesce(new.section_key, '') =
      p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    and p.id <> coalesce(auth.uid(), '00000000-0000-0000-0000-000000000000'::uuid)
  on conflict (id) do nothing;

  return null;
end;
$$;

revoke all on function public.campusone_notice_notify() from public, anon, authenticated;
revoke all on function public.campusone_assignment_notify() from public, anon, authenticated;
