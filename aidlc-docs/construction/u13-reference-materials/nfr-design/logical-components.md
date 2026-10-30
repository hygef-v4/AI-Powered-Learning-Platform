# U13 Reference Materials — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [materials/controller](../../../../backend/src/main/java/edu/aiplatform/materials/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [materials/service](../../../../backend/src/main/java/edu/aiplatform/materials/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [materials/repository](../../../../backend/src/main/java/edu/aiplatform/materials/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [materials/model](../../../../backend/src/main/java/edu/aiplatform/materials/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [materials/integration](../../../../backend/src/main/java/edu/aiplatform/materials/integration) | Adapter hạ tầng/provider, listener và sender. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
