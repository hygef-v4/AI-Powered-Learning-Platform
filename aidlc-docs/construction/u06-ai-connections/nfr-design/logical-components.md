# U06 AI Connections — logical-components

> Current structure: parent HEAD 5a76e428 + working-tree refactor, 2026-10-30. Unit ownership unchanged; runtime/provider acceptance remains separate.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Source | Trách nhiệm |
|---|---|
| [ai/controller](../../../../backend/src/main/java/edu/aiplatform/ai/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [ai/service](../../../../backend/src/main/java/edu/aiplatform/ai/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [ai/repository](../../../../backend/src/main/java/edu/aiplatform/ai/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [ai/model](../../../../backend/src/main/java/edu/aiplatform/ai/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [ai/integration](../../../../backend/src/main/java/edu/aiplatform/ai/integration) | Adapter hạ tầng/provider, listener và sender. |
| [ai/config](../../../../backend/src/main/java/edu/aiplatform/ai/config) | Properties, cấu hình bean và topology. |
| [ai/contract](../../../../backend/src/main/java/edu/aiplatform/ai/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Ghi nhận trước refactor chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.

## Structure refactor — 2026-10-30

Đường dẫn hiện hành theo bảng trên; [unit source inventory](../code/as-built-baseline.md). Public contract, quyền, transaction và cấu hình chạy giữ nguyên; [automated verification and limits](../../build-and-test/2026-10-30-package-moves-only-results.md).
