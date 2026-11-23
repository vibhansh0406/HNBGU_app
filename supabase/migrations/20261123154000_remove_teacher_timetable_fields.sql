-- Remove teacher/faculty data from subject and timetable records.
-- Keep the Room-only legacy property in Android as an empty value for local
-- database compatibility; it is never read, displayed, or sent.
do $$
declare
  v_def text;
begin
  v_def := pg_get_functiondef(
    'public.campusone_publish_timetable(text,text,integer,text,text,text,jsonb)'::regprocedure
  );
  v_def := regexp_replace(v_def, 'teacher_name,[[:space:]]*', '', 'g');
  v_def := regexp_replace(v_def, 'coalesce\(x\.faculty, ''''\),[[:space:]]*', '', 'g');
  v_def := regexp_replace(v_def, 'room text,[[:space:]]*faculty text', 'room text', 'g');
  execute v_def;
end;
$$;

alter table public.subjects drop column if exists teacher_name;
alter table public.timetable_slots drop column if exists teacher_name;
