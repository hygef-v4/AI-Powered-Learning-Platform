-- Attempts stranded by a class that is no longer OPEN or a student who left the class are auto-submitted (BR-U11-14).
alter table quiz_attempts drop constraint ck_quiz_attempts_mode;
alter table quiz_attempts add constraint ck_quiz_attempts_mode
    check (submit_mode is null or submit_mode in ('MANUAL', 'AUTO_TIME_LIMIT', 'AUTO_RETIRED', 'AUTO_ACCESS_LOST'));
