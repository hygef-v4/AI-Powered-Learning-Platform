# U09 Assignment Grading — logical-components

## Request-local gradebook assembly — 2026-11-04

`GradebookService` batches account contacts and indexes current evaluations/submission metadata by assignment and learner. It constructs only the requested page's grade rows; export loads the class dataset once rather than repeating full assembly per 100 students. Current-attempt selection, group-member overrides, official-only export fields, authorization and audit transactions remain unchanged. No cross-request cache or schema change: source data and export output still consume memory proportional to the class dataset. See [verification and limits](../../build-and-test/2026-11-04-u05-u09-u15-cleanup-results.md).

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [grading/controller](../../../../backend/src/main/java/edu/aiplatform/grading/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [grading/service](../../../../backend/src/main/java/edu/aiplatform/grading/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [grading/repository](../../../../backend/src/main/java/edu/aiplatform/grading/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [grading/model](../../../../backend/src/main/java/edu/aiplatform/grading/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [grading/dto](../../../../backend/src/main/java/edu/aiplatform/grading/dto) | Kiểu dữ liệu request/response riêng. |
| [grading/contract](../../../../backend/src/main/java/edu/aiplatform/grading/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
