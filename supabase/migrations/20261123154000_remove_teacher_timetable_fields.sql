-- The live schema has no timetable publishing RPC. Android now sends only
-- subject, room, day and time fields, so remove the unused teacher-name columns.
-- Dropping these columns removes the legacy values permanently.
set local lock_timeout = '5s';
alter table public.subjects drop column if exists teacher_name;
alter table public.timetable_slots drop column if exists teacher_name;
