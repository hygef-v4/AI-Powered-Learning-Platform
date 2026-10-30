# U12 Group Assignment — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `ClassGroupController`, `LeaderRequestController`, `MyGroupController`, `GroupDocumentController`, `GroupProgressController` | backend | API mục 5 của domain-entities |
| `ClassGroupService`, `RandomGroupSplitter` | backend | Nhóm, chia ngẫu nhiên, đổi thành viên (F1, F2, F14, P2) |
| `LeaderRequestService` | backend | Yêu cầu đổi trưởng nhóm (F3, F5) |
| `GroupReadinessService` (cài `IGroupAssignmentSupport`), `GroupAssignmentLifecycle` (cài `IAssignmentLifecycleListener`) | backend | Sẵn sàng; mở, ngưng giao (F6, F7, F13) |
| `GroupDocumentService`, `GroupDocCreateJobHandler` | backend, worker | Tạo tài liệu, workspace (F7, F8, P3) |
| `GroupSectionService` | backend | Giao, nhận, làm, Xong, nhả (F9–F11, P1) |
| `GroupSubmissionService`, `GroupAutoSubmitScanner` | backend, worker | Nộp, tự nộp (F12, F13, P5) |
| `GroupWorkQueryService` (cài `IGroupWorkStatus`, `IGroupSubmissionSource`), `GroupMcpTools` | backend | Interface cho U08, U09, U06 (F15) |
| `ISseHub`, `RabbitSseHub` | backend | Realtime (P4) |
| Repository các bảng nhóm, tài liệu | backend, worker | Chỉ dùng trong package |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
