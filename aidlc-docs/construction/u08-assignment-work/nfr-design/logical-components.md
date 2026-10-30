# U08 Assignment Work — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [assignmentwork/controller](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [assignmentwork/service](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [assignmentwork/repository](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [assignmentwork/model](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [assignmentwork/dto](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/dto) | Kiểu dữ liệu request/response riêng. |
| [assignmentwork/contract](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
