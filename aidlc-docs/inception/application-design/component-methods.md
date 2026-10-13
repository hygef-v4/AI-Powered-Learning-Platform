# Public contracts — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

Đây là chữ ký logical đề xuất cho Inception; không khẳng định contracts/code hiện có đã cập nhật. Các API hiện hành không mâu thuẫn vẫn cần review downstream.

| Owner | Contract responsibilities |
|---|---|
| U01 | authenticate/activate/logout/reset/changePassword; account CRUD/status; authorize(actor, action, target) |
| U02 | recordAudit; queryAudit(admin, filters) append-only |
| U03 | file store/download scoped; jobs enqueue-after-commit/retry; typed Settings version/audit; manageSystemSettings (UC 70) delegates quota reads/writes via U07 port; runScheduledBackup/listBackupStatus; publishBackupFailure |
| U04 | subject/class CRUD and assignments/enrollment; academicScope check; count summaries |
| U05 | attach/list/download references; announcements; optional Lesson extension |
| U06 | rubric CRUD/validate/lock/snapshot; MCQ bank version/select |
| U07 | reserveTeacherQuota(actor, connection, model, requestRef, estimate); reconcileUsage; markUsageUnknown; listOwnUsage; getTeacherQuotaPolicy/setTeacherQuota(admin, policy, version) (support UC 70; owner U07) |
| U08 | createAssignment(actor, submissionMode, diagramEnabled); createQuiz; review/publish/retire/version/copy |
| U09 | document skeleton/editor/import/export; validate diagramEnabled/XML; Quiz questions/config; versioned activityMonitoringPolicy |
| U11 | start/save/submit individual/Quiz; requestFeedback(actor, contentVersion, connectionRef); feedbackHistory; recordActivityEvent/queryScopedActivity (individual/Quiz/group) |
| U12 | groups/membership/leader/allocation |
| U13 | managePersonalConnection; manageSharedConnection(admin); testSyntheticConnection; revoke; requestAi(task, scopedContext, connectionRef); create/revokeMcpCredential; scoped MCP tools |
| U14 | claim/save group section; group context policy; leader submit/version; record own member activity through U11 port |
| U15 | proposal validation; manual evaluation; finalize/publish/history/gradebook; reviewWorkActivity(authorizedTeacher, attempt/member) within UC 37 via U11 ActivityQueryPort |
| U16 | notifications/email/reminders; aggregates/gradebook export via ports; dashboardBackupStatus (UC 58) via U03 BackupStatusPort; idempotent importantAnnouncement/backupFailure email |

MCP tools minimum: read_assignment_context, read_authorized_material, read_own_work, read_authorized_submission (assigned educator only), save_assignment_draft(expectedVersion), record_private_feedback, record_grading_proposal. Names/spec schema cần chốt Construction. Không submit official hoặc publish_grade tool giai đoạn đầu. Token số liệu do agent gửi chỉ reported.

## UC consolidation — 66 active UC

UC 70 Manage System Settings gộp 71/78: U03 primary; U07 giữ quota service/data và US-AIC-003. UC 58 gộp 80: U16 primary dashboard; U03 giữ backup service/data và US-OPS-001. UC 37 gộp 81: U15 primary review; U11/U14 giữ activity capture. UC 76 giữ riêng credentials/provider. Story count vẫn 50 MVP + 1 extension, 15 units; không bỏ chức năng hoặc acceptance vì gộp UC. Quiz Practice activity chỉ đọc trong scope/policy cho phép, không tự thêm Gradebook.
