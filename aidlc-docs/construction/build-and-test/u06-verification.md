# U06 — build, test và acceptance gates

Phạm vi: U06 AI Connections & MCP (UC 31–33, phần UC 14–15) trên nhánh `ai-connections`, ngày 2026-10-22. Đây là phần bổ sung Build & Test của một unit, giống `u15-verification.md`; không thay gate của unit khác và không đánh dấu Build & Test toàn dự án là xong. Xem [code-summary](../u06-ai-connections/code/code-summary.md) và [plan](../plans/code-generation-plan/u06-ai-connections-code-generation-plan.md).

## 1. Build

**Yêu cầu:** JDK 17 trở lên (máy chạy dùng JDK 21), Maven 3.9, Node 20.9 trở lên, Docker cho PostgreSQL, Redis, RabbitMQ khi chạy thử. Biến `U06_AI_KEY_ENCRYPTION_KEY` (base64 32 byte, `openssl rand -base64 32`) để lưu và dùng khóa API; thiếu thì backend vẫn chạy nhưng thao tác với khóa trả `503` (xem `docs/runbooks/ai-key-encryption.md`).

```bash
mvn -B -f backend/pom.xml verify
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend test
NEXT_DIST_DIR=.next-verify-u06 npm --prefix frontend run build
```

Cùng lệnh với CI. Next tự thêm đường dẫn type của thư mục build tạm vào `frontend/tsconfig.json`; khôi phục file này sau khi build.

**Kết quả mong đợi:** JAR `backend/target/platform-backend-0.1.0-SNAPSHOT.jar`; route `/ai/connection`, `/ai/usage` trong bảng route của Next.

## 2. Unit test

```bash
mvn -B -f backend/pom.xml test -Dtest='edu.aiplatform.ai.**,ArchitectureTest'
npm --prefix frontend test -- src/features/ai src/shared/ai
```

Test repository chạy trên PostgreSQL thật khi có biến môi trường (database đã chạy migration):

```bash
export U06_TEST_JDBC_URL=jdbc:postgresql://localhost:55433/platform
export U06_TEST_JDBC_USER=app
export U06_TEST_JDBC_PASSWORD=<mật khẩu app của DB test>
export U06_TEST_ADMIN_JDBC_URL=$U06_TEST_JDBC_URL
export U06_TEST_ADMIN_USER=postgres
export U06_TEST_ADMIN_PASSWORD=<mật khẩu postgres của DB test>
mvn -B -f backend/pom.xml test -Dtest=AiRepositoriesIntegrationTest
```

## 3. Integration test (tester viết)

Môi trường: `docker compose -f infra/docker-compose.yml -f infra/docker-compose.local.yml up` (PostgreSQL, Redis, RabbitMQ, backend, worker) kèm `U06_AI_KEY_ENCRYPTION_KEY`. Gọi provider thật cần khóa API của người thử, nhập trên màn Kết nối AI.

| Kịch bản | Cách làm | Kết quả đúng |
|---|---|---|
| Giữ chỗ đồng thời | 20 yêu cầu `SHARED` đồng thời của một giảng viên có hạn mức nhỏ | Tổng giữ chỗ không vượt mức ngày và trần toàn hệ thống; yêu cầu vượt trả `429 AI_QUOTA_EXCEEDED` |
| Message đến lại sau mốc gửi | Dừng worker ngay sau khi task sang `RUNNING`, khởi động lại | Không gọi provider lần hai; task `UNKNOWN`; quota tính phần đã giữ |
| Hết quota không đổi nguồn | Giảng viên hết hạn mức gửi `SHARED` | `429`, không tự chuyển sang API cá nhân |
| Student và API chung | Student gửi `SHARED` qua web | `404`, audit `ACCESS_DENIED` |
| Thu hồi credential MCP | Thu hồi rồi gọi `POST /mcp` ngay | `401` |
| Khóa tài khoản | Admin khóa tài khoản rồi gọi `/mcp` bằng token còn hạn | `401` |
| Giới hạn MCP | 61 lời gọi `tools/call` trong một phút bằng một token | Lời gọi thứ 61 trả lỗi JSON-RPC `-32029` |
| Giới hạn API chung | 3 task `SHARED` cùng provider chạy cùng lúc trên một worker | Tối đa 2 lời gọi provider đồng thời; task thứ ba chờ (U03 thử lại), vẫn `QUEUED` |
| Provider thật | Lưu khóa thật từng provider, bấm Kiểm tra | "Đạt"; khóa sai báo lỗi, không lộ khóa trong log |
| Agent MCP thật | Tạo token, cấu hình một ứng dụng agent theo `docs/guides/mcp-clients.md` | `tools/list` trả đúng công cụ theo role và quyền; ghi lại ứng dụng đã kiểm chứng |

## 4. Hiệu năng (NFR-U06-01–04)

| Chỉ tiêu | Mục tiêu | Cách đo |
|---|---|---|
| Giữ chỗ quota | p95 ≤ 50 ms | Đo thời gian `QuotaReservationService.reserve` khi 50 yêu cầu `SHARED` đồng thời |
| API danh sách và usage | p95 ≤ 300 ms, trang ≤ 100 | k6 hoặc JMeter gọi `GET /api/v1/me/ai-usage`, `/me/ai-connections`, `/ai/sources` |
| Timeout provider | 120 giây (kiểm tra kết nối 20 giây) | HTTP server giả trả chậm (đã có trong `HttpProviderGatewayTest`) |
| Lời gọi đồng thời API chung | Tối đa 2 mỗi provider mỗi worker | Đã có unit test `AiTaskFlowTest`; integration kiểm như mục 3 |
| Xử lý MCP của U06 | p95 ≤ 100 ms (chưa tính unit sở hữu tool) | 50 lời gọi `tools/list` và `tools/call` với tool giả trả ngay |

Chưa chạy: máy dev không có công cụ tải.

## 5. Bảo mật

- Khóa API không xuất hiện trong response, log hay audit (chỉ 4 ký tự cuối); kiểm log sau kịch bản ở mục 3.
- `/mcp`: không cookie, không CSRF, chỉ bearer; có header `Origin` thì `403`; `GET` trả `405`.
- API thường giữ nguyên `401` khi chưa đăng nhập và `403 CSRF_INVALID` khi thiếu CSRF.
- Thiếu `U06_AI_KEY_ENCRYPTION_KEY` thì từ chối lưu khóa (`503`), không dùng khóa mặc định.

## 6. Kết quả đã chạy (2026-10-22)

| Hạng mục | Kết quả |
|---|---|
| Backend U06 | 91 test (89 pass + 2 test repository; 2 test repository chạy pass trên PostgreSQL 16 khi có biến môi trường), `ArchitectureTest` pass |
| `mvn -B verify` toàn backend (trước khi thêm giới hạn đồng thời) | 915 test, 33 bỏ qua, 4 lỗi; cả 4 là integration test của unit khác dừng ở migration `V20260925_1500__create_grading_tables.sql` (U09) vì thiếu bảng `assignments` |
| Frontend | 305 test pass (U06: 13), lint, typecheck, build pass |
| Khởi động thử | Context lên với PostgreSQL, Redis local; `/mcp` trả `401` kèm `WWW-Authenticate: Bearer` khi thiếu hoặc sai token, `GET /mcp` trả `405`; API thường giữ `401`, `403 CSRF_INVALID` |
| Trình duyệt (API giả) | Kết nối AI, tạo agent và popup token, Lịch sử sử dụng AI, Setting List, trang cài đặt AI của Admin |
| Integration, hiệu năng, provider và agent thật | Chưa chạy |

## 7. Sửa trong Build & Test

Rà NFR-U06-03 thấy chỉ có giới hạn chung 4 luồng của queue `jobs.ai` (U03), chưa có giới hạn 2 lời gọi đồng thời mỗi provider của API chung. Đã thêm (`eff4083`): `AiTaskJobHandler` lấy chỗ của provider trước khi ghi mốc gửi; hết chỗ thì ném `RetryableJobException` để U03 thử lại, task vẫn `QUEUED` nên không bị coi là đã gọi. Test `sharedApiAllowsTwoConcurrentCallsPerProviderAndThirdWaitsStillQueued`.

## 8. Gate còn mở

- Migration 1500 của U09 phải sửa thì `mvn verify` mới sạch.
- Thử với khóa API thật của ba provider và một ứng dụng agent MCP thật; cập nhật mục "Ứng dụng đã kiểm chứng" trong `docs/guides/mcp-clients.md`.
- Integration test và đo hiệu năng ở mục 3, 4 do tester làm.
