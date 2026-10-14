# Construction — Current contract 2026-10-13

**66 active UC; 50 MVP stories + 1 extension; 15 logical units; 39 FR IDs.** Nguồn [Inception requirements](../inception/requirements/requirements.md), [catalog](../inception/requirements/use-cases.md), [English table](../../docs/usecase_table.md), [story map](../inception/application-design/unit-of-work-story-map.md), [decisions](../inception/requirements/revision-decisions-2026-10-13.md).

Revision tài liệu theo yêu cầu người dùng; không implementation/approval/runtime evidence. [Snapshot cũ](historical/baseline-before-66-uc-2026-10-13/README.md) giữ checklist/phê duyệt trước, không source code mới. Ngày 2026-10-14 15 unit được chia lại thành 3 iteration; các folder Construction giữ tên cũ, đối chiếu unit mới ở [unit-of-work.md](../inception/application-design/unit-of-work.md). Các ID unit nhắc trong phần còn lại của tài liệu này là ID cũ cho tới khi được rà lại.

## Ownership

| Iter | Unit | Tên | Primary UC |
|---|---|---|---|
| 1 | U01 | Account & Access | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12 |
| 1 | U02 | Audit | 13 |
| 1 | U03 | File, Job, Settings & Backup | 14, 15 |
| 1 | U04 | Subject Administration | 16, 17, 18, 19 |
| 1 | U05 | Class & Enrollment | 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 |
| 2 | U06 | AI Connections & MCP | 31, 32, 33 |
| 2 | U07 | Assignment Authoring | 34, 35, 36 |
| 2 | U08 | Assignment Work | 37, 38, 39, 40, 41 |
| 2 | U09 | Assignment Grading & Gradebook | 42, 43, 44, 45 |
| 2 | U10 | Quiz Authoring & Bank | 46, 47, 48 |
| 3 | U11 | Quiz Practice | 49, 50, 51, 52 |
| 3 | U12 | Group Assignment | 53, 54, 55, 56, 57 |
| 3 | U13 | Reference Materials | 58, 59, 60, 61, 62 |
| 3 | U14 | Announcements & Notifications | 63, 64, 65 |
| 3 | U15 | Admin Dashboard | 66 |

Iteration, phụ thuộc và phân công: [unit-of-work.md](../inception/application-design/unit-of-work.md).

## Authority và scope

### UC 57 — Create/Update/Delete Class Group

Giảng viên được giao dạy lớp (Teacher hoặc Subject Manager trong vai trò giảng viên lớp) tạo nhóm thủ công/ngẫu nhiên, sửa/xóa nhóm theo điều kiện, thêm/chuyển/gỡ thành viên, chỉ định trưởng nhóm và duyệt/từ chối yêu cầu đổi trưởng nhóm. Điểm truy cập: Class Dashboard → Teacher Class Detail (UC 23) → Students → Groups; UC 23 cung cấp màn lớp, UC 57 sở hữu thao tác nhóm. UC 54 là sinh viên xem nhóm; UC 55 là thành viên gửi/hủy yêu cầu; UC 53/56 là soạn/làm bài nhóm. Không thêm trang riêng hoặc chuyển quyền quản lý nhóm cho Admin hay Chủ nhiệm môn không được giao dạy lớp. Xóa chỉ khi nhóm chưa có tài liệu nhóm hoặc bản nộp; giữ các quy tắc và audit hiện hành.

| Actor / scope | Quyền |
|---|---|
| R1 own data | Profile, connection cá nhân, usage/feedback/notification của chính actor |
| R2 subject manager | Quản lý môn/material/bank/bài cá nhân cấp môn; không tự quyền chấm lớp |
| R3/R4 assigned teacher | Dạy/chấm/review/activity/export đúng lớp; shared quota nếu educator ACTIVE và scope hợp lệ |
| R5 student | Enrollment/group membership current, own editable work; personal API/MCP, không shared quota |
| Admin | Accounts/subjects, UC14 Settings/quota orchestration, UC14/15 shared credential/test synthetic through U13, UC66 counts/backup metadata, UC13 audit; không học vụ |

Web/API/MCP đều current auth/resource/state/version; keys/client không mở rộng quyền; scope revoked deny request tiếp theo. Cookie-session state mutation phải có CSRF defense; MCP auth không thay provider credentials.

## Core invariants

- Chỉ ASSIGNMENT/QUIZ; submissionMode INDIVIDUAL/GROUP và diagramEnabled độc lập. Bài nhóm chỉ lớp, tạm GRADED; Quiz tạm PRACTICE (D01/D02).
- Reference files và educational YouTube links là MVP; Lesson processing/transcription/summary/advanced RAG là extension; không lesson/indexing prerequisite.
- Quota không mua bán/wallet/payment; shared educator only, personal API hoặc external agent cho Student. Personal fallback cần user chọn.
- Gemini/OpenAI/Claude direct provider adapters; MCP authenticated inbound domain tools, không mặc định proxy gọi LLM. External usage reported/unavailable, không trừ shared grant.
- AI drafts/feedback/proposals validate snapshot/version; official grades/publication do người có quyền. No initial MCP submit/publish_grade tool.
- Activity opt-in snapshot/default off, thông báo trước; no data khác zero; không auto verdict/penalty. Group activity per member U11 owner, U14 port.
- Backup encrypted DB + independent files/manifest private Drive, SUCCESS only verified, isolated restore evidence; UC66 status, UC14 view / UC15 config, không restore UI.
- UC14 View Settings và UC15 Edit Setting giữ riêng; UC76/78/82 (ID cũ) vào UC14/15, UC80 (ID cũ) vào UC66, UC81 (ID cũ) vào UC42; UC26 (ID cũ) vào UC39/56; UC79 (ID cũ) retired; UC35 Individual và UC53 Group authoring. Backup/quota/activity stories vẫn giữ chủ dữ liệu; shared UI UC không chuyển ownership.

## Contracts xuyên unit

| Port | Owner | Consumers / semantics |
|---|---|---|
| AuthorizationPort / AcademicScopePort | U01 / U04 | Current actor/object/scope; no caller actor spoofing |
| AuditPort | U02 | All owner transactions; sanitize/append-only |
| FileStoragePort / JobPort / OutboxPort | U03 | Owner-scoped private files, durable event/request refs |
| BackupStatusPort | U03 | U16 metadata only, unavailable/lastKnown not false SUCCESS |
| TeacherQuotaPolicyPort | U07 | U03 UC14 read / UC15 mutation validate Admin; versioned quota policy transaction owned U07 |
| QuotaReservationPort / UsagePort | U07 | U13 institutional calls; UNKNOWN retains uncertainty; provenance and request fingerprint |
| AiConnectionPort / AiProviderPort | U13 | Connection owner authorization, protected secrets, normalized direct provider output/usage |
| DocumentModelPort / AssessmentPolicyPort | U09 / U08 | Locked snapshots/config/version and current editability |
| GroupMembershipPort / GroupReadinessPort | U12 | U14/U08/U11 current enrolled member/leader |
| ActivityWritePort / ActivityQueryPort | U11 | U14 member events, U15 teacher review, per-work/policy/scope validation |
| SubmissionPort / PracticeResultPort | U11/U14 / U15 | Immutable versions/receipts; Quiz Practice outside Gradebook |
| GradebookProjectionPort | U15 | U16 teacher-only export; no official Practice data |
| MailPort | Shared U01/U16 | Brevo demo or Mailpit local, bounded failures and minimal data |

Public DTOs không expose repository entities/credential ciphertext; contracts/types/schema implementation vẫn cần diff trước code. U03 nền không hard-depend U07/U16: define neutral contracts và deny/unavailable khi adapter chưa có, tránh dependency cycle.

## Events

Outbox record cùng domain transaction, IDs/version/time/refs tối thiểu: assessment.opened/retired, submission.created, grade.published, class.announcement-posted(important/sendEmail), backup.failed/completed, enrollment.changed, group.membership-changed. Consumer checks current scope, dedup; email retry không rollback producer. Durable grade/backup/usage facts không chỉ Redis.

## Open decisions / migrations

D01–D04 còn mở; backup schedule/retention và activity retention cần cấu hình trước enabled. Client MCP compatibility/schema/chosen transport được nghiệm thu theo client, không hứa mọi agent tự kết nối. Quota estimate/unknown semantics không promise exactly-once external billing.

Mapping dữ liệu cũ: Text/Diagram/Group thành ASSIGNMENT + submissionMode/diagramEnabled; MCQ thành QUIZ, Code data chỉ archive/export/read-only historical, không silently convert. Retired payment/credit data lưu riêng không đổi thành quota balance. Không drop trước backup/restore và migration review. IDs/snapshots/grade history phải giữ, cả Essay answer theo câu phải có adapter thành document parts kèm rubric mapping, không thay receipt cũ.

## Functional requirement coverage

| FR active | Owners/support |
|---|---|
| FR-001 | U01 |
| FR-002 | U01/U04; mọi unit |
| FR-003 | U04 |
| FR-004 | U05/U03 |
| FR-005 | U04/U05 |
| FR-006 | U09/U13 |
| FR-007 | U08 |
| FR-008 | U15/U06/U13 |
| FR-009 | U15/U16 |
| FR-011 | U01/U16 |
| FR-012 | U13 |
| FR-013 | U03 |
| FR-014 | U02; mọi owner audit |
| FR-015 | U01 |
| FR-016 | U06 |
| FR-017 | U08/U09 |
| FR-018 | U11/U14/U03 |
| FR-019 | U11/U16 |
| FR-020 | U15 |
| FR-021 | U07/U13 |
| FR-023 | U05/U16 |
| FR-024 | U16 |
| FR-025 | U12/U14 |
| FR-026 | U14 |
| FR-027 | U04/U08 |
| FR-028 | U08 |
| FR-030 | U11/U14/U13 |
| FR-033 | U03/U07/U13 |
| FR-034 | U07 |
| FR-035 | U13 |
| FR-036 | U07/U13 |
| FR-037 | U13 |
| FR-038 | U03/U16 |
| FR-039 | U09/U11/U14/U15 |

FR-010/022/029/031/032 retired theo Inception, không triển khai: giữ IDs và lịch sử, không tái sử dụng. 34 active FR + 5 retired = 39 stable IDs. Coverage chỉ thiết kế, không runtime evidence.

## Critical DTO/command shapes

- QuotaPolicyCommand(adminActor, educatorId, provider/model, hourLimit/dayLimit, expectedVersion) → SavedPolicyVersion hoặc VALIDATION/CONFLICT/UNAVAILABLE. U03 UC14 không trả thành công nếu U07 chưa xử lý; không multi-owner atomic update.
- ReserveCommand(actor, connectionRef, requestRef, targetRef/contentVersion, model, estimatedBound) → reservationRef/currentState/replay. UNKNOWN không tự release hoặc dispatch lại; provider overage được ghi đúng, không guarantee cứng ngân sách billing.
- BackupStatusQuery(adminActor, page/filter) → status/latestGood/snapshotTime/bytes/safeError/observedAt/availability, không archiveRef download/secrets. Dashboard port lỗi khác backup FAILED.
- ActivityEventCommand(authenticatedActor, workKind/workId, sessionId, clientEventId, sequence, kind, clientTime) → accepted/deduplicated/rejected; actor/class/policy derived server. Query(assignedTeacher, workRef, memberId) → policyState/availability/intervals/count/knownDuration/unknownGaps; không fraud score/grade mutation.
- AiTaskCommand(actor, sourceConnectionRef, taskType, targetRef, inputVersion, requestRef) → taskState/provenance. Draft/proposal apply expected targetVersion và current permissions; output chưa validated không tự applied.
- MCP read_own_work phục vụ chủ bài; read_authorized_submission là supporting tool cho assigned educator đọc snapshot chấm bài đúng lớp, không hidden Quiz keys. Tools write expectedVersion, tool names/schema/transport có integration evidence trước public enable.

## UC table synchronization — feedback, authoring and Settings

- UC35 Create/Update/Delete Individual Assignment and UC53 Create/Update/Delete Group Assignment are distinct authoring entries using one Assignment document model. diagramEnabled is independent in both; UC39/56 include Draw.io XML/preview editing without a separate UC26 (ID cũ).
- UC41 Student feedback only on owned SUBMITTED PRACTICE snapshots. Deny draft/IN_PROGRESS/Graded and unauthorized requests or MCP writes; no draft-feedback policy remains. Teacher UC43 Grade Submission With AI remains a reviewed proposal workflow.
- UC14 masked read and UC15 validated update/test/revoke contain institutional API configuration; U03 UI delegates to U13 protected connection records. Primary ownership does not move U13 credentials to U03.
- AiConnectionAdministrationPort (U13) exposes masked config, versioned update/revoke and bounded synthetic test to U03; unavailable adapters do not report success. U07 quota and U13 credentials have separate owner transactions/audits.
- Material references include private FILE and YOUTUBE_LINK with title/url/scope/version. Opening a public YouTube link does not give ALAP control over external video visibility. Transcript/summary/RAG processing is not required.
