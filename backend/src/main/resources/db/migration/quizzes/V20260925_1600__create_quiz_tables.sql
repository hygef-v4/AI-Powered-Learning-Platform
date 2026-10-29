-- U10: ngân hàng câu hỏi, Quiz, câu trong Quiz và đề xuất AI (infrastructure-design U10).
-- Chạy sau subjects (1000), course_classes (1100) và accounts (0930) vì có FK sang các bảng đó.
-- version trong DB bắt đầu 0 (khóa lạc quan, NFR-U10-10).

create table quizzes (
    id                          uuid primary key,
    scope                       varchar(10) not null,
    class_id                    uuid,
    subject_id                  uuid not null,
    source_quiz_id              uuid,
    origin                      varchar(12) not null default 'MANUAL',
    version_no                  integer not null default 1,
    title                       varchar(200) not null,
    instructions                varchar(5000),
    settings                    jsonb not null,
    activity_monitoring_enabled boolean not null default false,
    activity_notice             varchar(1000),
    reference_material_ids      jsonb not null default '[]'::jsonb,
    status                      varchar(10) not null default 'DRAFT',
    published_at                timestamptz,
    retired_at                  timestamptz,
    retired_reason              varchar(1000),
    created_by                  uuid not null,
    created_at                  timestamptz not null,
    updated_at                  timestamptz not null,
    version                     bigint not null default 0,
    constraint ck_quizzes_scope check (
        (scope = 'CLASS' and class_id is not null) or (scope = 'SUBJECT' and class_id is null)),
    constraint ck_quizzes_origin check (origin in ('MANUAL', 'CLONE', 'NEW_VERSION', 'CLASS_COPY')),
    constraint ck_quizzes_source check (origin = 'MANUAL' or source_quiz_id is not null),
    constraint ck_quizzes_version_no check (version_no >= 1),
    constraint ck_quizzes_title check (length(btrim(title)) > 0),
    constraint ck_quizzes_status check (status in ('DRAFT', 'REVIEWED', 'OPEN', 'RETIRED')),
    constraint ck_quizzes_published check (status not in ('OPEN', 'RETIRED') or published_at is not null),
    constraint ck_quizzes_retired check (status <> 'RETIRED' or retired_at is not null),
    constraint ck_quizzes_material_refs check (
        jsonb_typeof(reference_material_ids) = 'array' and jsonb_array_length(reference_material_ids) <= 10),
    constraint ck_quizzes_updated check (updated_at >= created_at),
    constraint ck_quizzes_version check (version >= 0),
    constraint fk_quizzes_class foreign key (class_id) references course_classes (id),
    constraint fk_quizzes_subject foreign key (subject_id) references subjects (id),
    constraint fk_quizzes_source foreign key (source_quiz_id) references quizzes (id),
    constraint fk_quizzes_created_by foreign key (created_by) references accounts (id)
);

create index ix_quizzes_class_status on quizzes (class_id, status);
create index ix_quizzes_subject_scope_status on quizzes (subject_id, scope, status);
-- Mỗi Quiz nguồn tối đa một version mới đang soạn (BR-U10-30, NFR-U10-11).
create unique index ux_quizzes_open_new_version on quizzes (source_quiz_id)
    where origin = 'NEW_VERSION' and status in ('DRAFT', 'REVIEWED');

-- Một dòng là một phiên bản câu: câu ngân hàng của môn (BANK) hoặc câu riêng của Quiz (QUIZ).
create table questions (
    id                 uuid primary key,
    lineage_id         uuid not null,
    version_no         integer not null,
    scope              varchar(10) not null,
    subject_id         uuid not null,
    quiz_id            uuid,
    type               varchar(12) not null,
    stem               text not null,
    explanation        text,
    options            jsonb not null,
    correct_option_ids jsonb not null,
    default_points     numeric(6, 2) not null,
    difficulty         varchar(10) not null,
    tags               jsonb not null default '[]'::jsonb,
    status             varchar(10) not null default 'DRAFT',
    activated_at       timestamptz,
    created_by         uuid not null,
    created_at         timestamptz not null,
    updated_at         timestamptz not null,
    version            bigint not null default 0,
    constraint uq_questions_lineage_version unique (lineage_id, version_no),
    constraint ck_questions_version_no check (version_no >= 1),
    constraint ck_questions_scope check (
        (scope = 'BANK' and quiz_id is null) or (scope = 'QUIZ' and quiz_id is not null and status = 'DRAFT')),
    constraint ck_questions_type check (type in ('MCQ_SINGLE', 'MCQ_MULTI')),
    constraint ck_questions_stem check (length(btrim(stem)) > 0 and char_length(stem) <= 20000),
    constraint ck_questions_explanation check (explanation is null or char_length(explanation) <= 20000),
    constraint ck_questions_options check (
        jsonb_typeof(options) = 'array' and jsonb_array_length(options) between 2 and 6),
    constraint ck_questions_correct check (
        jsonb_typeof(correct_option_ids) = 'array' and jsonb_array_length(correct_option_ids) >= 1
        and (type <> 'MCQ_SINGLE' or jsonb_array_length(correct_option_ids) = 1)),
    constraint ck_questions_points check (default_points > 0),
    constraint ck_questions_difficulty check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
    constraint ck_questions_tags check (jsonb_typeof(tags) = 'array' and jsonb_array_length(tags) <= 10),
    constraint ck_questions_status check (status in ('DRAFT', 'ACTIVE', 'RETIRED')),
    constraint ck_questions_activated check (status = 'DRAFT' or activated_at is not null),
    constraint ck_questions_updated check (updated_at >= created_at),
    constraint ck_questions_version check (version >= 0),
    constraint fk_questions_subject foreign key (subject_id) references subjects (id),
    constraint fk_questions_quiz foreign key (quiz_id) references quizzes (id),
    constraint fk_questions_created_by foreign key (created_by) references accounts (id)
);

create index ix_questions_subject_status on questions (subject_id, status) where scope = 'BANK';
create index ix_questions_lineage on questions (lineage_id, version_no desc);
create index ix_questions_quiz on questions (quiz_id) where quiz_id is not null;
-- Mỗi lineage của ngân hàng tối đa một bản nháp (BR-U10-04, NFR-U10-11).
create unique index ux_questions_bank_draft on questions (lineage_id) where scope = 'BANK' and status = 'DRAFT';

-- Câu trong Quiz ghim id phiên bản câu (BR-U10-04, 13).
create table quiz_items (
    quiz_id     uuid not null,
    question_id uuid not null,
    position    integer not null,
    points      numeric(6, 2) not null,
    primary key (quiz_id, question_id),
    constraint uq_quiz_items_position unique (quiz_id, position) deferrable initially deferred,
    constraint ck_quiz_items_position check (position >= 1 and position <= 200),
    constraint ck_quiz_items_points check (points > 0),
    constraint fk_quiz_items_quiz foreign key (quiz_id) references quizzes (id),
    constraint fk_quiz_items_question foreign key (question_id) references questions (id)
);

create index ix_quiz_items_question on quiz_items (question_id);

-- Đề xuất câu từ AI hoặc MCP, chờ người soạn chấp nhận (P4).
create table quiz_ai_drafts (
    id           uuid primary key,
    target       varchar(5) not null,
    quiz_id      uuid,
    subject_id   uuid not null,
    origin       varchar(8) not null,
    task_id      uuid,
    status       varchar(10) not null default 'PENDING',
    proposal     jsonb not null default '[]'::jsonb,
    provenance   jsonb not null default '{}'::jsonb,
    base_version bigint,
    created_by   uuid not null,
    created_at   timestamptz not null,
    decided_at   timestamptz,
    constraint ck_quiz_ai_drafts_target check (
        (target = 'QUIZ' and quiz_id is not null) or (target = 'BANK' and quiz_id is null)),
    constraint ck_quiz_ai_drafts_origin check (origin in ('AI_TASK', 'MCP')),
    constraint ck_quiz_ai_drafts_status check (status in ('PENDING', 'READY', 'ACCEPTED', 'DISCARDED', 'FAILED')),
    constraint fk_quiz_ai_drafts_quiz foreign key (quiz_id) references quizzes (id),
    constraint fk_quiz_ai_drafts_subject foreign key (subject_id) references subjects (id),
    constraint fk_quiz_ai_drafts_created_by foreign key (created_by) references accounts (id)
);

create index ix_quiz_ai_drafts_quiz_status on quiz_ai_drafts (quiz_id, status);
create index ix_quiz_ai_drafts_subject_status on quiz_ai_drafts (subject_id, status);

-- DELETE chỉ dùng cho câu nháp chưa từng kích hoạt, Quiz chưa từng phát hành, câu trong Quiz và đề xuất AI;
-- điều kiện kiểm trong code (infrastructure-design U10).
grant select, insert, update, delete on quizzes, questions, quiz_items, quiz_ai_drafts to app;
