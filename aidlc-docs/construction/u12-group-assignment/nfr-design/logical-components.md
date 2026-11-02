# U12 Group Assignment — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [groups/controller](../../../../backend/src/main/java/edu/aiplatform/groups/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [groups/service](../../../../backend/src/main/java/edu/aiplatform/groups/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [groups/repository](../../../../backend/src/main/java/edu/aiplatform/groups/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [groups/model](../../../../backend/src/main/java/edu/aiplatform/groups/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [groupdocs/controller](../../../../backend/src/main/java/edu/aiplatform/groupdocs/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [groupdocs/service](../../../../backend/src/main/java/edu/aiplatform/groupdocs/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [groupdocs/repository](../../../../backend/src/main/java/edu/aiplatform/groupdocs/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [groupdocs/model](../../../../backend/src/main/java/edu/aiplatform/groupdocs/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [groupdocs/integration](../../../../backend/src/main/java/edu/aiplatform/groupdocs/integration) | Adapter hạ tầng/provider, listener và sender. |
| [groupdocs/contract](../../../../backend/src/main/java/edu/aiplatform/groupdocs/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
