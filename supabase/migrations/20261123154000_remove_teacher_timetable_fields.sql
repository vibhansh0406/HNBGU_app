-- Remove teacher/faculty data from subject and timetable records.
-- Keep the Room-only legacy property in the Android client as an empty value
-- for backwards-compatible local database upgrades; it is never read or sent.
alter table public.subjects drop column if exists teacher_name;
alter table public.timetable_slots drop column if exists teacher_name;

-- Update the timetable publishing RPC so new CSV uploads do not require or
-- write a teacher/faculty column.
do $$
declare
  v_def text;
begin
  v_def := pg_get_functiondef(
    'public.campusone_publish_timetable(text,text,integer,text,text,text,jsonb)'::regprocedure
  );
  v_def := regexp_replace(v_def, 'teacher_name,\\s*', '', 'g');
  v_def := regexp_replace(v_def, 'coalesce\\(x\\.faculty, ''''\\),\\s*', '', 'g');
  v_def := regexp_replace(v_def, 'room text,\\s*faculty text', 'room text', 'g');
  execute v_def;
end;
$$;

-- The app no longer uses legacy faculty names; clear any values before removal
-- in environments where the columns were not yet dropped by a previous deploy.
