-- Retaking a subject in a new class preserves the previous class's attempts.
alter table quiz_attempts drop constraint uq_quiz_attempts_no;
alter table quiz_attempts add constraint uq_quiz_attempts_no
    unique (quiz_id, account_id, class_id, attempt_no);
drop index ux_quiz_attempts_in_progress;
create unique index ux_quiz_attempts_in_progress
    on quiz_attempts (quiz_id, account_id, class_id) where status = 'IN_PROGRESS';

