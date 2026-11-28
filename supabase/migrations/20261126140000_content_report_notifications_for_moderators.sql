-- Content report notifications → notify moderators (and admin who holds MODERATE_REPORTS).
-- Role requests continue to go only to ADMIN/SUPER_ADMIN (see previous migration).

create or replace function public.campusone_content_report_notify()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
  insert into public.notifications (id, user_id, type, title, body, target_type, target_id, is_read, created_at)
  select 'content_report_' || new.id::text || '_' || p.id::text,
         p.id,
         'CONTENT_REPORT',
         'New content report',
         coalesce(nullif(new.reporter_email, ''), 'A user')
           || ' reported ' || new.target_type || ' #' || left(new.target_id, 16)
           || case when coalesce(new.reason, '') = '' then '' else ' • ' || left(new.reason, 160) end,
         'CONTENT_REPORT',
         new.id::text,
         false,
         (extract(epoch from now()) * 1000)::bigint
    from public.profiles p
   where exists (
         select 1
           from public.role_permissions rp
          where rp.role = p.role
            and rp.permission = 'MODERATE_REPORTS'
       )
  on conflict (id) do nothing;
  return null;
end;
$$;

revoke all on function public.campusone_content_report_notify() from public, anon, authenticated;

drop trigger if exists content_reports_notify on public.content_reports;
create trigger content_reports_notify
  after insert on public.content_reports
  for each row execute function public.campusone_content_report_notify();

notify pgrst, 'reload schema';
