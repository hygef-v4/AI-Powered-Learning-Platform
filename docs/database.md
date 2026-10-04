# Database Design for SDS

[Open the editable Draw.io physical ERD](database.drawio)

This is the implementation-level PostgreSQL 16 design derived from the current U01-U16 domain and infrastructure artifacts. It contains 24 tables, 35 enforceable foreign keys, and one explicit logical self-reference (`assignments.source_assignment_id`) that the domain design intentionally does not enforce as an FK.

## Conventions

- Primary identifiers use `uuid`; timestamps use `timestamptz`.
- Flexible snapshots and value objects use `jsonb`; semantic vectors use `vector(768)`.
- Enum-like fields use `varchar` plus database `CHECK` constraints so Flyway migrations remain explicit.
- File bytes, sessions, jobs/events and code-sandbox results are not separate PostgreSQL tables; see the Draw.io legend.
- `source_assignment_id`, bank `scope_id`, AI `target_id`, rubric IDs and lesson IDs inside JSON are logical references validated by the owning service, not physical foreign keys.

## Table catalog

### `accounts` (U01)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `school_email` | `varchar(320)` | NO | UK | School email, normalized lowercase |
| `role` | `varchar(20)` | NO |  | STUDENT/TEACHER/SUBJECT_MANAGER/ADMIN |
| `status` | `varchar(16)` | NO |  | PENDING/ACTIVE/DISABLED |
| `password_hash` | `varchar(255)` | YES |  |  |
| `password_changed_at` | `timestamptz` | YES |  |  |
| `display_name` | `varchar(150)` | NO |  |  |
| `phone_number` | `varchar(16)` | YES |  |  |
| `avatar_file_id` | `varchar(128)` | YES |  | Google Drive file id |
| `failed_login_count` | `smallint` | NO |  | default 0 |
| `locked_until` | `timestamptz` | YES |  |  |
| `credential_version` | `integer` | NO |  | default 0 |
| `free_balance` | `bigint` | NO |  | U07, default 0 |
| `free_period` | `char(7)` | YES |  | yyyy-MM |
| `purchased_balance` | `bigint` | NO |  | U07, default 0 |
| `email_preferences` | `jsonb` | NO |  | U16, default {} |
| `created_at` | `timestamptz` | NO |  |  |
| `updated_at` | `timestamptz` | NO |  |  |
| `version` | `integer` | NO |  | optimistic lock |

Constraints: CK role/status; CK balances >= 0; CK phone format.

### `audit_logs` (U02)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `actor_id` | `uuid` | YES | FK | accounts.id; NULL=SYSTEM/WORKER |
| `action` | `varchar(100)` | NO |  |  |
| `object_type` | `varchar(80)` | NO |  |  |
| `object_id` | `varchar(128)` | NO |  |  |
| `result` | `varchar(16)` | NO |  | SUCCESS/DENIED/FAILURE |
| `reason` | `varchar(1000)` | YES |  |  |
| `details` | `jsonb` | NO |  | masked before/after + correlationId |
| `occurred_at` | `timestamptz` | NO |  |  |

Constraints: APPEND ONLY; REVOKE UPDATE/DELETE/TRUNCATE.

### `subjects` (U04)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `code` | `varchar(20)` | NO | UK |  |
| `name` | `varchar(200)` | NO |  |  |
| `description` | `varchar(2000)` | YES |  |  |
| `status` | `varchar(16)` | NO |  | ACTIVE/ARCHIVED |
| `manager_id` | `uuid` | YES | FK | accounts.id |
| `version` | `integer` | NO |  | optimistic lock |

Constraints: CK code = [A-Z0-9-]+.

### `course_classes` (U04)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `subject_id` | `uuid` | NO | FK | subjects.id |
| `teacher_id` | `uuid` | YES | FK | accounts.id |
| `code` | `varchar(30)` | NO |  |  |
| `name` | `varchar(200)` | NO |  |  |
| `description` | `varchar(2000)` | YES |  |  |
| `term` | `varchar(20)` | NO |  |  |
| `status` | `varchar(16)` | NO |  | DRAFT/OPEN/ARCHIVED |
| `invite_code` | `char(8)` | YES | UK |  |
| `invite_enabled` | `boolean` | NO |  | default false |
| `invite_expires_at` | `timestamptz` | YES |  |  |
| `show_grade_distribution` | `boolean` | NO |  | default false |
| `version` | `integer` | NO |  | optimistic lock |

Constraints: UK (subject_id, code); CK OPEN => teacher_id IS NOT NULL; CK invite enabled => code + expiry.

### `enrollments` (U04 REL)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `class_id` | `uuid` | NO | PK,FK | course_classes.id |
| `account_id` | `uuid` | NO | PK,FK | accounts.id |
| `status` | `varchar(16)` | NO |  | ACTIVE/REMOVED |
| `source` | `varchar(16)` | NO |  | MANUAL/LIST/INVITE |
| `enrolled_at` | `timestamptz` | NO |  |  |
| `removed_at` | `timestamptz` | YES |  |  |

Constraints: PK (class_id, account_id); Account must be STUDENT.

### `modules` (U05)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `subject_id` | `uuid` | NO | FK | subjects.id |
| `title` | `varchar(200)` | NO |  |  |
| `order_no` | `integer` | NO |  |  |
| `status` | `varchar(16)` | NO |  | ACTIVE/ARCHIVED |

Constraints: IX (subject_id, order_no); CK order_no >= 0.

### `lessons` (U05)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `module_id` | `uuid` | NO | FK | modules.id |
| `class_id` | `uuid` | YES | FK | course_classes.id; NULL=subject material |
| `title` | `varchar(200)` | NO |  |  |
| `order_no` | `integer` | NO |  |  |
| `source_type` | `varchar(16)` | NO |  | FILE/YOUTUBE |
| `file_id` | `varchar(128)` | YES |  |  |
| `file_name` | `varchar(255)` | YES |  |  |
| `mime_type` | `varchar(100)` | YES |  |  |
| `size_bytes` | `bigint` | YES |  |  |
| `youtube_url` | `text` | YES |  |  |
| `scan_status` | `varchar(24)` | NO |  |  |
| `extracted_text` | `text` | YES |  | max 2,000,000 chars |
| `embedding` | `vector(768)` | YES |  |  |
| `scanned_at` | `timestamptz` | YES |  |  |
| `status` | `varchar(16)` | NO |  | ACTIVE/ARCHIVED |

Constraints: CK FILE xor YOUTUBE fields; HNSW embedding cosine index; IX (module_id,class_id,order_no).

### `announcements` (U05)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `class_id` | `uuid` | NO | FK | course_classes.id |
| `author_id` | `uuid` | NO | FK | accounts.id |
| `title` | `varchar(200)` | NO |  |  |
| `body` | `varchar(5000)` | NO |  |  |
| `status` | `varchar(16)` | NO |  | VISIBLE/HIDDEN |
| `hidden_reason` | `varchar(1000)` | YES |  |  |
| `posted_at` | `timestamptz` | NO |  |  |

Constraints: Immutable after post except moderation status.

### `announcement_comments` (U05 REL)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `announcement_id` | `uuid` | NO | FK | announcements.id |
| `account_id` | `uuid` | NO | FK | accounts.id |
| `body` | `varchar(2000)` | NO |  |  |
| `status` | `varchar(16)` | NO |  | VISIBLE/HIDDEN |
| `hidden_reason` | `varchar(1000)` | YES |  |  |
| `posted_at` | `timestamptz` | NO |  |  |

Constraints: Immutable after post except moderation status.

### `questions` (U06)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK | version id |
| `lineage_id` | `uuid` | NO |  |  |
| `version` | `integer` | NO |  |  |
| `scope_type` | `varchar(16)` | NO |  | SUBJECT/CLASS/ASSIGNMENT |
| `scope_id` | `uuid` | NO |  | polymorphic logical reference |
| `title` | `varchar(200)` | NO |  |  |
| `assignment_type` | `varchar(32)` | NO |  |  |
| `question_type` | `varchar(16)` | NO |  | MCQ_SINGLE/MCQ_MULTI/ESSAY/DOCUMENT/CODE |
| `definition` | `jsonb` | NO |  |  |
| `difficulty` | `varchar(16)` | NO |  | EASY/MEDIUM/HARD |
| `tags` | `text[]` | NO |  | default {} |
| `status` | `varchar(16)` | NO |  | DRAFT/ACTIVE/RETIRED |
| `created_at` | `timestamptz` | NO |  |  |

Constraints: UK (lineage_id, version); Partial UK lineage WHERE DRAFT; GIN(tags).

### `rubrics` (U06)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK | version id |
| `lineage_id` | `uuid` | NO |  |  |
| `version` | `integer` | NO |  |  |
| `scope_type` | `varchar(16)` | NO |  | SUBJECT/CLASS/ASSIGNMENT |
| `scope_id` | `uuid` | NO |  | polymorphic logical reference |
| `title` | `varchar(200)` | NO |  |  |
| `criteria` | `jsonb` | NO |  |  |
| `total_points` | `numeric(6,2)` | NO |  |  |
| `status` | `varchar(16)` | NO |  | DRAFT/ACTIVE/RETIRED |
| `created_at` | `timestamptz` | NO |  |  |

Constraints: UK (lineage_id, version); Partial UK lineage WHERE DRAFT; CK total_points > 0.

### `credit_packages` (U07)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `name` | `varchar(100)` | NO |  |  |
| `credits` | `bigint` | NO |  |  |
| `price_vnd` | `bigint` | NO |  |  |
| `active` | `boolean` | NO |  |  |

Constraints: CK credits > 0; CK price_vnd >= 2000.

### `payments` (U07 REL)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `order_code` | `bigint` | NO | UK |  |
| `account_id` | `uuid` | NO | FK | accounts.id |
| `package_id` | `uuid` | NO | FK | credit_packages.id |
| `credits` | `bigint` | NO |  | snapshot |
| `amount_vnd` | `bigint` | NO |  | snapshot |
| `status` | `varchar(16)` | NO |  |  |
| `checkout_url` | `text` | YES |  |  |
| `provider_reference` | `varchar(128)` | YES |  |  |
| `idempotency_key` | `varchar(128)` | NO | UK |  |
| `expires_at` | `timestamptz` | NO |  |  |
| `paid_at` | `timestamptz` | YES |  |  |
| `created_at` | `timestamptz` | NO |  |  |

Constraints: PAID transition once; CK credits/amount > 0.

### `assignments` (U08/U09/U10/U15/U16)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK | one row = one version |
| `class_id` | `uuid` | YES | FK | course_classes.id |
| `subject_id` | `uuid` | YES | FK | subjects.id |
| `source_assignment_id` | `uuid` | YES |  | logical self-reference; NO FK |
| `type` | `varchar(32)` | NO |  |  |
| `grading_mode` | `varchar(16)` | NO |  | GRADED/PRACTICE |
| `title` | `varchar(200)` | NO |  |  |
| `instructions` | `varchar(20000)` | YES |  |  |
| `config` | `jsonb` | NO |  | type config, skeleton, origin |
| `status` | `varchar(16)` | NO |  |  |
| `version` | `integer` | NO |  |  |
| `opens_at` | `timestamptz` | YES |  |  |
| `closes_at` | `timestamptz` | YES |  |  |
| `late_until` | `timestamptz` | YES |  |  |
| `max_attempts` | `smallint` | NO |  | 1..10 |
| `reminder_sent_at` | `timestamptz` | YES |  | U16 |
| `grades_released_at` | `timestamptz` | YES |  | U15 |

Constraints: CK exactly one of class_id/subject_id; CK GROUP_ASSIGNMENT => GRADED; CK opens < closes < late_until.

### `assignment_questions` (U08 REL)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `assignment_id` | `uuid` | NO | PK,FK | assignments.id |
| `question_id` | `uuid` | NO | PK,FK | questions.id |
| `order_no` | `integer` | NO |  |  |
| `points` | `numeric(6,2)` | NO |  |  |

Constraints: PK (assignment_id, question_id); UK (assignment_id, order_no); CK points > 0.

### `attempts` (U11)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `assignment_id` | `uuid` | NO | FK | assignments.id |
| `account_id` | `uuid` | NO | FK | accounts.id |
| `attempt_no` | `smallint` | NO |  |  |
| `snapshot` | `jsonb` | NO |  |  |
| `content` | `jsonb` | NO |  |  |
| `status` | `varchar(16)` | NO |  | IN_PROGRESS/SUBMITTED |
| `submit_mode` | `varchar(24)` | YES |  |  |
| `is_late` | `boolean` | NO |  | default false |
| `started_at` | `timestamptz` | NO |  |  |
| `deadline_at` | `timestamptz` | NO |  |  |
| `submitted_at` | `timestamptz` | YES |  |  |
| `content_version` | `integer` | NO |  |  |
| `receipt_hash` | `char(64)` | YES |  |  |
| `run_result` | `jsonb` | YES |  | U13 CodeRunResult |

Constraints: UK (assignment_id,account_id,attempt_no); Partial UK one IN_PROGRESS per user+assignment.

### `student_groups` (U12)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `class_id` | `uuid` | NO | FK | course_classes.id |
| `name` | `varchar(100)` | NO |  |  |
| `created_at` | `timestamptz` | NO |  |  |
| `version` | `integer` | NO |  | optimistic lock |

Constraints: UK (class_id, name).

### `group_members` (U12 REL)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `group_id` | `uuid` | NO | PK,FK | student_groups.id |
| `account_id` | `uuid` | NO | PK,FK | accounts.id |
| `is_leader` | `boolean` | NO |  | default false |
| `joined_at` | `timestamptz` | NO |  |  |

Constraints: PK (group_id, account_id); Partial UK one leader per group; One group/class enforced transactionally.

### `leader_change_requests` (U12)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `group_id` | `uuid` | NO | FK | student_groups.id |
| `requester_id` | `uuid` | NO | FK | accounts.id |
| `nominee_id` | `uuid` | NO | FK | accounts.id |
| `reason` | `varchar(1000)` | NO |  |  |
| `status` | `varchar(16)` | NO |  |  |
| `decision_reason` | `varchar(1000)` | YES |  |  |
| `created_at` | `timestamptz` | NO |  |  |
| `decided_at` | `timestamptz` | YES |  |  |

Constraints: Partial UK one PENDING per group; REVOKE DELETE.

### `ai_services` (U13)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `task_type` | `varchar(32)` | NO | UK |  |
| `model` | `varchar(64)` | YES |  |  |
| `enabled` | `boolean` | NO |  |  |
| `daily_cost_cap` | `numeric(12,6)` | YES |  |  |
| `rate_per_minute` | `integer` | YES |  |  |
| `updated_at` | `timestamptz` | NO |  |  |

Constraints: GLOBAL row owns global cap/rate/kill switch.

### `ai_suggestions` (U13)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `ai_service_id` | `uuid` | NO | FK | ai_services.id |
| `requested_by` | `uuid` | NO | FK | accounts.id |
| `target_type` | `varchar(32)` | NO |  |  |
| `target_id` | `uuid` | NO |  | polymorphic logical reference |
| `status` | `varchar(24)` | NO |  |  |
| `input_tokens` | `integer` | NO |  | default 0 |
| `output_tokens` | `integer` | NO |  | default 0 |
| `cost_usd` | `numeric(12,6)` | NO |  | default 0 |
| `latency_ms` | `integer` | YES |  |  |
| `credits_reserved` | `bigint` | NO |  | default 0 |
| `free_credits_reserved` | `bigint` | NO |  | default 0 |
| `credits_used` | `bigint` | NO |  | default 0 |
| `credit_status` | `varchar(16)` | NO |  |  |
| `result` | `jsonb` | YES |  |  |
| `created_at` | `timestamptz` | NO |  |  |
| `completed_at` | `timestamptz` | YES |  |  |

Constraints: Partial UK active PRACTICE_ATTEMPT; REVOKE DELETE; No raw prompts stored.

### `group_documents` (U14)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `group_id` | `uuid` | NO | FK | student_groups.id |
| `assignment_id` | `uuid` | NO | FK | assignments.id |
| `status` | `varchar(16)` | NO |  | IN_PROGRESS/CLOSED |
| `sections` | `jsonb` | NO |  | sharedBlocks + items[] |
| `revisions` | `jsonb` | NO |  | append-only array |
| `submitted_snapshot` | `jsonb` | YES |  |  |
| `submit_mode` | `varchar(24)` | YES |  |  |
| `submitted_at` | `timestamptz` | YES |  |  |
| `receipt_hash` | `char(64)` | YES |  |  |
| `is_late` | `boolean` | NO |  | default false |
| `updated_at` | `timestamptz` | NO |  |  |
| `version` | `integer` | NO |  | optimistic lock |

Constraints: UK (group_id, assignment_id); No REVIEW state; Submission snapshot immutable.

### `evaluations` (U15)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `attempt_id` | `uuid` | YES | FK | attempts.id |
| `group_document_id` | `uuid` | YES | FK | group_documents.id |
| `account_id` | `uuid` | YES | FK | accounts.id |
| `kind` | `varchar(24)` | NO |  | ATTEMPT/GROUP_DOCUMENT/MEMBER/PRACTICE |
| `method` | `varchar(16)` | YES |  | DETERMINISTIC/MANUAL |
| `max_score` | `numeric(6,2)` | NO |  |  |
| `score` | `numeric(6,2)` | YES |  |  |
| `rubric_checks` | `jsonb` | NO |  | default {} |
| `feedback` | `varchar(10000)` | YES |  |  |
| `ai_score` | `numeric(6,2)` | YES |  |  |
| `ai_feedback` | `varchar(10000)` | YES |  |  |
| `status` | `varchar(16)` | NO |  |  |
| `history` | `jsonb` | NO |  | append-only array |
| `published_at` | `timestamptz` | YES |  |  |
| `version` | `integer` | NO |  | optimistic lock |

Constraints: CK exactly one submission target; CK 0 <= score <= max_score; Partial UK by kind + target + member.

### `notifications` (U16)

| Column | PostgreSQL type | Null | Key | Description |
|---|---|---:|---|---|
| `id` | `uuid` | NO | PK |  |
| `account_id` | `uuid` | NO | FK | accounts.id |
| `type` | `varchar(40)` | NO |  |  |
| `title` | `varchar(200)` | NO |  |  |
| `body` | `varchar(2000)` | NO |  |  |
| `link` | `varchar(500)` | YES |  |  |
| `source_event_id` | `uuid` | NO |  |  |
| `email_status` | `varchar(16)` | NO |  |  |
| `email_sent_at` | `timestamptz` | YES |  |  |
| `read_at` | `timestamptz` | YES |  |  |
| `created_at` | `timestamptz` | NO |  |  |

Constraints: UK (source_event_id,account_id,type); Retention 180 days.

## Relationships

| Child | Column | Parent | Enforcement |
|---|---|---|---|
| `audit_logs` | `actor_id` | `accounts` | Physical FK |
| `subjects` | `manager_id` | `accounts` | Physical FK |
| `course_classes` | `subject_id` | `subjects` | Physical FK |
| `course_classes` | `teacher_id` | `accounts` | Physical FK |
| `enrollments` | `class_id` | `course_classes` | Physical FK |
| `enrollments` | `account_id` | `accounts` | Physical FK |
| `modules` | `subject_id` | `subjects` | Physical FK |
| `lessons` | `module_id` | `modules` | Physical FK |
| `lessons` | `class_id` | `course_classes` | Physical FK |
| `announcements` | `class_id` | `course_classes` | Physical FK |
| `announcements` | `author_id` | `accounts` | Physical FK |
| `announcement_comments` | `announcement_id` | `announcements` | Physical FK |
| `announcement_comments` | `account_id` | `accounts` | Physical FK |
| `payments` | `account_id` | `accounts` | Physical FK |
| `payments` | `package_id` | `credit_packages` | Physical FK |
| `assignments` | `class_id` | `course_classes` | Physical FK |
| `assignments` | `subject_id` | `subjects` | Physical FK |
| `assignment_questions` | `assignment_id` | `assignments` | Physical FK |
| `assignment_questions` | `question_id` | `questions` | Physical FK |
| `attempts` | `assignment_id` | `assignments` | Physical FK |
| `attempts` | `account_id` | `accounts` | Physical FK |
| `student_groups` | `class_id` | `course_classes` | Physical FK |
| `group_members` | `group_id` | `student_groups` | Physical FK |
| `group_members` | `account_id` | `accounts` | Physical FK |
| `leader_change_requests` | `group_id` | `student_groups` | Physical FK |
| `leader_change_requests` | `requester_id` | `accounts` | Physical FK |
| `leader_change_requests` | `nominee_id` | `accounts` | Physical FK |
| `ai_suggestions` | `ai_service_id` | `ai_services` | Physical FK |
| `ai_suggestions` | `requested_by` | `accounts` | Physical FK |
| `group_documents` | `group_id` | `student_groups` | Physical FK |
| `group_documents` | `assignment_id` | `assignments` | Physical FK |
| `evaluations` | `attempt_id` | `attempts` | Physical FK |
| `evaluations` | `group_document_id` | `group_documents` | Physical FK |
| `evaluations` | `account_id` | `accounts` | Physical FK |
| `notifications` | `account_id` | `accounts` | Physical FK |
| `assignments` | `source_assignment_id` | `assignments` | Logical only; no FK |

## Important integrity rules

- An assignment belongs to exactly one class or one subject template; never both.
- A group assignment is always `GRADED`. Published assignment content is immutable; a change creates a new row/version.
- An attempt preserves assignment/question/rubric snapshots and only one `IN_PROGRESS` attempt may exist per student and assignment.
- A group has exactly one leader. Group documents have only `IN_PROGRESS` and `CLOSED`; there is no `REVIEW` state.
- An evaluation points to either an attempt or a group document. `MEMBER` rows additionally identify the receiving account.
- Audit logs and AI suggestions are append-oriented; database privileges prevent destructive changes where specified.

## Text alternative to the diagram

Accounts connect to academic scope through subjects, classes and enrollments. Subjects own modules, while lessons may be shared by a subject or restricted to a class. Classes own announcements, class groups and class assignments. Assignments connect to versioned questions, individual attempts or group documents. Evaluations grade attempts or group documents and may identify an individual group member. Payments add account credit, AI suggestions consume it, and notifications project domain events for one account.
