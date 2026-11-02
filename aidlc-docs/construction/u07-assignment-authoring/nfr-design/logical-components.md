# U07 Assignment Authoring — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [assignments/controller](../../../../backend/src/main/java/edu/aiplatform/assignments/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [assignments/service](../../../../backend/src/main/java/edu/aiplatform/assignments/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [assignments/repository](../../../../backend/src/main/java/edu/aiplatform/assignments/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [assignments/model](../../../../backend/src/main/java/edu/aiplatform/assignments/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [assignments/dto](../../../../backend/src/main/java/edu/aiplatform/assignments/dto) | Kiểu dữ liệu request/response riêng. |
| [assignments/contract](../../../../backend/src/main/java/edu/aiplatform/assignments/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
