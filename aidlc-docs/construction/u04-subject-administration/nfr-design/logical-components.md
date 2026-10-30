# U04 Subject Administration — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Source | Trách nhiệm |
|---|---|
| [subjects/controller](../../../../backend/src/main/java/edu/aiplatform/subjects/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [subjects/service](../../../../backend/src/main/java/edu/aiplatform/subjects/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [subjects/repository](../../../../backend/src/main/java/edu/aiplatform/subjects/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [subjects/model](../../../../backend/src/main/java/edu/aiplatform/subjects/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [subjects/dto](../../../../backend/src/main/java/edu/aiplatform/subjects/dto) | Kiểu dữ liệu request/response riêng. |
| [subjects/contract](../../../../backend/src/main/java/edu/aiplatform/subjects/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

U04 không chạy gì trong worker, không có queue, không có secret, không gọi dịch vụ ngoài.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
