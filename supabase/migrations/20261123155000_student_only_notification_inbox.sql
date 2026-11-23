-- Ensure notice and assignment Notification Centre entries go to students only.
-- This updates the already-installed trigger functions without touching existing rows.
do $$
declare
  v_def text;
begin
  v_def := pg_get_functiondef('public.campusone_notice_notify()'::regprocedure);
  if position('p.role = ''STUDENT''' in v_def) = 0 then
    v_def := replace(
      v_def,
      'where (',
      'where p.role = ''STUDENT'' and ('
    );
    execute v_def;
  end if;

  v_def := pg_get_functiondef('public.campusone_assignment_notify()'::regprocedure);
  if position('p.role = ''STUDENT''' in v_def) = 0 then
    v_def := replace(
      v_def,
      'where coalesce(new.section_key, '''') =',
      'where p.role = ''STUDENT'' and coalesce(new.section_key, '''') ='
    );
    execute v_def;
  end if;
end;
$$;
