# U15 Admin Dashboard — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `AdminDashboardController` | backend | API mục 3 của domain-entities |
| `AdminDashboardService` | backend | Gọi nguồn song song, ghép kết quả (F1, F2, P1, P2) |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Activity projection — current components

AdminDashboardActivityService reads IAuditStats and IAssignmentQuery through DashboardSourceReader; independent timeout/invalid data gives per-section UNAVAILABLE. AdminDashboardController GET /activity rejects query keys, current ACTIVE Admin only and Cache-Control no-store. Full DTO/window details are in AdminDashboardViews and the OpenAPI activity schema.
