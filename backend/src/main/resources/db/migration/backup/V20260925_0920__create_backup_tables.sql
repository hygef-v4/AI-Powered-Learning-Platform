-- U03: lịch sử các lần backup database (FR-038).
create table backup_runs (
    id             uuid         primary key,
    scheduled_for  date         not null unique,
    state          varchar(10)  not null check (state in ('CREATED', 'RUNNING', 'SUCCESS', 'FAILED')),
    created_at     timestamptz  not null,
    snapshot_at    timestamptz,
    schema_version varchar(50),
    app_version    varchar(50),
    archive_ref    varchar(200),
    archive_sha256 char(64),
    byte_size      bigint,
    started_at     timestamptz,
    completed_at   timestamptz,
    purged_at      timestamptz,
    safe_error     varchar(500)
);

create index backup_runs_state_snapshot on backup_runs (state, snapshot_at);
