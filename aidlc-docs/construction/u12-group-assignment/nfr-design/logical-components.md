# U12 Group Assignment — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `ClassGroupController`, `LeaderRequestController`, `MyGroupController`, `GroupDocumentController`, `GroupProgressController` | backend | API mục 5 của domain-entities |
| `ClassGroupService`, `RandomGroupSplitter` | backend | Nhóm, chia ngẫu nhiên, đổi thành viên (F1, F2, F14, P2) |
| `LeaderRequestService` | backend | Yêu cầu đổi trưởng nhóm (F3, F5) |
| `GroupReadinessService` (cài `GroupAssignmentSupport`), `GroupAssignmentLifecycle` (cài `AssignmentLifecycleListener`) | backend | Sẵn sàng; mở, ngưng giao (F6, F7, F13) |
| `GroupDocumentService`, `GroupDocCreateJobHandler` | backend, worker | Tạo tài liệu, workspace (F7, F8, P3) |
| `GroupSectionService` | backend | Giao, nhận, làm, Xong, nhả (F9–F11, P1) |
| `GroupSubmissionService`, `GroupAutoSubmitScanner` | backend, worker | Nộp, tự nộp (F12, F13, P5) |
| `GroupWorkQueryService` (cài `GroupWorkStatusPort`, `GroupSubmissionSource`), `GroupMcpTools` | backend | Port cho U08, U09, U06 (F15) |
| `SseHub`, `RabbitSseHub` | backend | Realtime (P4) |
| Repository các bảng nhóm, tài liệu | backend, worker | Chỉ dùng trong package |
