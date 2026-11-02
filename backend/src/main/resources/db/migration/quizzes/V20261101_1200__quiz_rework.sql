-- Làm lại Quiz (góp ý 2026-10-29): bỏ bước duyệt Quiz, thêm sinh đề theo quy tắc, chống trùng câu, duyệt câu đề xuất.
-- Cột reference_material_ids không còn được dùng; giữ lại để dễ quay lại, xóa bằng migration riêng sau khi kiểm dữ liệu.

-- BR-U10-20: bỏ REVIEWED, Quiz phát hành thẳng từ nháp.
update quizzes set status = 'DRAFT' where status = 'REVIEWED';
alter table quizzes drop constraint ck_quizzes_status;
alter table quizzes add constraint ck_quizzes_status check (status in ('DRAFT', 'OPEN', 'RETIRED'));
drop index ux_quizzes_open_new_version;
create unique index ux_quizzes_open_new_version on quizzes (source_quiz_id)
    where origin = 'NEW_VERSION' and status = 'DRAFT';

-- BR-U10-17: FIXED dùng quiz_items; GENERATED bốc câu từ ngân hàng cho từng lượt theo quiz_draw_rules.
alter table quizzes add column paper_mode varchar(10) not null default 'FIXED';
alter table quizzes add constraint ck_quizzes_paper_mode check (paper_mode in ('FIXED', 'GENERATED'));

create table quiz_draw_rules (
    quiz_id        uuid not null,
    position       integer not null,
    tag            varchar(100) not null,
    difficulty     varchar(10),
    question_count integer not null,
    points_each    numeric(6, 2) not null,
    primary key (quiz_id, position),
    constraint ck_quiz_draw_rules_position check (position >= 1),
    constraint ck_quiz_draw_rules_tag check (length(btrim(tag)) > 0),
    constraint ck_quiz_draw_rules_difficulty check (difficulty is null or difficulty in ('EASY', 'MEDIUM', 'HARD')),
    constraint ck_quiz_draw_rules_count check (question_count between 1 and 200),
    constraint ck_quiz_draw_rules_points check (points_each > 0),
    constraint fk_quiz_draw_rules_quiz foreign key (quiz_id) references quizzes (id) on delete cascade
);

-- BR-U10-08: dấu vân tay nội dung để chống trùng; dòng cũ được điền khi ứng dụng khởi động (QuestionFingerprintBackfill).
alter table questions add column content_hash varchar(64);
alter table questions add column normalized_stem text;
create index ix_questions_subject_hash on questions (subject_id, content_hash) where scope = 'BANK';
create index ix_questions_missing_fingerprint on questions (id) where content_hash is null;

-- BR-U10-09: giảng viên đề xuất câu; chủ nhiệm môn duyệt hoặc từ chối.
alter table questions drop constraint ck_questions_status;
alter table questions add constraint ck_questions_status
    check (status in ('DRAFT', 'PENDING', 'REJECTED', 'ACTIVE', 'RETIRED'));
alter table questions drop constraint ck_questions_activated;
alter table questions add constraint ck_questions_activated
    check (status in ('DRAFT', 'PENDING', 'REJECTED') or activated_at is not null);
drop index ux_questions_bank_draft;
create unique index ux_questions_bank_draft on questions (lineage_id)
    where scope = 'BANK' and status in ('DRAFT', 'PENDING', 'REJECTED');
create index ix_questions_created_by_status on questions (created_by, status) where scope = 'BANK';

create table question_reviews (
    id          uuid primary key,
    question_id uuid not null,
    decision    varchar(10) not null,
    reason      varchar(1000),
    reviewer_id uuid not null,
    created_at  timestamptz not null,
    constraint ck_question_reviews_decision check (decision in ('APPROVED', 'REJECTED')),
    constraint ck_question_reviews_reason check (decision = 'APPROVED' or length(btrim(reason)) > 0),
    constraint fk_question_reviews_question foreign key (question_id) references questions (id) on delete cascade,
    constraint fk_question_reviews_reviewer foreign key (reviewer_id) references accounts (id)
);
create index ix_question_reviews_question on question_reviews (question_id, created_at desc);

-- Lớp mà giảng viên gửi đề xuất, để xem lại và mở thông báo đúng lớp.
create table question_proposals (
    question_id uuid primary key,
    class_id    uuid not null,
    created_at  timestamptz not null,
    constraint fk_question_proposals_question foreign key (question_id) references questions (id) on delete cascade,
    constraint fk_question_proposals_class foreign key (class_id) references course_classes (id)
);
create index ix_question_proposals_class on question_proposals (class_id);

grant select, insert, update, delete on quiz_draw_rules, question_reviews, question_proposals to app;
