-- Retirement must not manufacture publication history for a DRAFT/REVIEWED Quiz.
-- OPEN still requires publication; RETIRED may have never been published.
-- Equal legacy timestamps are ambiguous. Require positive successful publication
-- audit provenance before allowing a legacy row to reopen directly to OPEN.
alter table quizzes drop constraint ck_quizzes_published;
alter table quizzes add constraint ck_quizzes_published
    check (status <> 'OPEN' or published_at is not null);

update quizzes q set published_at = null
where q.status = 'RETIRED' and q.published_at = q.retired_at
  and not exists (
    select 1 from audit_logs a where a.action = 'QUIZ_PUBLISHED'
      and a.object_type = 'QUIZ' and a.object_id = q.id::text and a.result = 'SUCCESS'
  );
