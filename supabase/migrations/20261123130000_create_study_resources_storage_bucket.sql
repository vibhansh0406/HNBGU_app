-- CampusOne: enable authenticated file uploads for Study Resources and Notice attachments.
-- This is the existing shared bucket contract used by SupabaseFileStore.
insert into storage.buckets (id, name, public)
values ('study-resources', 'study-resources', true)
on conflict (id) do update set public = excluded.public;

drop policy if exists "campusone_storage_read" on storage.objects;
create policy "campusone_storage_read" on storage.objects
  for select using (bucket_id = 'study-resources');

drop policy if exists "campusone_storage_insert" on storage.objects;
create policy "campusone_storage_insert" on storage.objects
  for insert to authenticated with check (bucket_id = 'study-resources');

drop policy if exists "campusone_storage_update" on storage.objects;
create policy "campusone_storage_update" on storage.objects
  for update to authenticated using (bucket_id = 'study-resources')
  with check (bucket_id = 'study-resources');

drop policy if exists "campusone_storage_delete" on storage.objects;
create policy "campusone_storage_delete" on storage.objects
  for delete to authenticated using (bucket_id = 'study-resources');

notify pgrst, 'reload schema';
