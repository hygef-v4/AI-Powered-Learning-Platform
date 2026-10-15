# Public contracts — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

Đây là chữ ký logical đề xuất cho Inception; không khẳng định contracts/code hiện có đã cập nhật. Các API hiện hành không mâu thuẫn vẫn cần review downstream.

| Owner | Contract responsibilities |
|---|---|
| U01 | authenticate/activate/logout/reset/changePassword; account CRUD/status; authorize(actor, action, target) |
| U02 | recordAudit; queryAudit(admin, filters) append-only |
| U03 | file store/download scoped; jobs enqueue-after-commit/retry; typed Settings version/audit; viewSettings (UC14) / editSetting (UC15) delegate quota via U07 and institutional AI configuration via U13 public ports; runScheduledBackup/listBackupStatus; publishBackupFailure |
| U04 | subject/class CRUD and assignments/enrollment; academicScope check; count summaries |
| U05 | attach/list/download references; announcements; optional Lesson extension |
| U06 | rubric CRUD/validate/lock/snapshot; MCQ bank version/select |
| U07 | reserveTeacherQuota(actor, connection, model, requestRef, estimate); reconcileUsage; markUsageUnknown; listOwnUsage; getTeacherQuotaPolicy/setTeacherQuota(admin, policy, version) (read UC14 / update UC15; owner U07) |
| U08 | createAssignment(actor, submissionMode, diagramEnabled); createQuiz; review/publish/retire/version/copy |
| U09 | document skeleton/editor/import/export; validate diagramEnabled/XML; Quiz questions/config; versioned activityMonitoringPolicy |
| U11 | start/save/submit individual/Quiz; requestFeedback(actor, contentVersion, connectionRef); feedbackHistory; recordActivityEvent/queryScopedActivity (individual/Quiz/group) |
| U12 | groups/membership/leader/allocation |
| U13 | managePersonalConnection; manageSharedConnection(admin); testSyntheticConnection; revoke; requestAi(task, scopedContext, connectionRef); create/revokeMcpCredential; scoped MCP tools |
| U14 | claim/save group section; group context policy; leader submit/version; record own member activity through U11 port |
| U15 | proposal validation; manual evaluation; finalize/publish/history/gradebook; reviewWorkActivity(authorizedTeacher, attempt/member) within UC 42 via U11 ActivityQueryPort |
| U16 | notifications/email/reminders; aggregates/gradebook export via ports; dashboardBackupStatus (UC 66) via U03 BackupStatusPort; idempotent importantAnnouncement/backupFailure email |

MCP tools minimum: read_assignment_context, read_authorized_material, read_own_work, read_authorized_submission (assigned educator only), save_assignment_draft(expectedVersion), record_private_feedback, record_grading_proposal. Names/spec schema cần chốt Construction. Không submit official hoặc publish_grade tool giai đoạn đầu. Token số liệu do agent gửi chỉ reported.

## UC consolidation — 66 active UC

UC 14 View Settings và UC 15 Edit Setting là hai UC riêng, primary U03; bao gồm UI cấu hình quota/API chung. U07 giữ quota service/data; U13 giữ connection/provider/credentials bảo vệ và test synthetic qua public port. UC 76/78/82 (ID cũ) không còn độc lập. UC 66 gộp UC 80 (ID cũ) (U16 dashboard, U03 backup); UC 42 gộp UC 81 (ID cũ) (U15 review, U11/U14 activity). UC 35 soạn Individual, UC 53 soạn Group (U09); Draw.io tùy chọn trong UC 39/56, UC 26 (ID cũ) retired. UC 79 (ID cũ) retired; UC 41 chỉ feedback Assignment Practice đã nộp. 50 MVP + 1 extension story, 15 units; không tự phê duyệt D01–D04 hoặc runtime.

## Current public contract clarifications

- U03 viewSettings(UC14) / editSetting(UC15) authorize Admin; read masked institutional config and delegate mutations/testing/revocation to U13 AiConnectionAdministrationPort. Public contracts allow unavailable adapters without hard dependency cycles.
- AiConnectionAdministrationPort U13: getMaskedInstitutionalConfiguration(adminActor), updateInstitutionalConfiguration(adminActor, provider/model, secretInput, expectedVersion), testInstitutionalConnection(adminActor, syntheticRequest), revokeInstitutionalConnection(adminActor, connectionRef, expectedVersion). No raw/ciphertext key readback; each operation validates and audits at its owner.
- U07 TeacherQuotaPolicyPort provides separate read/update quota semantics. U03 does not write U07/U13 repositories or promise atomic multi-owner edits.
- record_private_feedback requires owned SUBMITTED PRACTICE Assignment and immutable submissionVersion, before saving incoming MCP results. Draft saves remain editing operations; they cannot bypass feedback restrictions.
- U05 material commands support FILE and YOUTUBE_LINK. Links are validated reference URLs/title/version with current subject/class scope; no mandatory captions/download/LLM processing.
