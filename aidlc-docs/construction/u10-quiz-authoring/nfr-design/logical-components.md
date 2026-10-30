# U10 Quiz Authoring — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [quizzes/controller](../../../../backend/src/main/java/edu/aiplatform/quizzes/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [quizzes/service](../../../../backend/src/main/java/edu/aiplatform/quizzes/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [quizzes/repository](../../../../backend/src/main/java/edu/aiplatform/quizzes/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [quizzes/model](../../../../backend/src/main/java/edu/aiplatform/quizzes/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [quizzes/contract](../../../../backend/src/main/java/edu/aiplatform/quizzes/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
