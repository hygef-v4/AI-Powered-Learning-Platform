-- U06: kết nối AI, task AI, usage, quota API chung, credential MCP (domain-entities U06).

create table ai_connections (
    id               uuid         primary key,
    scope            varchar(20)  not null check (scope in ('INSTITUTIONAL', 'PERSONAL')),
    owner_id         uuid         references accounts (id),
    provider         varchar(20)  not null check (provider in ('GEMINI', 'OPENAI', 'CLAUDE')),
    model            varchar(100) not null,
    key_ciphertext   bytea,
    key_iv           bytea,
    key_version      integer,
    key_hint         varchar(4),
    status           varchar(20)  not null check (status in ('ACTIVE', 'DISABLED', 'REVOKED')),
    last_test_at     timestamptz,
    last_test_result varchar(500),
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    version          bigint       not null default 0 check (version >= 0),
    -- BR-U06-01: kết nối cá nhân có chủ, kết nối của trường không; chỉ API chung có DISABLED.
    constraint ck_ai_connections_owner check ((scope = 'PERSONAL') = (owner_id is not null)),
    constraint ck_ai_connections_disabled check (status <> 'DISABLED' or scope = 'INSTITUTIONAL'),
    -- BR-U06-03: thu hồi thì không còn bản mã.
    constraint ck_ai_connections_key check ((status = 'REVOKED') = (key_ciphertext is null))
);
-- BR-U06-02: một kết nối chưa thu hồi cho mỗi người và provider; một cho mỗi provider với API chung.
create unique index uq_ai_connections_personal on ai_connections (owner_id, provider)
    where scope = 'PERSONAL' and status <> 'REVOKED';
create unique index uq_ai_connections_institutional on ai_connections (provider)
    where scope = 'INSTITUTIONAL' and status <> 'REVOKED';

create table ai_tasks (
    id                  uuid         primary key,
    request_ref         varchar(100) not null unique,
    fingerprint         varchar(100) not null,
    actor_id            uuid         not null references accounts (id),
    source              varchar(20)  not null check (source in ('SHARED', 'PERSONAL')),
    connection_id       uuid         references ai_connections (id),
    provider            varchar(20)  not null,
    model               varchar(100) not null,
    task_type           varchar(50)  not null,
    target_ref          varchar(200) not null,
    input_version       bigint       not null,
    state               varchar(20)  not null check (state in ('QUEUED', 'RUNNING', 'READY', 'FAILED', 'UNKNOWN', 'CANCELLED')),
    input_payload       jsonb,
    output_schema       jsonb,
    result_payload      jsonb,
    max_output_tokens   integer      not null check (max_output_tokens > 0),
    reserved_tokens     bigint,
    quota_day           date,
    dispatched_at       timestamptz,
    provider_request_id varchar(200),
    apply_result        varchar(20)  check (apply_result in ('APPLIED', 'STALE', 'REJECTED')),
    handler_done        boolean      not null default false,
    safe_error          varchar(500),
    created_at          timestamptz  not null,
    finished_at         timestamptz,
    version             bigint       not null default 0 check (version >= 0)
);
create index ix_ai_tasks_actor_state on ai_tasks (actor_id, state);
create index ix_ai_tasks_state_created on ai_tasks (state, created_at);

create table ai_usage_records (
    id                 uuid         primary key,
    account_id         uuid         not null references accounts (id),
    source             varchar(20)  not null check (source in ('SHARED', 'PERSONAL', 'MCP')),
    task_id            uuid,
    mcp_credential_id  uuid,
    provider           varchar(20),
    model              varchar(100),
    task_type          varchar(50),
    tool_name          varchar(100),
    outcome            varchar(30)  not null,
    input_tokens       bigint,
    output_tokens      bigint,
    cached_tokens      bigint,
    reasoning_tokens   bigint,
    provenance         varchar(30)  not null check (provenance in ('PROVIDER_MEASURED', 'ESTIMATED', 'EXTERNALLY_REPORTED', 'UNAVAILABLE')),
    created_at         timestamptz  not null
);
create index ix_ai_usage_account_created on ai_usage_records (account_id, created_at desc);
create index ix_ai_usage_source_created on ai_usage_records (source, created_at desc);

-- Một dòng cấu hình; rỗng là chưa cấu hình (BR-U06-12).
create table ai_quota_policy (
    id                 smallint    primary key check (id = 1),
    default_day_tokens bigint      check (default_day_tokens >= 0),
    global_day_tokens  bigint      check (global_day_tokens >= 0),
    version            bigint      not null default 0,
    updated_by         uuid        references accounts (id),
    updated_at         timestamptz not null
);

create table ai_teacher_quotas (
    account_id  uuid        primary key references accounts (id),
    day_tokens  bigint      not null check (day_tokens >= 0),
    version     bigint      not null default 0,
    updated_by  uuid        references accounts (id),
    updated_at  timestamptz not null
);

-- Bộ đếm theo cửa sổ ngày (Asia/Ho_Chi_Minh); scope_key là accountId hoặc GLOBAL (P1).
create table ai_quota_windows (
    scope_key       varchar(40) not null,
    day             date        not null,
    reserved_tokens bigint      not null default 0 check (reserved_tokens >= 0),
    used_tokens     bigint      not null default 0 check (used_tokens >= 0),
    primary key (scope_key, day)
);

create table mcp_credentials (
    id           uuid        primary key,
    account_id   uuid        not null references accounts (id),
    label        varchar(60) not null check (length(trim(label)) > 0),
    access       varchar(20) not null check (access in ('READ_ONLY', 'READ_WRITE')),
    token_hash   varchar(64) not null unique,
    token_hint   varchar(4)  not null,
    expires_at   timestamptz not null,
    revoked_at   timestamptz,
    last_used_at timestamptz,
    created_at   timestamptz not null,
    constraint ck_mcp_credentials_expiry check (expires_at <= created_at + interval '90 days')
);
create index ix_mcp_credentials_account on mcp_credentials (account_id);

-- User ứng dụng không xóa dữ liệu U06, trừ mức riêng của giảng viên (bỏ mức riêng thì về mức chung). Không có role
-- app thì bỏ qua.
do $$
begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        revoke all on ai_connections, ai_tasks, ai_usage_records, ai_quota_policy, ai_teacher_quotas,
            ai_quota_windows, mcp_credentials from app;
        grant select, insert, update on ai_connections, ai_tasks, ai_usage_records, ai_quota_policy, ai_teacher_quotas,
            ai_quota_windows, mcp_credentials to app;
        grant delete on ai_teacher_quotas to app;
    end if;
end
$$;
