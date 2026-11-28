-- Role request notifications + moderator self-request + all-role notice inbox.

-- 1. Students can request the MODERATOR role (still reviewed by the owner).
drop policy if exists "own_role_requests_insert" on public.role_requests;
create policy "own_role_requests_insert" on public.role_requests
  for insert to authenticated
  with check (
    auth.uid()::text = user_id::text
    and requested_role in ('CR', 'MODERATOR')
    and status = 'PENDING'
  );

-- 2. Notify admins/owner when a new role request arrives.
create or replace function public.campusone_role_request_notify()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
  insert into public.notifications (id, user_id, type, title, body, target_type, target_id, is_read, created_at)
  select 'role_request_' || new.id::text || '_' || p.id::text,
         p.id,
         'ROLE_REQUEST',
         case when new.requested_role = 'MODERATOR' then 'New moderator request' else 'New CR request' end,
         coalesce(nullif(new.email, ''), 'A student')
           || ' requested ' || new.requested_role
           || case when coalesce(new.note, '') = '' then '' else ' • ' || left(new.note, 160) end,
         'ROLE_REQUEST',
         new.id::text,
         false,
         (extract(epoch from now()) * 1000)::bigint
    from public.profiles p
   where p.role in ('ADMIN', 'SUPER_ADMIN')
  on conflict (id) do nothing;
  return null;
end;
$$;

revoke all on function public.campusone_role_request_notify() from public, anon, authenticated;

drop trigger if exists role_requests_notify on public.role_requests;
create trigger role_requests_notify
  after insert on public.role_requests
  for each row execute function public.campusone_role_request_notify();

-- 3. Tell the requester when the owner approves or declines the request.
create or replace function public.campusone_role_request_status_notify()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
  if new.status is distinct from old.status and new.status in ('APPROVED', 'REJECTED') then
    insert into public.notifications (id, user_id, type, title, body, target_type, target_id, is_read, created_at)
    values (
      'role_request_status_' || new.id::text || '_' || new.status,
      new.user_id,
      'ROLE_REQUEST',
      case when new.status = 'APPROVED' then 'Role request approved' else 'Role request declined' end,
      case when new.status = 'APPROVED'
           then 'You are now ' || new.requested_role || '. Open the app to see your new tools.'
           else 'Your ' || new.requested_role || ' request was reviewed by the admin.' end,
      'PROFILE',
      '',
      false,
      (extract(epoch from now()) * 1000)::bigint
    )
    on conflict (id) do nothing;
  end if;
  return null;
end;
$$;

revoke all on function public.campusone_role_request_status_notify() from public, anon, authenticated;

drop trigger if exists role_requests_status_notify on public.role_requests;
create trigger role_requests_status_notify
  after update on public.role_requests
  for each row execute function public.campusone_role_request_status_notify();

-- 4. Campus notices should reach every role, not only students.
do $$
declare
  v_def text;
begin
  v_def := pg_get_functiondef('public.campusone_notice_notify()'::regprocedure);
  if position('p.role = ''STUDENT'' and (' in v_def) > 0 then
    v_def := replace(v_def, 'p.role = ''STUDENT'' and (', '(');
    execute v_def;
  end if;
end;
$$;

notify pgrst, 'reload schema';
