-- CampusOne: restore the notification inbox and publish-time notice/assignment alerts.
-- The Android app reads this exact public.notifications contract.

alter table public.notices
  add column if not exists updated_at bigint not null default 0;

create table if not exists public.notifications (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null default 'SYSTEM',
  title text not null default '',
  body text not null default '',
  target_type text not null default '',
  target_id text not null default '',
  is_read boolean not null default false,
  created_at bigint not null default ((extract(epoch from now()) * 1000)::bigint)
);

create index if not exists notifications_user_idx
  on public.notifications (user_id, is_read, created_at desc);

alter table public.notifications enable row level security;

drop policy if exists notifications_read_own on public.notifications;
create policy notifications_read_own
  on public.notifications for select to authenticated
  using (user_id = (select auth.uid()));

drop policy if exists notifications_update_own on public.notifications;
create policy notifications_update_own
  on public.notifications for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

revoke all on table public.notifications from anon, authenticated;
grant select on table public.notifications to authenticated;
grant update (is_read) on table public.notifications to authenticated;
grant all on table public.notifications to service_role;

create or replace function public.campusone_notify(
  p_user uuid,
  p_type text,
  p_title text,
  p_body text,
  p_target_type text,
  p_target_id text,
  p_id text
)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
  if p_user is null then
    return;
  end if;

  insert into public.notifications (
    id, user_id, type, title, body, target_type, target_id, is_read, created_at
  )
  values (
    p_id, p_user, coalesce(p_type, 'SYSTEM'), coalesce(p_title, ''),
    coalesce(p_body, ''), coalesce(p_target_type, ''), coalesce(p_target_id, ''),
    false, (extract(epoch from now()) * 1000)::bigint
  )
  on conflict (id) do nothing;
end;
$$;

revoke all on function public.campusone_notify(uuid, text, text, text, text, text, text)
  from public, anon, authenticated;

create or replace function public.campusone_notice_notify()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
begin
  insert into public.notifications (
    id, user_id, type, title, body, target_type, target_id, is_read, created_at
  )
  select
    'notice_' || new.id::text || '_' || p.id::text,
    p.id,
    case when coalesce(new.is_official, false) then 'ANNOUNCEMENT' else 'NOTICE' end,
    coalesce(new.title, ''),
    left(coalesce(new.body, ''), 200),
    'NOTICE',
    new.id::text,
    false,
    v_now
  from public.profiles p
  where (
    coalesce(new.section_key, 'global') = 'global'
    or coalesce(new.section_key, '') =
      p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
  )
  and p.id <> coalesce(auth.uid(), '00000000-0000-0000-0000-000000000000'::uuid)
  on conflict (id) do nothing;

  return null;
end;
$$;

drop trigger if exists notices_notify on public.notices;
create trigger notices_notify
  after insert on public.notices
  for each row execute function public.campusone_notice_notify();

create or replace function public.campusone_assignment_notify()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
begin
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

drop trigger if exists assignments_notify on public.assignments;
create trigger assignments_notify
  after insert on public.assignments
  for each row execute function public.campusone_assignment_notify();

notify pgrst, 'reload schema';
