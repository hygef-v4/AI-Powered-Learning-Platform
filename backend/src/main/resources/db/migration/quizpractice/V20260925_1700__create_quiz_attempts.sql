-- U11: lượt làm Quiz Practice (domain-entities U11). Chạy sau quizzes (1600), course_classes (1100), accounts (0930).
-- Lượt không bao giờ bị xóa: kết quả luyện tập của người học giữ lại để xem (BR-U11-21).

create table quiz_attempts (
    id             uuid primary key,
    quiz_id        uuid not null,
    class_id       uuid not null,
    account_id     uuid not null,
    attempt_no     integer not null,
    snapshot       jsonb not null,
    answers        jsonb not null default '{}'::jsonb,
    answer_version bigint not null default 0,
    status         varchar(12) not null default 'IN_PROGRESS',
    submit_mode    varchar(16),
    started_at     timestamptz not null,
    deadline_at    timestamptz,
    last_saved_at  timestamptz,
    submitted_at   timestamptz,
    score          numeric(8, 2),
    max_score      numeric(8, 2),
    correct_count  integer,
    result         jsonb,
    constraint uq_quiz_attempts_no unique (quiz_id, account_id, attempt_no),
    constraint ck_quiz_attempts_no check (attempt_no >= 1),
    constraint ck_quiz_attempts_status check (status in ('IN_PROGRESS', 'SUBMITTED')),
    constraint ck_quiz_attempts_mode check (submit_mode is null or submit_mode in ('MANUAL', 'AUTO_TIME_LIMIT', 'AUTO_RETIRED')),
    constraint ck_quiz_attempts_submitted check (
        (status = 'IN_PROGRESS' and submitted_at is null and submit_mode is null and score is null)
        or (status = 'SUBMITTED' and submitted_at is not null and submit_mode is not null and score is not null
            and max_score is not null and correct_count is not null and result is not null)),
    constraint ck_quiz_attempts_deadline check (deadline_at is null or deadline_at > started_at),
    constraint ck_quiz_attempts_answer_version check (answer_version >= 0),
    constraint fk_quiz_attempts_quiz foreign key (quiz_id) references quizzes (id),
    constraint fk_quiz_attempts_class foreign key (class_id) references course_classes (id),
    constraint fk_quiz_attempts_account foreign key (account_id) references accounts (id)
);

-- Mỗi người học tối đa một lượt đang làm mỗi Quiz (BR-U11-10, NFR-U11-11).
create unique index ux_quiz_attempts_in_progress on quiz_attempts (quiz_id, account_id) where status = 'IN_PROGRESS';
-- Scanner tự nộp khi hết giờ (BR-U11-14).
create index ix_quiz_attempts_status_deadline on quiz_attempts (status, deadline_at);
create index ix_quiz_attempts_quiz_class on quiz_attempts (quiz_id, class_id);
create index ix_quiz_attempts_account_submitted on quiz_attempts (account_id, submitted_at);

grant select, insert, update on quiz_attempts to app;
revoke delete on quiz_attempts from app;
