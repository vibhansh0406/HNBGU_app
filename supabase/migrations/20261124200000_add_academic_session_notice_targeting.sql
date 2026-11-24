alter table public.profiles
  add column if not exists academic_session text;

alter table public.profiles
  drop constraint if exists profiles_academic_session_format_check;

alter table public.profiles
  add constraint profiles_academic_session_format_check
  check (
    academic_session is null
    or academic_session = ''
    or academic_session ~ '^[0-9]{4}-[0-9]{2}$'
  );

drop policy if exists notices_select_scoped on public.notices;

create policy notices_select_scoped on public.notices
for select to authenticated
using (
  exists (
    select 1 from public.profiles p
    where p.id = auth.uid()
      and p.college_id = coalesce(
        (select author.college_id from public.profiles author
         where author.id = case
           when notices.author_uid ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
           then notices.author_uid::uuid else null end),
        p.college_id
      )
      and (
        p.role in ('ADMIN', 'SUPER_ADMIN', 'MODERATOR')
        or (p.role = 'CR' and (
          notices.section_key = 'global'
          or notices.section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
        ))
        or (p.role = 'STUDENT'
          and (notices.section_key = 'global' or notices.section_key =
            p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section)
          and (coalesce(jsonb_array_length(notices.audience->'courses'), 0) = 0 or (notices.audience->'courses') ? p.course_id)
          and (coalesce(jsonb_array_length(notices.audience->'semesters'), 0) = 0 or (notices.audience->'semesters') ? p.semester::text)
          and (coalesce(jsonb_array_length(notices.audience->'years'), 0) = 0 or (notices.audience->'years') ? (((p.semester + 1) / 2)::text))
          and (coalesce(jsonb_array_length(notices.audience->'sections'), 0) = 0 or (notices.audience->'sections') ? upper(p.section))
          and (coalesce(jsonb_array_length(notices.audience->'sessions'), 0) = 0
            or (nullif(p.academic_session, '') is not null and (notices.audience->'sessions') ? p.academic_session))
        )
      )
  )
);

create or replace function public.campusone_notice_notify()
returns trigger language plpgsql security definer
set search_path = pg_catalog, public
as $function$
declare
  v_now bigint := (extract(epoch from now()) * 1000)::bigint;
  v_college_id text;
begin
  select p.college_id into v_college_id
  from public.profiles p
  where p.id = case
    when new.author_uid ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    then new.author_uid::uuid else null end;

  if v_college_id is null then
    raise exception 'Notice author must have a valid college profile';
  end if;

  insert into public.notifications (id, user_id, type, title, body, target_type, target_id, is_read, created_at)
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
  where p.role = 'STUDENT'
    and p.college_id = v_college_id
    and p.id <> coalesce(
      case when new.author_uid ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        then new.author_uid::uuid else null end,
      '00000000-0000-0000-0000-000000000000'::uuid
    )
    and (coalesce(new.section_key, 'global') = 'global'
      or coalesce(new.section_key, '') = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section)
    and (coalesce(jsonb_array_length(new.audience->'courses'), 0) = 0 or (new.audience->'courses') ? p.course_id)
    and (coalesce(jsonb_array_length(new.audience->'semesters'), 0) = 0 or (new.audience->'semesters') ? p.semester::text)
    and (coalesce(jsonb_array_length(new.audience->'years'), 0) = 0 or (new.audience->'years') ? (((p.semester + 1) / 2)::text))
    and (coalesce(jsonb_array_length(new.audience->'sections'), 0) = 0 or (new.audience->'sections') ? upper(p.section))
    and (coalesce(jsonb_array_length(new.audience->'sessions'), 0) = 0
      or (nullif(p.academic_session, '') is not null and (new.audience->'sessions') ? p.academic_session))
  on conflict (id) do nothing;

  return null;
end;
$function$;