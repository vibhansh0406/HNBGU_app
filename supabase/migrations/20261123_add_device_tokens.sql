-- CampusOne — Add device_tokens table for FCM push notifications
-- Run this in the Supabase SQL editor: Dashboard -> SQL Editor -> New query -> paste -> Run.
-- Safe to re-run (uses IF NOT EXISTS / DO blocks).

-- =========================================================================
-- DEVICE TOKENS — FCM push notification device registration
-- =========================================================================
create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  fcm_token text not null,
  device_platform text not null default 'android',
  device_name text not null default '',
  email text not null default '',
  is_active boolean not null default true,
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  constraint device_tokens_user_id_fcm_token_unique unique (user_id, fcm_token)
);

create index if not exists device_tokens_user_id_idx on public.device_tokens (user_id);
create index if not exists device_tokens_fcm_token_idx on public.device_tokens (fcm_token);
create index if not exists device_tokens_is_active_idx on public.device_tokens (is_active);

-- Enable RLS
alter table public.device_tokens enable row level security;

-- Drop existing policies if any (idempotent)
drop policy if exists "device_tokens_own_select" on public.device_tokens;
drop policy if exists "device_tokens_own_insert" on public.device_tokens;
drop policy if exists "device_tokens_own_update" on public.device_tokens;
drop policy if exists "device_tokens_own_delete" on public.device_tokens;
drop policy if exists "device_tokens_admin_all" on public.device_tokens;

-- Users can read their own device tokens
create policy "device_tokens_own_select" on public.device_tokens
  for select to authenticated
  using (auth.uid()::text = user_id::text);

-- Users can insert their own device tokens
create policy "device_tokens_own_insert" on public.device_tokens
  for insert to authenticated
  with check (auth.uid()::text = user_id::text);

-- Users can update their own device tokens
create policy "device_tokens_own_update" on public.device_tokens
  for update to authenticated
  using (auth.uid()::text = user_id::text)
  with check (auth.uid()::text = user_id::text);

-- Users can delete their own device tokens
create policy "device_tokens_own_delete" on public.device_tokens
  for delete to authenticated
  using (auth.uid()::text = user_id::text);

-- Admins can read all device tokens (for sending notifications)
create policy "device_tokens_admin_all" on public.device_tokens
  for all to authenticated
  using (public.campusone_has_permission('VIEW_USERS'))
  with check (public.campusone_has_permission('VIEW_USERS'));

-- Grants
grant select, insert, update, delete on public.device_tokens to authenticated;
revoke all on public.device_tokens from anon;
