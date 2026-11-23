-- Make notice writes follow the role hierarchy.
-- Admin/Moderator manage college notices; CRs may only create and manage
-- their own non-official notices in their assigned section.

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.campusone_is_admin();
$$;

drop policy if exists notices_write on public.notices;
drop policy if exists notices_admin_manage on public.notices;
create policy notices_admin_manage on public.notices
  for all to authenticated
  using (public.campusone_is_admin())
  with check (public.campusone_is_admin());

drop policy if exists notices_cr_insert_section on public.notices;
create policy notices_cr_insert_section on public.notices
  for insert to authenticated
  with check (
    author_uid = auth.uid()::text
    and author_role = 'CR'
    and is_official = false
    and exists (
      select 1
        from public.profiles p
       where p.id = auth.uid()
         and p.role = 'CR'
         and p.verified_role = true
         and section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    )
  );

drop policy if exists notices_cr_update_own_section on public.notices;
create policy notices_cr_update_own_section on public.notices
  for update to authenticated
  using (
    author_uid = auth.uid()::text
    and author_role = 'CR'
    and is_official = false
    and exists (
      select 1
        from public.profiles p
       where p.id = auth.uid()
         and p.role = 'CR'
         and p.verified_role = true
         and section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    )
  )
  with check (
    author_uid = auth.uid()::text
    and author_role = 'CR'
    and is_official = false
    and exists (
      select 1
        from public.profiles p
       where p.id = auth.uid()
         and p.role = 'CR'
         and p.verified_role = true
         and section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    )
  );

drop policy if exists notices_cr_delete_own_section on public.notices;
create policy notices_cr_delete_own_section on public.notices
  for delete to authenticated
  using (
    author_uid = auth.uid()::text
    and author_role = 'CR'
    and is_official = false
    and exists (
      select 1
        from public.profiles p
       where p.id = auth.uid()
         and p.role = 'CR'
         and p.verified_role = true
         and section_key = p.college_id || '_' || p.course_id || '_sem' || p.semester || '_sec' || p.section
    )
  );
