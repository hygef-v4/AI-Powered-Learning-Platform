# U05 Class & Enrollment — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Source | Trách nhiệm |
|---|---|
| [academics/controller](../../../../backend/src/main/java/edu/aiplatform/academics/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [academics/service](../../../../backend/src/main/java/edu/aiplatform/academics/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [academics/repository](../../../../backend/src/main/java/edu/aiplatform/academics/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [academics/model](../../../../backend/src/main/java/edu/aiplatform/academics/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [academics/dto](../../../../backend/src/main/java/edu/aiplatform/academics/dto) | Kiểu dữ liệu request/response riêng. |
| [academics/contract](../../../../backend/src/main/java/edu/aiplatform/academics/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

U05 không chạy gì trong worker, không có queue, không có secret, không gọi dịch vụ ngoài.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
