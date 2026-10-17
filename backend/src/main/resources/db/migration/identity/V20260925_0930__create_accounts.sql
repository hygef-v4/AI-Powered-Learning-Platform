-- U01: tài khoản (domain-entities U01 mục 2–5). Không bảng cấu hình; tên miền email là biến triển khai.
-- FK audit_logs.actor_id -> accounts thêm ở migration riêng khi bảng audit_logs của U02 đã có (Q02).
create table accounts (
    id                  uuid         primary key,
    school_email        varchar(254) not null,
    role                varchar(20)  not null,
    status              varchar(20)  not null,
    password_hash       varchar(100),
    password_changed_at timestamptz,
    display_name        varchar(150) not null,
    phone_number        varchar(16),
    failed_login_count  int          not null default 0,
    locked_until        timestamptz,
    -- U14 ghi tùy chọn nhận email; U01 không đọc, không sửa.
    email_preferences   jsonb        not null default '{}'::jsonb,
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now(),
    version             bigint       not null default 1,
    constraint accounts_school_email_key unique (school_email),
    -- BR-U01-02: email lưu ở dạng đã chuẩn hóa.
    constraint accounts_school_email_normalized check (school_email = lower(btrim(school_email))),
    constraint accounts_role_check check (role in ('STUDENT', 'TEACHER', 'SUBJECT_MANAGER', 'ADMIN')),
    -- BR-U01-70: chỉ ba trạng thái.
    constraint accounts_status_check check (status in ('PENDING', 'ACTIVE', 'DISABLED')),
    -- Tài khoản ACTIVE luôn có mật khẩu; PENDING thì chưa (BR-U01-10, 13).
    constraint accounts_active_has_password check (status <> 'ACTIVE' or password_hash is not null),
    constraint accounts_failed_login_count_check check (failed_login_count >= 0)
);

-- Account List lọc theo role và trạng thái (UC 08); Admin Dashboard đếm theo role và trạng thái (U15).
create index accounts_role_status_idx on accounts (role, status);

alter table system_settings
    add constraint system_settings_updated_by_fkey foreign key (updated_by) references accounts (id);
