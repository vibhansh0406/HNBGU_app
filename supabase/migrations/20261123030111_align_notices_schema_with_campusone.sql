-- Align the shared notice table with the Android NoticeEntity contract.
-- Keep UUID IDs because notice_reads references notices.id.
alter table public.notices alter column created_at drop default;
alter table public.notices
  alter column created_at type bigint
    using (extract(epoch from created_at) * 1000)::bigint;
alter table public.notices
  alter column created_at set default (extract(epoch from now()) * 1000)::bigint;

alter table public.notices
  add column if not exists section_key text not null default 'global',
  add column if not exists attachment_url text not null default '',
  add column if not exists attachment_type text not null default 'NONE',
  add column if not exists is_pinned boolean not null default false,
  add column if not exists priority text not null default 'NORMAL',
  add column if not exists is_official boolean not null default false,
  add column if not exists author_uid text not null default '',
  add column if not exists author_name text not null default '',
  add column if not exists author_role text not null default 'STUDENT',
  add column if not exists is_read boolean not null default false;

create index if not exists notices_section_created_idx
  on public.notices (section_key, created_at desc);
