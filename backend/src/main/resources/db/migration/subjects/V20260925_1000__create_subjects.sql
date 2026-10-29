-- U04: chạy sau accounts của U01. version trong DB bắt đầu 0; public version = DB + 1.
-- Không tạo accounts tại đây, không thay migration đã áp dụng trên DB chia sẻ.
create table subjects (
    id          uuid primary key,
    code        varchar(20) not null,
    name        varchar(200) not null,
    description varchar(2000),
    status      varchar(20) not null default 'ACTIVE',
    manager_id  uuid,
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint not null default 0,
    constraint uk_subjects_code unique (code),
    constraint ck_subjects_code check (code ~ '^[A-Z0-9-]{1,20}$'),
    constraint ck_subjects_name check (length(btrim(name)) > 0),
    constraint ck_subjects_status check (status in ('ACTIVE', 'ARCHIVED')),
    constraint ck_subjects_version check (version >= 0 and version < 9223372036854775807),
    constraint fk_subjects_manager foreign key (manager_id) references accounts(id)
);

create index ix_subjects_manager on subjects (manager_id);
create index ix_subjects_status_code on subjects (status, code);

-- Default privileges của nền hiện có bao gồm DELETE; môn chỉ lưu trữ, không xóa.
grant select, insert, update on subjects to app;
revoke delete on subjects from app;
