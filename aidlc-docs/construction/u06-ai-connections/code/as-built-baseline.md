# U06 AI Connections — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

MCP credential dành ACTIVE Student/Teacher/Subject Manager; Admin bị từ chối. Nhiều institutional keys/provider; repository chọn ACTIVE tạo sớm nhất. Owner reveal ACTIVE MCP token khi có ciphertext; Admin reveal key chung không REVOKED (kể cả DISABLED) và ghi audit. Personal key không có readback cho Admin. Staff write tools có proposal/retry/confirmation theo source, không tự finalize/publish.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [ai/controller](../../../../backend/src/main/java/edu/aiplatform/ai/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [ai/service](../../../../backend/src/main/java/edu/aiplatform/ai/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [ai/repository](../../../../backend/src/main/java/edu/aiplatform/ai/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [ai/model](../../../../backend/src/main/java/edu/aiplatform/ai/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [ai/integration](../../../../backend/src/main/java/edu/aiplatform/ai/integration) | Adapter hạ tầng/provider, listener và sender. |
| [ai/config](../../../../backend/src/main/java/edu/aiplatform/ai/config) | Properties, cấu hình bean và topology. |
| [ai/contract](../../../../backend/src/main/java/edu/aiplatform/ai/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [ai](../../../../frontend/src/features/ai) | [api](../../../../frontend/src/features/ai/api) | [model](../../../../frontend/src/features/ai/model) | [pages](../../../../frontend/src/features/ai/pages) | [components](../../../../frontend/src/features/ai/components) | — | — | [tests](../../../../frontend/tests/features/ai) |

Tests: [ai](../../../../backend/src/test/java/edu/aiplatform/ai).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md](../../build-and-test/e2e-u02-u06-u12-2026-10-24.md)
- [aidlc-docs/construction/build-and-test/u06-verification.md](../../build-and-test/u06-verification.md)
