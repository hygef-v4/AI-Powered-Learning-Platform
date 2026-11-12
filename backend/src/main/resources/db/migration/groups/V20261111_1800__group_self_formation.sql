-- U12: sinh viên tự chia nhóm, giảng viên duyệt (BR-U12-70…96). Chỉ thêm bảng; bảng nhóm hiện có không đổi.

-- Cài đặt đợt tự chia nhóm, một dòng mỗi lớp (BR-U12-70).
create table group_formation_settings (
    class_id   uuid        primary key references course_classes (id),
    enabled    boolean     not null default false,
    min_size   integer     not null check (min_size between 2 and 20),
    max_size   integer     not null check (max_size between 2 and 20),
    deadline   timestamptz,
    updated_by uuid        references accounts (id),
    updated_at timestamptz not null,
    version    bigint      not null default 0 check (version >= 0),
    constraint ck_group_formation_settings_size check (min_size <= max_size)
);

-- Đề xuất nhóm của sinh viên; chỉ thành nhóm chính thức khi giảng viên duyệt (BR-U12-75, 83).
create table group_proposals (
    id                uuid          primary key,
    class_id          uuid          not null references course_classes (id),
    name              varchar(100)  not null check (length(trim(name)) > 0),
    leader_id         uuid          not null references accounts (id),
    status            varchar(20)   not null
        check (status in ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    submitted_at      timestamptz,
    decided_by        uuid          references accounts (id),
    decided_at        timestamptz,
    decision_reason   varchar(1000),
    approved_group_id uuid          references class_groups (id) on delete set null,
    end_reason        varchar(30)
        check (end_reason in ('LEADER_CANCELLED', 'LEADER_PLACED', 'LEADER_LEFT_CLASS')),
    created_at        timestamptz   not null,
    updated_at        timestamptz   not null,
    version           bigint        not null default 0 check (version >= 0),
    constraint ck_group_proposals_end_reason check ((status = 'CANCELLED') = (end_reason is not null))
);
create index ix_group_proposals_class_status on group_proposals (class_id, status);

-- Lời mời và thành viên của đề xuất; một dòng mỗi cặp đề xuất–sinh viên (BR-U12-77, 78).
create table group_proposal_members (
    proposal_id  uuid        not null references group_proposals (id),
    class_id     uuid        not null references course_classes (id),
    account_id   uuid        not null references accounts (id),
    status       varchar(20) not null
        check (status in ('INVITED', 'ACCEPTED', 'DECLINED', 'REVOKED', 'EXPIRED', 'LEFT')),
    active       boolean     not null default false,
    invited_by   uuid        references accounts (id),
    created_at   timestamptz not null,
    responded_at timestamptz,
    primary key (proposal_id, account_id),
    constraint ck_group_proposal_members_active check (not active or status = 'ACCEPTED')
);
-- Một sinh viên chỉ ở một đề xuất đang hoạt động trong lớp, kể cả khi hai thao tác chạy cùng lúc (BR-U12-78).
create unique index uq_group_proposal_members_active on group_proposal_members (class_id, account_id) where active;
create index ix_group_proposal_members_account on group_proposal_members (class_id, account_id, status);

-- User ứng dụng: SELECT, INSERT, UPDATE; không xóa để giữ lịch sử (quyết định Q5). Không có role app thì bỏ qua.
do $$
begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        revoke all on group_formation_settings, group_proposals, group_proposal_members from app;
        grant select, insert, update on group_formation_settings, group_proposals, group_proposal_members to app;
    end if;
end
$$;
