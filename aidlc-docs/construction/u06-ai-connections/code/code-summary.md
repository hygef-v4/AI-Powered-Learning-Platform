# U06 AI Connections — code summary

Code viết ngày 2026-10-21 đến 2026-10-22 theo [plan](../../plans/code-generation-plan/u06-ai-connections-code-generation-plan.md) (duyệt 2026-10-21), trên nhánh `ai-connections` tách từ `staging`, chưa push. Người code: Võ Minh Tài (Người 2). Chưa gọi provider AI thật (không có khóa API thật trên máy), chưa thử với ứng dụng agent MCP thật; integration test do tester viết.

## Commit

| Bước | Commit | Nội dung |
|---|---|---|
| Plan | `43e983a` | Chi tiết hóa plan |
| 1 | `2c0fbb8` | Migration 7 bảng, quyền `AI_PERSONAL` / `AI_SHARED_USE` / `AI_ADMIN`, cấu hình `platform.ai`, Flyway out-of-order |
| 2 | `f847706` | Domain và repository |
| 3 | `c26943a` | Mã hóa khóa, gọi provider |
| 4 | `b954a13` | Kết nối cá nhân và của trường |
| 5 | `5becb57` | Giữ chỗ và quyết toán quota chung |
| 6 | `0061df4` | Chạy task AI qua worker |
| 7 | `bac7e80` | Ghi usage và lịch sử |
| 8 | `41fd952` | MCP server cho agent của người dùng |
| 9 | `76ee288` | Hai mục trong Settings |
| 10 | `3ed1b80` | REST API (kèm sửa constructor `HttpProviderGateway` phát hiện khi khởi động thử) |
| 11 | `a1fcfd7` | Màn Kết nối AI, Lịch sử sử dụng AI, `AiSourcePicker`, `AiTaskStatus` |
| 12 | `df97b06` | Panel Admin trong Settings |
| 13 | `7706f9c` | Biến môi trường, rewrite `/mcp` cho Next dev, runbook, hướng dẫn |
| 15 | `7a624b6` | Sửa sau review: giới hạn độ dài tên tool MCP và ô tìm giảng viên, log lỗi tool chỉ ghi loại lỗi |

## Business logic

| Luồng | Class chính |
|---|---|
| F1–F3 API cá nhân | `AiConnectionService` (lưu, thay có `version`, thu hồi, kiểm tra; audit `AI_CONNECTION_CHANGED`, `AI_CONNECTION_TESTED`), `ConnectionTestService` (yêu cầu cố định, tối đa 5 token), `KeyCipher` (AES-256-GCM, IV 12 byte, phiên bản khóa 1 và 2, thiếu khóa thì 503), `ProviderCatalog` |
| F4–F6 MCP | `McpCredentialService` (token `mcp_` + 256 bit, lưu SHA-256 và 4 ký tự cuối, hạn 7/30/90 ngày, tối đa 5; xác thực mỗi lời gọi theo role và trạng thái tài khoản hiện hành), `McpServer` (JSON-RPC 2.0: `initialize`, `ping`, `tools/list`, `tools/call`; lọc theo role và quyền, tham số 64 KB và đúng schema, cắt kết quả 256 KB, usage và audit `MCP_TOOL_CALLED`), `McpToolRegistry` (gom `IMcpToolProvider`, trùng tên thì báo lỗi), `IMcpRateLimiter` / `RedisMcpRateLimiter` (Bucket4j, 60 lời gọi/phút, Redis lỗi thì từ chối) |
| F7 Lịch sử | `AiUsageService` (ghi usage provider và MCP, không đổi "không biết" thành 0; lịch sử của chính mình, tối đa 100 dòng/trang) |
| F8–F10 API chung, quota | `AiConnectionService` (phần `INSTITUTIONAL`, bật/tắt), `QuotaPolicyService` (hạn mức chung, mức riêng chỉ cho giảng viên, tổng hợp 7/30 ngày, audit `QUOTA_CHANGED`), `QuotaReservationService` (ngày theo Asia/Ho_Chi_Minh, khóa `GLOBAL` rồi tài khoản, giữ chỗ `(ký tự + 1)/2 + maxOutputTokens`, quyết toán, giải phóng) |
| F11–F13 Task | `AiTaskService` (cài `IAiTask`: kiểm, dấu vân tay SHA-256, `requestRef` trùng thì trả lại hoặc 409, tối đa 3 task, giữ chỗ, gửi job `AI_TASK`; hủy task `QUEUED`), `AiTaskJobHandler` (worker: ghi mốc đã gửi trước khi gọi provider, nhận lại sau khi gửi thì `UNKNOWN`; quét timeout), `AiTaskDelivery` (chuyển kết quả cho handler trong transaction riêng, xóa prompt và kết quả), `JsonSchemaChecker`, `AiSourceService` (nguồn dùng được kèm lý do và quota) |
| F14 Settings | `AiConnectionsSection` (`ai-connections`), `TeacherQuotaSection` (`teacher-quota`) cài `ISettingsSectionProvider` |
| Quyền chung | `AiAccess` (qua `IAuthorization`; từ chối thì audit `ACCESS_DENIED` và trả 404) |

## Provider

`HttpProviderGateway` gọi REST bằng `java.net.http.HttpClient` tới địa chỉ cố định: Gemini `generateContent` với `responseJsonSchema`, OpenAI Chat Completions với `response_format` JSON schema, Claude Messages với tool bắt buộc `return_result`. 4xx, 503, 529 là `REJECTED`; 5xx khác và hết thời gian là `UNKNOWN`. Mô hình mặc định: `gemini-2.5-flash`, `gemini-2.5-pro`, `gpt-4.1-mini`, `gpt-4.1`, `claude-sonnet-5-5`, `claude-haiku-5-5` (đổi qua biến môi trường).

## API

| Endpoint | UC |
|---|---|
| `GET /api/v1/ai/providers` | 14, 31 |
| `GET /api/v1/me/ai-connections`, `PUT`, `DELETE /{provider}`, `POST /{provider}:test` | 31 |
| `GET`, `POST /api/v1/me/mcp-credentials`, `DELETE /{id}` | 32 |
| `POST /mcp` (Bearer; `GET` 405; có `Origin` thì 403) | 32 |
| `GET /api/v1/me/ai-usage` | 33 |
| `GET /api/v1/ai/sources`, `GET /api/v1/ai/tasks/{id}`, `POST /{id}:cancel` | U07–U10 |
| `GET /api/v1/admin/ai/connections`, `PUT /{provider}`, `POST /{provider}:test`, `PUT /{provider}/status`, `DELETE /{provider}` | 14, 15 |
| `GET`, `PUT /api/v1/admin/ai/quota-policy`; `GET /api/v1/admin/ai/teacher-quotas`, `PUT`, `DELETE /{accountId}`; `GET /api/v1/admin/ai/teacher-candidates` | 14, 15 |
| `GET /api/v1/admin/ai/usage-summary?days=7\|30` | 33 |

Lỗi trả Problem Detail kèm `code` qua `GlobalExceptionHandler`; ngoài quyền trả 404 `AI_NOT_FOUND`.

## Repository và migration

- `AiConnectionRepository`, `AiTaskRepository`, `AiUsageRepository`, `QuotaRepository`, `McpCredentialRepository` dùng `JdbcClient`; cửa sổ quota khóa bằng `SELECT ... FOR UPDATE`; chuyển trạng thái task và kết nối là cập nhật có điều kiện theo `version`.
- `db/migration/ai/V20260925_1200__create_ai_tables.sql`: 7 bảng; role `app` có `select`, `insert`, `update` (thêm `delete` cho `ai_teacher_quotas`).
- `spring.flyway.out-of-order: true` vì 1200 nhỏ hơn migration đã chạy trên `staging`.

## Frontend

- `app/ai/connection` (`AiConnectionPage`: `AiSummaryCards`, `PersonalApiSection`, `ApiKeyDialog`, `ApiSetupGuide`, `McpAgentSection`, `McpCredentialDialog`, `McpTokenDialog`, `McpSetupGuide`), `app/ai/usage` (`AiUsageHistoryPage`); route chỉ cho Student, Teacher, Subject Manager.
- `shared/ai`: `AiSourcePicker` (popup c16, không chọn sẵn, nguồn không dùng được hiện mờ kèm lý do), `AiTaskStatus` (hỏi mỗi 2 giây, hủy khi còn chờ), kiểu dữ liệu và định dạng số, ngày.
- `features/ai/admin`: `AiConnectionsPanel`, `TeacherQuotaPanel` (cùng trang, tab Khóa và hạn mức, Usage; mở từ thẻ hạn mức thì cuộn tới phần hạn mức), `InstitutionalKeysSection`, `QuotaPolicySection`, `TeacherQuotaTable`, `SharedUsageTab`; đăng ký vào `settingsSectionRegistry` của U03.

## Hạ tầng và hướng dẫn

- Biến `U06_AI_KEY_ENCRYPTION_KEY`, `U06_AI_KEY_ENCRYPTION_KEY_V2` trong `infra/docker-compose.yml` và `infra/.env.example`; nginx đã có `location /mcp`; Next dev chuyển tiếp `/mcp` sang backend.
- `docs/runbooks/ai-key-encryption.md` (tạo khóa, chạy dev, đổi khóa), `docs/guides/ai-api-keys.md`, `docs/guides/mcp-clients.md` (ghi "chưa kiểm chứng").

## Kiểm tra

- Backend U06: 90 test (domain 16, repository trên PostgreSQL 16 2, khóa và catalog 6, gateway 8, kết nối 7, quota 10, task 13, nguồn 2, usage 3, MCP credential 6, MCP server 8, Settings 2, controller 5) và `ArchitectureTest` pass.
- `mvn -B verify` toàn backend: 915 test, 33 bỏ qua, 4 lỗi. Cả 4 lỗi là integration test của unit khác (`AccountRepositoryIntegrationTest`, `JdbcQuizAttemptRepositoryIntegrationTest`, `JdbcQuestionRepositoryIntegrationTest`, `JdbcQuizRepositoryIntegrationTest`) dừng ở migration `V20260925_1500__create_grading_tables.sql` của U09 (đã có trên `staging` từ `470d787`): bảng `assignments` chưa tồn tại. U06 không sửa file này.
- Frontend: 305 test (U06: 13), lint, typecheck, build pass.
- Khởi động thử backend với PostgreSQL và Redis local (tắt Flyway và kiểm schema vì lỗi migration 1500 ở trên): context lên; `/mcp` trả 401 kèm `WWW-Authenticate: Bearer` khi thiếu hoặc sai token, `GET /mcp` trả 405; API thường vẫn 401 khi chưa đăng nhập và 403 `CSRF_INVALID` khi thiếu CSRF.
- Kiểm trên trình duyệt với API giả: màn Kết nối AI, tạo agent và popup token, Lịch sử sử dụng AI, Setting List, trang cài đặt AI của Admin (hai tab).

## Khác với thiết kế

- MCP tự cài JSON-RPC 2.0 theo Streamable HTTP chế độ trả JSON, không dùng MCP Java SDK; xác thực bearer trong `McpController` và chuỗi bảo mật riêng `/mcp` (không có `McpAuthenticationFilter` như plan ghi).
- Provider gọi bằng `java.net.http.HttpClient` thay cho `RestClient`.
- Thêm `UsageMeasurement.ESTIMATED` vào contract.
- Thêm `GET /api/v1/admin/ai/teacher-candidates` để tìm giảng viên khi đặt mức riêng (domain-entities mục 7 chưa có).
- Yêu cầu tới `/mcp` có header `Origin` bị từ chối (chặn DNS rebinding theo đặc tả MCP).
- Hướng dẫn agent MCP chưa kiểm chứng với ứng dụng thật (BR-U06-50 yêu cầu hướng dẫn cho ứng dụng đã thử).

## Việc cho nhóm

- Migration 1500 của U09 cần bảng `assignments` (U07/U08) nên chưa chạy được trên DB mới; báo Hưng trước khi gộp.
- U07 (1300), U08 (1400) gộp sau cũng nhờ `out-of-order`.
- U09 đang dùng ô chọn nguồn tạm `AiSourceFields`; đổi sang `AiSourcePicker` trong `@/shared/ai` sau khi U06 vào `staging`.

## Review bước 15

### UC và business rule

| Phạm vi | Đáp ứng ở | Ghi chú |
|---|---|---|
| UC 31 API cá nhân; BR-U06-01–06 | `AiConnectionService`, `KeyCipher`, `ConnectionTestService`, màn Kết nối AI | Khóa chỉ hiện 4 ký tự cuối; thay có `version`, lệch thì 409 |
| UC 32 MCP; BR-U06-30–35 | `McpCredentialService`, `McpServer`, `McpController`, `RedisMcpRateLimiter`, phần Agent MCP | Tool ghi ẩn với token chỉ đọc; usage `UNAVAILABLE` hoặc `EXTERNALLY_REPORTED` |
| UC 33 lịch sử; BR-U06-40, 41 | `AiUsageService`, `QuotaPolicyService.summary`, màn Lịch sử, tab Usage | Không có số liệu thì hiện "Không có số liệu" |
| UC 14, 15 API chung và quota; BR-U06-10–17 | `QuotaReservationService`, `QuotaPolicyService`, `AiSourceService`, panel Admin | Hết quota không tự đổi nguồn; Student không dùng `SHARED` |
| Task cho U07–U10; BR-U06-20–27 | `AiTaskService`, `AiTaskJobHandler`, `AiTaskDelivery`, `AiSourcePicker`, `AiTaskStatus` | Nhận lại sau khi đã gửi provider thì `UNKNOWN`, không gọi lần hai |
| BR-U06-50 hướng dẫn | `docs/guides/ai-api-keys.md`, `docs/guides/mcp-clients.md` | MCP chưa kiểm chứng với ứng dụng thật (khác thiết kế) |
| BR-U06-51–53 quyền, audit, fail closed | `AiAccess`, audit từng service, `McpServer` | Ngoài quyền trả 404 kèm `ACCESS_DENIED`; U01, Redis lỗi thì từ chối |

### Extension (Security Baseline, Resiliency Baseline theo phạm vi rút gọn 2026-09-24)

| Rule | Kết quả | Lý do |
|---|---|---|
| SECURITY-03 Logging | Tuân thủ | Log qua SLF4J của Spring Boot; không log khóa, token, prompt; lỗi tool chỉ ghi loại lỗi |
| SECURITY-04 Header HTTP | N/A | U06 không thêm endpoint trả HTML; header do nginx (`security-headers.conf`) |
| SECURITY-05 Kiểm đầu vào | Tuân thủ | Kiểm provider, model, độ dài khóa, nhãn, hạn dùng, trang, khoảng ngày, số token, từ khóa tìm (≤ 100), tên tool (≤ 100), tham số tool theo JSON schema và ≤ 64 KB, thân `/mcp` ≤ 1 MB; SQL dùng tham số |
| SECURITY-08 Kiểm quyền | Tuân thủ | Mọi service gọi `IAuthorization`; tài nguyên theo id kiểm chủ sở hữu; API Admin chỉ `AI_ADMIN`; `/mcp` không CORS và từ chối `Origin` |
| SECURITY-09 Hardening | Tuân thủ | Khóa mã hóa không có giá trị mặc định; lỗi trả Problem Detail không có stack trace |
| SECURITY-12 Credential | Tuân thủ | Khóa API mã hóa AES-256-GCM; token MCP chỉ lưu SHA-256, so sánh thời gian hằng, hết hạn tối đa 90 ngày; không có bí mật trong code |
| SECURITY-15 Lỗi và fail closed | Tuân thủ | Lỗi provider phân loại `REJECTED`/`UNKNOWN`; quyền, giới hạn lời gọi lỗi thì từ chối; `GlobalExceptionHandler` bắt lỗi chung |
| RESILIENCY-04 Triển khai | N/A | Không đổi pipeline; chỉ thêm biến môi trường vào compose |
| RESILIENCY-06 Health check | N/A | Không thêm phụ thuộc cần health riêng (Redis, PostgreSQL đã có health) |
| RESILIENCY-10 Cô lập phụ thuộc | Tuân thủ | Timeout provider 120 giây (kiểm tra 20 giây), không tự gọi lại; task chạy ở worker, không chặn request web |
