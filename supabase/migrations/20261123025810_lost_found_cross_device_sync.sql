-- Keep Lost & Found posts in Supabase so every signed-in student sees the same feed.
alter table public.lost_found_items
  add column if not exists category text not null default 'OTHER';

drop policy if exists campusone_insert_lost_found_items on public.lost_found_items;
create policy campusone_insert_lost_found_items
on public.lost_found_items
for insert to authenticated
with check (
  poster_uid = (select auth.uid())::text
  and coalesce(is_hidden, false) = false
  and coalesce(is_resolved, false) = false
);

drop policy if exists campusone_update_own_lost_found_items on public.lost_found_items;
create policy campusone_update_own_lost_found_items
on public.lost_found_items
for update to authenticated
using (
  poster_uid = (select auth.uid())::text
  or public.campusone_has_permission('MODERATE_REPORTS')
)
with check (
  poster_uid = (select auth.uid())::text
  or public.campusone_has_permission('MODERATE_REPORTS')
);

grant select, insert, update on public.lost_found_items to authenticated;
revoke all on public.lost_found_items from anon;
