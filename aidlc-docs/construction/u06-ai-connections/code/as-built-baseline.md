# U06 AI Connections — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 31–33; FR-012, FR-021, FR-034, FR-035, FR-036, FR-037.

MCP credential dành ACTIVE Student/Teacher/Subject Manager; Admin bị từ chối. Nhiều institutional keys/provider; repository chọn ACTIVE tạo sớm nhất. Owner reveal ACTIVE MCP token khi có ciphertext; Admin reveal key chung không REVOKED (kể cả DISABLED) và ghi audit. Personal key không có readback cho Admin. Staff write tools có proposal/retry/confirmation theo source, không tự finalize/publish.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/ai/application/AiConnectionService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/AiConnectionService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiSourceService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/AiSourceService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiTaskService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/AiTaskService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/AiUsageService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/AiUsageService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/ConnectionTestService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/ConnectionTestService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/McpCredentialService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/McpCredentialService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/McpWriteRequestService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/McpWriteRequestService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/QuotaPolicyService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/QuotaPolicyService.java)
- [backend/src/main/java/edu/aiplatform/ai/application/QuotaReservationService.java](../../../../backend/src/main/java/edu/aiplatform/ai/application/QuotaReservationService.java)
- [backend/src/main/java/edu/aiplatform/ai/web/AiAdminController.java](../../../../backend/src/main/java/edu/aiplatform/ai/web/AiAdminController.java)
- [backend/src/main/java/edu/aiplatform/ai/web/AiConnectionController.java](../../../../backend/src/main/java/edu/aiplatform/ai/web/AiConnectionController.java)
- [backend/src/main/java/edu/aiplatform/ai/web/AiUsageController.java](../../../../backend/src/main/java/edu/aiplatform/ai/web/AiUsageController.java)
- [backend/src/main/java/edu/aiplatform/ai/web/McpController.java](../../../../backend/src/main/java/edu/aiplatform/ai/web/McpController.java)
- [backend/src/main/java/edu/aiplatform/ai/web/McpCredentialController.java](../../../../backend/src/main/java/edu/aiplatform/ai/web/McpCredentialController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md](../../build-and-test/e2e-u02-u06-u12-2026-10-24.md)
- [aidlc-docs/construction/build-and-test/u06-verification.md](../../build-and-test/u06-verification.md)
