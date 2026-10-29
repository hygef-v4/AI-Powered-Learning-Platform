-- U08: lượt làm bài cá nhân, phản hồi Practice, sự kiện hoạt động (domain-entities U08 mục 2, 3).
-- Chạy sau assignments (1300) vì có FK sang bảng đó. content_version bắt đầu 0, tăng mỗi lần lưu.
create table assignment_attempts (
    id              uuid primary key,
    assignment_id   uuid not null references assignments(id),
    class_id        uuid not null references course_classes(id),
    account_id      uuid not null references accounts(id),
    attempt_no      integer not null,
    snapshot        jsonb not null,
    content         jsonb not null,
    content_version bigint not null default 0,
    status          varchar(12) not null default 'IN_PROGRESS',
    submit_mode     varchar(14),
    late            boolean not null default false,
    started_at      timestamptz not null,
    deadline_at     timestamptz not null,
    last_saved_at   timestamptz,
    submitted_at    timestamptz,
    receipt_hash    varchar(64),
    submit_warnings jsonb not null default '[]'::jsonb,
    constraint uq_assignment_attempts_no unique (assignment_id, account_id, attempt_no),
    constraint ck_assignment_attempts_no check (attempt_no between 1 and 10),
    constraint ck_assignment_attempts_status check (status in ('IN_PROGRESS', 'SUBMITTED')),
    constraint ck_assignment_attempts_mode check (submit_mode is null
        or submit_mode in ('MANUAL', 'AUTO_DEADLINE', 'AUTO_RETIRED')),
    constraint ck_assignment_attempts_submitted check (status = 'IN_PROGRESS'
        or (submitted_at is not null and submit_mode is not null and receipt_hash is not null))
);

-- BR-U08-11: mỗi người tối đa một lượt đang làm mỗi bài.
create unique index uq_assignment_attempts_in_progress on assignment_attempts (assignment_id, account_id)
    where status = 'IN_PROGRESS';
create index ix_assignment_attempts_deadline on assignment_attempts (status, deadline_at);
create index ix_assignment_attempts_class on assignment_attempts (assignment_id, class_id, status);
create index ix_assignment_attempts_account on assignment_attempts (account_id, assignment_id);

create table practice_feedback (
    id              uuid primary key,
    attempt_id      uuid not null references assignment_attempts(id),
    account_id      uuid not null references accounts(id),
    origin          varchar(10) not null,
    task_id         uuid,
    request_ref     varchar(100),
    status          varchar(10) not null,
    submission_hash varchar(64) not null,
    feedback        jsonb,
    provenance      jsonb not null default '{}'::jsonb,
    safe_error      varchar(500),
    created_at      timestamptz not null,
    completed_at    timestamptz,
    constraint ck_practice_feedback_origin check (origin in ('AI_TASK', 'MCP')),
    constraint ck_practice_feedback_status check (status in ('PENDING', 'READY', 'FAILED'))
);

create index ix_practice_feedback_attempt on practice_feedback (attempt_id, created_at);
create index ix_practice_feedback_account on practice_feedback (account_id, created_at);
create unique index uq_practice_feedback_request on practice_feedback (request_ref) where request_ref is not null;
create unique index uq_practice_feedback_task on practice_feedback (task_id) where task_id is not null;
-- BR-U08-44: mỗi lượt tối đa một yêu cầu đang chờ.
create unique index uq_practice_feedback_pending on practice_feedback (attempt_id) where status = 'PENDING';

create table activity_events (
    id              uuid primary key,
    account_id      uuid not null references accounts(id),
    class_id        uuid not null references course_classes(id),
    work_kind       varchar(20) not null,
    work_id         uuid not null,
    session_id      uuid not null,
    client_event_id varchar(64) not null,
    sequence        bigint not null,
    kind            varchar(14) not null,
    client_time     timestamptz,
    server_time     timestamptz not null,
    policy_version  integer not null default 1,
    constraint uq_activity_events_client unique (account_id, work_kind, work_id, client_event_id),
    constraint ck_activity_events_work check (work_kind in ('ASSIGNMENT_ATTEMPT', 'QUIZ_ATTEMPT', 'GROUP_DOCUMENT')),
    constraint ck_activity_events_kind check (kind in ('SESSION_START', 'HIDDEN', 'VISIBLE', 'HEARTBEAT', 'SESSION_END'))
);

create index ix_activity_events_work on activity_events (work_kind, work_id, account_id, server_time);
create index ix_activity_events_server_time on activity_events (server_time);

-- Lượt và phản hồi không xóa; sự kiện hoạt động xóa theo thời hạn giữ (BR-U08-54).
-- Default privileges của nền có DELETE nên thu lại trên hai bảng không được xóa.
grant select, insert, update on assignment_attempts, practice_feedback, activity_events to app;
grant delete on activity_events to app;
revoke delete on assignment_attempts, practice_feedback from app;
