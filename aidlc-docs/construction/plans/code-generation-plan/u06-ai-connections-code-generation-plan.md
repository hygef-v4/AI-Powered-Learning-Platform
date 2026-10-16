# U06 AI Connections — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U06 AI Connections thuộc Iter 2, người code: Người 5; UC 31–33 và phần API chung, quota của UC 14–15. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037.

Thiết kế: [business-logic-model](../../u06-ai-connections/functional-design/business-logic-model.md), [business-rules](../../u06-ai-connections/functional-design/business-rules.md), [domain-entities](../../u06-ai-connections/functional-design/domain-entities.md), [frontend-components](../../u06-ai-connections/functional-design/frontend-components.md), [NFR](../../u06-ai-connections/nfr-requirements/nfr-requirements.md), [NFR design](../../u06-ai-connections/nfr-design/nfr-design-patterns.md), [infrastructure](../../u06-ai-connections/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/ai/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../ai/contract/` | interface `IAiTask`, `IAiTaskResultHandler`, `IMcpToolProvider`, `IReferenceMaterial`; kiểu dữ liệu `AiPrompt`, `AiProvider`, `AiSource`, `AiTaskFailure`, `AiTaskRequest`, `AiTaskResult`, `AiTaskState`, `AiTaskView`, `AiUsageSummary`, `ApplyOutcome`, `MaterialText`, `McpToolDefinition`, `McpToolKind`, `McpToolResult`, `SelectableMaterial`, `UsageMeasurement` |
| `backend/.../ai/contract/stub/AiContractStubs` | Bản tạm trả dữ liệu mẫu cho `IAiTask`, `IReferenceMaterial` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/ai/` | Chỉ có `index.ts` trống |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/ai/V20260925_1200__create_ai_tables.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `AiConnection`, `AiTask`, `AiUsageRecord`, `AiQuotaPolicy`, `AiTeacherQuota`, `AiQuotaWindow`, `McpCredential` và enum; kiểm chuyển trạng thái; repository chỉ dùng trong package.
- [ ] **Bước 3 — Key và provider:** `KeyCipher` (AES-256-GCM, `keyVersion`), `ProviderCatalog` (model cho phép, `maxOutputTokens`), `GeminiAdapter`, `OpenAiAdapter`, `ClaudeAdapter` (structured output, usage chuẩn hóa, phân loại lỗi có tính token và không tính token, hết giờ).
- [ ] **Bước 4 — Kết nối:** `AiConnectionService`, `ConnectionTestService` (F1–F3, F8); audit theo BR-U06-52.
- [ ] **Bước 5 — Quota:** `QuotaPolicyService` (F9), `QuotaReservationService` (giữ chỗ, quyết toán, giải phóng, tính đã dùng theo P1), `UsageSummaryService` (F10).
- [ ] **Bước 6 — Task:** `IAiTask`, `AiTaskService`, `AiSourceService` (F11), `IAiTaskResultHandler`, `AiTaskResultHandlerRegistry`, `AiTaskJobHandler` (F12, P2, P3), `AiTaskPendingSweeper`, `AiTaskTimeoutScanner` (F13); thêm `AI_TASK` vào routing job của U03 nếu chưa có; interface `IReferenceMaterial` với adapter rỗng tới khi U13 có.
- [ ] **Bước 7 — Usage:** `AiUsageService` (ghi cho task, kiểm tra kết nối, MCP; đọc F7).
- [ ] **Bước 8 — MCP:** `McpCredentialService` (F4, F5), `McpServerConfig` (Streamable HTTP `/mcp`), `McpAuthenticationFilter`, `IMcpToolProvider`, `McpToolRegistry`, `McpRateLimiter`; ghi usage và audit (F6).
- [ ] **Bước 9 — Settings:** `AiConnectionsSection`, `TeacherQuotaSection` cài `ISettingsSectionProvider` (F14).
- [ ] **Bước 10 — API:** controller theo domain-entities mục 7; `contracts/openapi/ai.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 11 — Frontend:** `AiConnectionPage` (`PersonalApiTab`, `ApiKeyDialog`, `ApiSetupGuide`, `McpAgentTab`, `McpCredentialDialog`, `McpSetupGuide`), `AiUsageHistoryPage`, `AiSourcePicker`, `AiTaskStatus`, `AiConnectionsPanel`, `TeacherQuotaPanel` (đăng ký vào `settingsSectionRegistry`), mục AI Connection qua `AccountMenuEntry`.
- [ ] **Bước 12 — Hạ tầng:** biến `U06_AI_KEY_ENCRYPTION_KEY`, cấu hình model cho phép, Nginx `/mcp`; cập nhật hướng dẫn chạy local.
- [ ] **Bước 13 — Hướng dẫn kết nối:** `docs/guides/ai-api-keys.md` (Gemini, OpenAI, Claude) và `docs/guides/mcp-clients.md` (chỉ ứng dụng agent đã thử, ghi ngày và phiên bản).
- [ ] **Bước 14 — Unit test (người code viết):** mã hóa và che key; model ngoài danh sách bị từ chối; Student, Admin không dùng được API chung; giữ chỗ vượt mức ngày, trần; quyết toán token thật, giải phóng khi provider từ chối, tính đã dùng khi không rõ; `requestRef` lặp và khác dấu vân tay; message đến lại sau mốc gửi thành `UNKNOWN`; kết quả sai schema; handler trả `STALE`; credential hết hạn, thu hồi, tài khoản không `ACTIVE`, vẫn dùng được sau khi đổi mật khẩu; tool ghi với `READ_ONLY`; tham số quá cỡ; usage MCP không ghi 0; adapter với HTTP server giả; component frontend. Integration test do tester viết.
- [ ] **Bước 15 — Review:** đối chiếu UC 31–33, phần UC 14–15, story, BR-U06 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- 20 yêu cầu đồng thời của một giảng viên không vượt mức ngày và trần toàn hệ thống.
- Worker nhận lại message sau khi đã gửi provider: không gọi lần hai, task `UNKNOWN`, quota tính đã dùng.
- Hết quota chung không tự chuyển sang API cá nhân; Student không dùng được API chung qua web hay MCP.
- Thu hồi credential MCP hoặc đổi mật khẩu rồi gọi tool ngay: bị từ chối.
- Key không xuất hiện trong response, log, audit.
- Phân quyền 4 role trên mọi endpoint, gồm dùng ID kết nối, task, credential của người khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
