# U14 Announcements & Notifications — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [announcements/controller](../../../../backend/src/main/java/edu/aiplatform/announcements/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [announcements/service](../../../../backend/src/main/java/edu/aiplatform/announcements/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [announcements/repository](../../../../backend/src/main/java/edu/aiplatform/announcements/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [announcements/model](../../../../backend/src/main/java/edu/aiplatform/announcements/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [announcements/dto](../../../../backend/src/main/java/edu/aiplatform/announcements/dto) | Kiểu dữ liệu request/response riêng. |
| [notifications/controller](../../../../backend/src/main/java/edu/aiplatform/notifications/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [notifications/service](../../../../backend/src/main/java/edu/aiplatform/notifications/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [notifications/repository](../../../../backend/src/main/java/edu/aiplatform/notifications/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [notifications/model](../../../../backend/src/main/java/edu/aiplatform/notifications/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [notifications/integration](../../../../backend/src/main/java/edu/aiplatform/notifications/integration) | Adapter hạ tầng/provider, listener và sender. |
| [notifications/config](../../../../backend/src/main/java/edu/aiplatform/notifications/config) | Properties, cấu hình bean và topology. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
