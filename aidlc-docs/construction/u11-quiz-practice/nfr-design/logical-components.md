# U11 Quiz Practice — logical-components — chuẩn JDBC

> Quyết định persistence 2026-11-06: [Spring JDBC cho toàn dự án](../../persistence-conventions.md). Thay lựa chọn ORM trong thiết kế cũ; snapshot/evidence có ngày là lịch sử. Code còn JPA được ghi là khoảng cách migration.

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [quizpractice/controller](../../../../backend/src/main/java/edu/aiplatform/quizpractice/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [quizpractice/service](../../../../backend/src/main/java/edu/aiplatform/quizpractice/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [quizpractice/repository](../../../../backend/src/main/java/edu/aiplatform/quizpractice/repository) | Repository interface/implementation, SQL và persistence mapping; JDBC là chuẩn đích toàn dự án. |
| [quizpractice/model](../../../../backend/src/main/java/edu/aiplatform/quizpractice/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [quizpractice/dto](../../../../backend/src/main/java/edu/aiplatform/quizpractice/dto) | Kiểu dữ liệu request/response riêng. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
