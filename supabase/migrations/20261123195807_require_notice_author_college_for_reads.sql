drop policy if exists notices_select_scoped on public.notices;
create policy notices_select_scoped on public.notices
for select to authenticated
using (
  exists (
    select 1
    from public.profiles p
    where p.id = auth.uid()
      and exists (
        select 1
        from public.profiles author
        where author.id = coalesce(
          case
            when notices.author_uid ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            then notices.author_uid::uuid
            else null
          end,
          notices.posted_by
        )
        and author.college_id = p.college_id
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
          and (coalesce(jsonb_array_length(notices.audience->'courses'),0)=0 or (notices.audience->'courses') ? p.course_id)
          and (coalesce(jsonb_array_length(notices.audience->'semesters'),0)=0 or (notices.audience->'semesters') ? p.semester::text)
          and (coalesce(jsonb_array_length(notices.audience->'years'),0)=0 or (notices.audience->'years') ? (((p.semester + 1)/2)::text))
          and (coalesce(jsonb_array_length(notices.audience->'sections'),0)=0 or (notices.audience->'sections') ? upper(p.section))
          and (coalesce(jsonb_array_length(notices.audience->'subjects'),0)=0 or exists (
            select 1 from public.subjects s
            where s.id in (select jsonb_array_elements_text(notices.audience->'subjects'))
              and s.section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
          ))
        )
      )
  )
);
