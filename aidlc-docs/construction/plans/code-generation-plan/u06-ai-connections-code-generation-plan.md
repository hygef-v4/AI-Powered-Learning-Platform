# U06 AI Connections — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U06 AI Connections thuộc Iter 2; UC 31–33 và phần API chung, quota của UC 14–15. Chi tiết hóa ngày 2026-10-21 trước khi code (Part 1). Jira ngày 2026-10-20 giao U06 cho Võ Minh Tài (Người 2), đổi với U09 sang Khuất Quang Hưng.

**Primary UC:** 31, 32, 33; hỗ trợ 14, 15. **Primary stories:** US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003. **Requirements:** FR-012, FR-021, FR-033, FR-034, FR-035, FR-036, FR-037. **Jira:** epic KAN-29; story KAN-34 (UC 31), KAN-35 (UC 32), KAN-37 (UC 33).

Thiết kế: [business-logic-model](../../u06-ai-connections/functional-design/business-logic-model.md), [business-rules](../../u06-ai-connections/functional-design/business-rules.md), [domain-entities](../../u06-ai-connections/functional-design/domain-entities.md), [frontend-components](../../u06-ai-connections/functional-design/frontend-components.md), [NFR](../../u06-ai-connections/nfr-requirements/nfr-requirements.md), [NFR design](../../u06-ai-connections/nfr-design/nfr-design-patterns.md), [infrastructure](../../u06-ai-connections/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chi tiết hóa ngày 2026-10-21 (Part 1); người dùng duyệt ngày 2026-10-21 ("duyet"). Bước 1–14 xong ngày 2026-10-22 (xem [code-summary](../../u06-ai-connections/code/code-summary.md)); bước 15: người dùng duyệt code ngày 2026-10-22; chờ Max2Min gộp vào `staging`. Nhánh git: `ai-connections` tạo từ `staging`; mỗi bước một commit scope `u06`. Code xong, test trên nhánh, Max2Min gộp vào `staging`.

## Ngữ cảnh unit

| Mục | Nội dung |
|---|---|
| Interface cung cấp | `IAiTask` (U07, U08, U09, U10 gọi; U09 của Hưng đã gọi `submit` với loại `GRADING_PROPOSAL`); `ISettingsSectionProvider` (2 section cho Setting List của U03); `IJobHandler` `AI_TASK`, `IPendingSweeper`, `IScheduledScanner` (U03); MCP server `POST /mcp`; frontend `AiSourcePicker`, `AiTaskStatus` |
| Contract khai báo (đã có) | `IAiTaskResultHandler` (U09 đã cài `GradingProposalResultHandler`), `IMcpToolProvider` (U09 đã cài `GradingMcpTools`), `IReferenceMaterial` (U13 cài; bản tạm trả rỗng) |
| Interface dùng | `IAuthorization`, `IAccountLookup` (U01), `IAudit` (U02), `IJob` (U03) |
| Dữ liệu sở hữu | 7 bảng `ai_connections`, `ai_tasks`, `ai_usage_records`, `ai_quota_policy`, `ai_teacher_quotas`, `ai_quota_windows`, `mcp_credentials` |
| Frontend đã có | Nút "Kết nối AI" (`features/ai/register.ts`, `href /ai/connection`); `settingsSectionRegistry` của U03 chờ panel U06 |
| Design | Màn #6 AI Connection (`c12`, `c12a`, `c12b`, `c12c`), #7 AI Usage History (`c13`), #10 Setting Detail phần API AI và hạn mức (`a14`, `a14c`), popup Chọn nguồn AI (`c16`) |
| Extension | Security Baseline (SECURITY-01, 03, 05, 08, 09, 10, 11, 12, 15), Resiliency Baseline (RESILIENCY-04, 06, 10) |

## Khác biệt phát hiện khi chi tiết hóa

1. **Người code.** Plan cũ ghi Người 5; Jira 2026-10-20 giao cho Người 2.
2. **`UsageMeasurement` thiếu `ESTIMATED`.** BR-U06-14, 40 có nhãn `ESTIMATED`; thêm hằng này vào contract (thêm giá trị enum, không phá unit đang dùng).
3. **Thứ tự Flyway.** Migration U06 là `V20260925_1200`, nhỏ hơn migration đã chạy trên `staging` (1500 U09, 1600 U10, 1700 U11). Bật `spring.flyway.out-of-order: true` trong `application.yml`; U07 (1300), U08 (1400) gộp sau cũng cần điều này (báo nhóm khi gộp).
4. **Quyền.** Thêm `Actions.AI_PERSONAL` (STUDENT, TEACHER, SUBJECT_MANAGER; kết nối cá nhân, credential MCP, usage của mình), `AI_SHARED_USE` (TEACHER, SUBJECT_MANAGER; dùng API chung), `AI_ADMIN` (ADMIN; API chung, quota, tổng hợp usage) vào U01.
5. **Lưu trữ.** tech-stack ghi JPA; dùng `JdbcClient` như U01–U03 và `SELECT ... FOR UPDATE` cho cửa sổ quota (P1).
6. **MCP server.** tech-stack chọn MCP Java SDK; SDK hiện ở bản 2.x đổi API nhiều và chưa có trong máy. Đề xuất tự cài JSON-RPC 2.0 theo transport Streamable HTTP ở chế độ trả JSON (`initialize`, `notifications/initialized`, `tools/list`, `tools/call`, `ping`) trong `McpController`, chuỗi bảo mật riêng cho `/mcp` (chỉ bearer, không cookie, không CSRF). Phạm vi giao thức nhỏ, test được hết; đổi sang SDK sau nếu nhóm muốn.
7. **Kiểm JSON schema.** Thêm `com.networknt:json-schema-validator` 1.5.6 để kiểm kết quả AI và tham số tool (NFR-U06-33).
8. **Giới hạn lời gọi MCP.** Dùng Bucket4j + Redis như `RequestThrottle` của U01 (60 lời gọi/phút/credential).
9. **Provider.** Gọi REST trực tiếp bằng `RestClient` (Gemini `generateContent` có `responseSchema`, OpenAI Responses/Chat có `response_format` JSON schema, Anthropic Messages với tool để ép JSON); test adapter bằng HTTP server giả của JDK (`com.sun.net.httpserver`), không thêm WireMock.
10. **`AiSourcePicker` đặt ở `frontend/src/shared/ai/`** để U07–U10 dùng mà không import feature của unit khác; U09 đang dùng ô chọn tạm `AiSourceFields`, đổi sang picker là việc của U09 sau khi U06 gộp.
11. **Hướng dẫn agent MCP.** Chưa thử được ứng dụng agent thật trên máy này; `docs/guides/mcp-clients.md` ghi cấu hình mẫu và nhãn "chưa kiểm chứng" cho từng ứng dụng (BR-U06-50).
12. **Biến môi trường.** `U06_AI_KEY_ENCRYPTION_KEY` bắt buộc ở môi trường chạy thật; local/test dùng khóa riêng trong `.env.local` và cấu hình test, không có giá trị mặc định trong code (SECURITY-09).

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/.../ai/contract/` | 20 kiểu dữ liệu và interface (xem domain-entities mục 6); bản tạm `AiContractStubs` (tự tắt khi có bean thật) |
| `backend/.../ai/` các tầng khác | Chỉ `.gitkeep` |
| `backend/.../jobs/application/JobRouting` | Đã có `AI_TASK` → queue `jobs.ai` |
| `infra/nginx/snippets/locations.conf` | Đã có `location /mcp` (không đệm, timeout 300 giây) |
| `frontend/src/features/ai/` | `register.ts` (nút Kết nối AI), `index.ts` trống |

## Checklist

Unit test viết cùng bước tạo code; mỗi bước xong chạy lệnh như CI rồi mới commit.

- [x] **Bước 1 — Migration, quyền, cấu hình:** `db/migration/ai/V20260925_1200__create_ai_tables.sql` (7 bảng; unique riêng phần kết nối chưa `REVOKED`; `ai_tasks.request_ref` unique, index `(actor_id, state)`, `(state, created_at)`; usage index `(account_id, created_at)`, `(source, created_at)`; `mcp_credentials.token_hash` unique; FK `accounts`; `app` không `DELETE`); `out-of-order`; `Actions` và `AccessPolicy` kèm test; `UsageMeasurement.ESTIMATED`; cấu hình `ai.providers.*.models`, `maxOutputTokens`, khóa mã hóa. Commit `feat(u06): add AI tables, actions and provider settings`.
- [x] **Bước 2 — Domain:** `AiConnection`, `AiTask`, `AiUsageRecord`, `AiQuotaPolicy`, `AiTeacherQuota`, `AiQuotaWindow`, `McpCredential`, enum; chuyển trạng thái task (BR-U06-22), kết nối (BR-U06-05); repository `JdbcClient`. Unit test chuyển trạng thái. Commit `feat(u06): add AI domain and repositories`.
- [x] **Bước 3 — Key và provider (BR-U06-02, 03, 25; P4):** `KeyCipher` (AES-256-GCM, IV ngẫu nhiên, `keyVersion`), `ProviderCatalog` (model cho phép, trần `maxOutputTokens`), `GeminiAdapter`, `OpenAiAdapter`, `ClaudeAdapter` (địa chỉ cố định, structured output, usage chuẩn hóa, lỗi có tính token / không tính token / không rõ, timeout 120 giây, kiểm tra 20 giây). Unit test mã hóa và che key, model ngoài danh sách, adapter với HTTP server giả (thành công, 401, 429, 5xx, hết giờ, JSON sai). Commit `feat(u06): encrypt keys and call AI providers`.
- [x] **Bước 4 — Kết nối (F1–F3, F8; BR-U06-01–06, 52):** `AiConnectionService` (cá nhân: thêm, thay, thu hồi; API chung: thêm, thay, bật/tắt, thu hồi; `version` lệch `409`), `ConnectionTestService` (yêu cầu tổng hợp cố định, tối đa 5 token, lưu kết quả đã làm sạch); audit `AI_CONNECTION_CHANGED`, `AI_CONNECTION_TESTED`. Unit test: Admin không có kết nối cá nhân, mỗi provider một kết nối, key không ra API, thu hồi xóa bản mã. Commit `feat(u06): manage personal and institutional AI connections`.
- [x] **Bước 5 — Quota (F9, F10; BR-U06-12–17; P1):** `QuotaPolicyService` (mức mặc định, trần, mức riêng; audit `QUOTA_CHANGED`), `QuotaReservationService` (giữ chỗ nguyên tử theo thứ tự toàn hệ thống rồi người gọi, quyết toán token thật, giải phóng, tính đã dùng `ESTIMATED`), `UsageSummaryService` (Admin: theo giảng viên, ngày, provider/model). Unit test: chưa cấu hình thì không dùng được `SHARED`, vượt mức ngày, vượt trần, mức riêng 0 chặn, quyết toán, giải phóng, ngày theo giờ Việt Nam. Commit `feat(u06): reserve and settle shared AI quota`.
- [x] **Bước 6 — Task (F11–F13; BR-U06-10, 11, 17, 20–27; P2, P3, P6):** `AiTaskService` cài `IAiTask` (kiểm nguồn và quyền dùng nguồn, tối đa 3 task đang chờ/chạy, `requestRef` và dấu vân tay, giữ chỗ, gửi job sau commit), `AiSourceService` (nguồn dùng được, quota còn lại, lý do không dùng được), `AiTaskResultHandlerRegistry`, `AiTaskJobHandler` (ghi mốc gửi rồi gọi provider; message đến lại có mốc gửi thì `UNKNOWN`; kiểm schema; lưu `READY` rồi gọi handler trong transaction riêng; xóa prompt và kết quả), `AiTaskPendingSweeper`, `AiTaskTimeoutScanner` (chờ quá 30 phút thì `CANCELLED`, chạy quá hạn thì `UNKNOWN`); hủy task `QUEUED`. Unit test: Student, Admin không dùng `SHARED`, không tự đổi nguồn, `requestRef` lặp và khác dấu vân tay, message đến lại, sai schema, handler trả `STALE`, quá 3 task. Commit `feat(u06): run AI tasks through the worker`.
- [x] **Bước 7 — Usage (F7; BR-U06-35, 40):** `AiUsageService` ghi usage cho task, kiểm tra kết nối, MCP (không ghi 0 khi không biết), đọc lịch sử của chính mình có lọc và phân trang. Unit test. Commit `feat(u06): record and list AI usage`.
- [x] **Bước 8 — MCP (F4–F6; BR-U06-30–35; P5):** `McpCredentialService` (tạo: token 256 bit hiện một lần, lưu SHA-256 và 4 ký tự cuối, hạn 7/30/90 ngày, tối đa 5; thu hồi), `McpAuthenticationFilter` và chuỗi bảo mật `/mcp`, `McpController` (JSON-RPC: `initialize`, `tools/list` lọc theo role và quyền, `tools/call` kiểm `READ_WRITE` cho tool ghi, schema và 64 KB tham số, 256 KB kết quả), `McpToolRegistry` (gom `IMcpToolProvider`), `McpRateLimiter`; usage và audit `MCP_TOOL_CALLED`. Unit test: credential hết hạn, thu hồi, tài khoản không `ACTIVE`, vẫn dùng được sau đổi mật khẩu, tool ghi với `READ_ONLY`, tham số quá cỡ, quá 60 lời gọi/phút, so sánh thời gian hằng. Commit `feat(u06): serve MCP tools to user agents`.
- [x] **Bước 9 — Settings (F14):** `AiConnectionsSection`, `TeacherQuotaSection` cài `ISettingsSectionProvider` (tóm tắt cho Setting List). Commit `feat(u06): add AI sections to settings`.
- [x] **Bước 10 — API:** controller theo domain-entities mục 7 (`/api/v1/ai/*`, `/api/v1/me/ai-*`, `/api/v1/me/mcp-credentials`, `/api/v1/admin/ai/*`); `Actor` từ `@AuthenticationPrincipal`; lỗi Problem Detail có `code`; ngoài phạm vi `404`. Unit test MockMvc standalone. Commit `feat(u06): expose AI connection REST API`.
- [x] **Bước 11 — Frontend người dùng (#6, #7, `c16`):** `app/ai/connection` `AiConnectionPage` (`AiSummaryCards`, `PersonalApiSection`, `ApiKeyDialog`, `ApiSetupGuide`, `McpAgentSection`, `McpCredentialDialog`, `McpTokenDialog`, `McpSetupGuide`); `app/ai/usage` `AiUsageHistoryPage`; `shared/ai/AiSourcePicker`, `AiTaskStatus`. Test component. Commit `feat(u06): add AI connection and usage screens`.
- [x] **Bước 12 — Frontend Admin (#10):** `AiConnectionsPanel`, `TeacherQuotaPanel` (`InstitutionalKeysSection`, `QuotaPolicySection`, `TeacherQuotaTable`, `SharedUsageTab`) đăng ký vào `settingsSectionRegistry`. Test component. Commit `feat(u06): add AI settings panels for administrators`.
- [x] **Bước 13 — Hạ tầng và hướng dẫn:** biến `U06_AI_KEY_ENCRYPTION_KEY` trong compose, `.env.example`, runbook local; `docs/guides/ai-api-keys.md` (Gemini, OpenAI, Claude), `docs/guides/mcp-clients.md`. Commit `docs(u06): add AI key and MCP client guides`.
- [x] **Bước 14 — Tài liệu:** `aidlc-docs/construction/u06-ai-connections/code/code-summary.md`; đánh dấu plan; cập nhật `aidlc-state.md`. Commit `docs(u06): add code summary and record U06 progress`.
- [x] **Bước 15 — Review:** đối chiếu UC 31–33, phần UC 14–15, story, BR-U06 với code; tóm tắt tuân thủ extension; người dùng duyệt; Max2Min gộp `ai-connections` vào `staging`.

## Integration test cases (tester viết)

- 20 yêu cầu đồng thời của một giảng viên không vượt mức ngày và trần toàn hệ thống.
- Worker nhận lại message sau khi đã gửi provider: không gọi lần hai, task `UNKNOWN`, quota tính đã dùng.
- Hết quota chung không tự chuyển sang API cá nhân; Student không dùng được API chung qua web hay MCP.
- Thu hồi credential MCP hoặc khóa tài khoản rồi gọi tool ngay: bị từ chối.
- Key không xuất hiện trong response, log, audit.
- Phân quyền 4 role trên mọi endpoint, gồm dùng ID kết nối, task, credential của người khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
