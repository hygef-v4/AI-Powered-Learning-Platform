# U15 Admin Dashboard — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 66. **Primary stories:** US-RPT-002. **Requirements:** FR-024, FR-038.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [admindashboard/controller](../../../../backend/src/main/java/edu/aiplatform/admindashboard/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [admindashboard/service](../../../../backend/src/main/java/edu/aiplatform/admindashboard/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [admindashboard/dto](../../../../backend/src/main/java/edu/aiplatform/admindashboard/dto) | Kiểu dữ liệu request/response riêng. |
| [admindashboard/integration](../../../../backend/src/main/java/edu/aiplatform/admindashboard/integration) | Adapter hạ tầng/provider, listener và sender. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Activity projection — current components

AdminDashboardActivityService reads IAuditStats and IAssignmentQuery through DashboardSourceReader; independent timeout/invalid data gives per-section UNAVAILABLE. AdminDashboardController GET /activity rejects query keys, current ACTIVE Admin only and Cache-Control no-store. Full DTO/window details are in AdminDashboardViews and the OpenAPI activity schema.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
