# U08 Assignment Work — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Lấy phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của U11 cũ. Lượt Quiz thuộc U11 mới, bài nhóm thuộc U12. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `StudentAssignmentController`, `AttemptController`, `PracticeFeedbackController`, `AttemptActivityController` | backend | API mục 5 của domain-entities |
| `StudentAssignmentQueryService` | backend | Danh sách, chi tiết (F1, F2) |
| `AttemptService` | backend | Bắt đầu, lưu, nộp, DOCX (F3–F6, P1, P2) |
| `AutoSubmitScanner` (cài `IScheduledScanner`), `AssignmentRetiredHandler` (cài `IAssignmentLifecycleListener`) | worker, backend | Tự nộp (F7) |
| `SubmissionHistoryService` | backend | Lịch sử nộp (F8) |
| `PracticeFeedbackService`, `PracticeFeedbackResultHandler`, `AssignmentWorkMcpTools` | backend, worker | Phản hồi Practice, tool MCP (F9, F10, P3) |
| `ActivityService` (cài `IActivityWrite`, `IActivityQuery`), `ActivitySummaryCalculator`, `ActivityRetentionScanner`, `ActivitySettingDefinitions` | backend, worker | Dữ liệu hoạt động (F11, F12, P4, P5) |
| `SubmissionQueryService` (cài `ISubmissionQuery`) | backend | Interface cho U09, U14 (F13) |
| Repository `assignment_attempts`, `practice_feedback`, `activity_events` | backend, worker | Chỉ dùng trong package |
