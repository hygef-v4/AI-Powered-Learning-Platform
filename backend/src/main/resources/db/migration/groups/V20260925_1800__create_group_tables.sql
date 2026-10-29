-- U12: nhóm của lớp, yêu cầu đổi trưởng nhóm, tài liệu nhóm (domain-entities U12). FK assignment_id -> assignments
-- do migration của U07 thêm khi có bảng.

-- Version chung của bộ nhóm một lớp: lưu cả bộ một lần kèm version (BR-U12-04, P2).
create table class_group_sets (
    class_id   uuid        primary key references course_classes (id),
    version    bigint      not null default 0 check (version >= 0),
    updated_at timestamptz not null
);

create table class_groups (
    id         uuid         primary key,
    class_id   uuid         not null references course_classes (id),
    name       varchar(100) not null check (length(trim(name)) > 0),
    leader_id  uuid         references accounts (id),
    created_by uuid         references accounts (id),
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    constraint uq_class_groups_name unique (class_id, name)
);

-- Mỗi sinh viên tối đa một nhóm trong lớp (BR-U12-02).
create table class_group_members (
    group_id   uuid        not null references class_groups (id) on delete cascade,
    class_id   uuid        not null references course_classes (id),
    account_id uuid        not null references accounts (id),
    joined_at  timestamptz not null,
    primary key (group_id, account_id),
    constraint uq_class_group_members_student unique (class_id, account_id)
);

create table leader_change_requests (
    id                 uuid          primary key,
    group_id           uuid          not null references class_groups (id) on delete cascade,
    requested_by       uuid          not null references accounts (id),
    proposed_leader_id uuid          references accounts (id),
    reason             varchar(1000) not null check (length(trim(reason)) between 10 and 1000),
    status             varchar(20)   not null check (status in ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    new_leader_id      uuid          references accounts (id),
    decided_by         uuid          references accounts (id),
    decided_at         timestamptz,
    decision_reason    varchar(1000),
    created_at         timestamptz   not null
);
-- Tối đa một yêu cầu đang chờ mỗi nhóm (BR-U12-10).
create unique index uq_leader_change_requests_pending on leader_change_requests (group_id) where status = 'PENDING';

create table group_documents (
    id                  uuid        primary key,
    group_id            uuid        not null references class_groups (id) on delete cascade,
    assignment_id       uuid        not null,
    class_id            uuid        not null references course_classes (id),
    status              varchar(20) not null check (status in ('IN_PROGRESS', 'CLOSED')),
    shared_blocks       jsonb       not null default '{"blocks":[]}'::jsonb,
    activity_monitoring boolean     not null default false,
    auto_submit_reason  varchar(20) check (auto_submit_reason in ('AUTO_DEADLINE', 'AUTO_RETIRED')),
    submitted_document  jsonb,
    section_authors     jsonb,
    submitted_by        uuid        references accounts (id),
    submit_mode         varchar(20) check (submit_mode in ('MANUAL', 'AUTO_DEADLINE', 'AUTO_RETIRED')),
    submitted_at        timestamptz,
    late                boolean     not null default false,
    receipt_hash        varchar(100),
    warnings            jsonb       not null default '[]'::jsonb,
    last_changed_at     timestamptz not null,
    version             bigint      not null default 0 check (version >= 0),
    created_at          timestamptz not null,
    -- Một tài liệu cho mỗi nhóm và bài (P3, NFR-U12-11).
    constraint uq_group_documents unique (group_id, assignment_id)
);
create index ix_group_documents_status_assignment on group_documents (status, assignment_id);
create index ix_group_documents_class_assignment on group_documents (class_id, assignment_id);

create table group_document_sections (
    id                   uuid         primary key,
    group_document_id    uuid         not null references group_documents (id) on delete cascade,
    part_id              varchar(200) not null,
    heading_block_id     uuid,
    ancestor_heading_ids jsonb        not null default '[]'::jsonb,
    order_no             integer      not null,
    title                varchar(500) not null,
    status               varchar(20)  not null check (status in ('OPEN', 'CLAIMED', 'DONE')),
    claimed_by           uuid         references accounts (id),
    claimed_at           timestamptz,
    assigned_by          uuid         references accounts (id),
    published_blocks     jsonb,
    draft_blocks         jsonb,
    draft_saved_at       timestamptz,
    last_author_id       uuid         references accounts (id),
    version              bigint       not null default 0 check (version >= 0),
    constraint uq_group_document_sections unique (group_document_id, part_id),
    constraint ck_group_document_sections_claim check ((status = 'CLAIMED') = (claimed_by is not null))
);

-- Lịch sử mục chỉ thêm (BR-U12-35, SECURITY-13).
create table group_document_revisions (
    id         uuid        primary key,
    section_id uuid        not null references group_document_sections (id) on delete cascade,
    author_id  uuid        not null references accounts (id),
    blocks     jsonb       not null,
    created_at timestamptz not null
);
create index ix_group_document_revisions_section on group_document_revisions (section_id, created_at);

-- User ứng dụng: SELECT, INSERT, UPDATE; DELETE chỉ thành viên và nhóm chưa dùng (code kiểm). Không có role app thì bỏ qua.
do $$
begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        revoke all on class_group_sets, class_groups, class_group_members, leader_change_requests, group_documents,
            group_document_sections, group_document_revisions from app;
        grant select, insert, update on class_group_sets, class_groups, class_group_members, leader_change_requests,
            group_documents, group_document_sections, group_document_revisions to app;
        grant delete on class_groups, class_group_members to app;
    end if;
end
$$;
