-- U07: bài tập (một dòng là một version), rubric theo phần, đề xuất AI (domain-entities U07 mục 2, 4).
-- Chạy sau subjects (1000) và classes (1100) vì có FK sang hai bảng đó. version trong DB bắt đầu 0.
create table assignments (
    id                          uuid primary key,
    scope                       varchar(10) not null,
    class_id                    uuid references course_classes(id),
    subject_id                  uuid not null references subjects(id),
    source_assignment_id        uuid references assignments(id) on delete set null,
    origin                      varchar(12) not null,
    version_no                  integer not null default 1,
    submission_mode             varchar(12) not null,
    diagram_enabled             boolean not null default false,
    grading_mode                varchar(10) not null,
    title                       varchar(200) not null,
    instructions                text not null default '',
    skeleton                    jsonb not null,
    parts                       jsonb not null,
    required_diagrams           jsonb not null default '{}'::jsonb,
    reference_material_ids      jsonb not null default '[]'::jsonb,
    activity_monitoring_enabled boolean not null default false,
    activity_notice             varchar(1000),
    status                      varchar(10) not null default 'DRAFT',
    opens_at                    timestamptz,
    closes_at                   timestamptz,
    late_until                  timestamptz,
    max_attempts                integer,
    retired_reason              varchar(500),
    created_by                  uuid not null references accounts(id),
    created_at                  timestamptz not null,
    updated_at                  timestamptz not null,
    version                     bigint not null default 0,
    constraint ck_assignments_scope check (scope in ('CLASS', 'SUBJECT')),
    constraint ck_assignments_scope_owner check ((scope = 'CLASS' and class_id is not null)
        or (scope = 'SUBJECT' and class_id is null and submission_mode = 'INDIVIDUAL')),
    constraint ck_assignments_origin check (origin in ('MANUAL', 'CLONE', 'NEW_VERSION', 'CLASS_COPY')),
    constraint ck_assignments_version_no check (version_no >= 1),
    constraint ck_assignments_submission_mode check (submission_mode in ('INDIVIDUAL', 'GROUP')),
    constraint ck_assignments_grading_mode check (grading_mode in ('GRADED', 'PRACTICE')),
    constraint ck_assignments_group_graded check (submission_mode = 'INDIVIDUAL' or grading_mode = 'GRADED'),
    constraint ck_assignments_title check (length(btrim(title)) > 0),
    constraint ck_assignments_instructions check (length(instructions) <= 20000),
    constraint ck_assignments_status check (status in ('DRAFT', 'REVIEWED', 'SCHEDULED', 'OPEN', 'CLOSED', 'RETIRED')),
    constraint ck_assignments_schedule check (opens_at is null or closes_at > opens_at),
    constraint ck_assignments_late check (late_until is null or (late_until > closes_at
        and late_until <= closes_at + interval '30 days')),
    constraint ck_assignments_attempts check (max_attempts is null or max_attempts between 1 and 10),
    constraint ck_assignments_published check (status in ('DRAFT', 'REVIEWED')
        or (opens_at is not null and closes_at is not null and max_attempts is not null)),
    constraint ck_assignments_version check (version >= 0)
);

create index ix_assignments_class_status on assignments (class_id, status);
create index ix_assignments_subject_scope_status on assignments (subject_id, scope, status);
create index ix_assignments_status_opens on assignments (status, opens_at);
create index ix_assignments_status_closes on assignments (status, closes_at);
-- BR-U07-41: mỗi bài nguồn tối đa một version đang soạn.
create unique index uq_assignments_open_version on assignments (source_assignment_id)
    where origin = 'NEW_VERSION' and status in ('DRAFT', 'REVIEWED');

create table assignment_rubrics (
    id            uuid primary key,
    assignment_id uuid not null references assignments(id) on delete cascade,
    part_id       varchar(64) not null,
    criteria      jsonb not null default '[]'::jsonb,
    total_points  numeric(10, 2) not null default 0,
    locked_at     timestamptz,
    created_at    timestamptz not null,
    updated_at    timestamptz not null,
    version       bigint not null default 0,
    constraint uq_assignment_rubrics_part unique (assignment_id, part_id),
    constraint ck_assignment_rubrics_points check (total_points >= 0)
);

create table assignment_ai_drafts (
    id            uuid primary key,
    assignment_id uuid not null references assignments(id) on delete cascade,
    origin        varchar(10) not null,
    task_id       uuid,
    request_ref   varchar(100),
    status        varchar(10) not null,
    proposal      jsonb,
    provenance    jsonb not null default '{}'::jsonb,
    base_version  bigint not null,
    safe_error    varchar(500),
    created_by    uuid not null references accounts(id),
    created_at    timestamptz not null,
    decided_at    timestamptz,
    constraint ck_assignment_ai_drafts_origin check (origin in ('AI_TASK', 'MCP')),
    constraint ck_assignment_ai_drafts_status check (status in ('PENDING', 'READY', 'APPLIED', 'DISCARDED', 'FAILED'))
);

create index ix_assignment_ai_drafts_assignment on assignment_ai_drafts (assignment_id, status);
create unique index uq_assignment_ai_drafts_task on assignment_ai_drafts (task_id) where task_id is not null;
create unique index uq_assignment_ai_drafts_request on assignment_ai_drafts (assignment_id, request_ref)
    where request_ref is not null;

-- Bài chỉ xóa khi còn DRAFT hoặc REVIEWED; code kiểm trạng thái trước khi xóa (BR-U07-31).
grant select, insert, update, delete on assignments, assignment_rubrics, assignment_ai_drafts to app;
