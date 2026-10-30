# U08 Assignment Work — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

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

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
