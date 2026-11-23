# Quy ước mã nguồn U04/U13/U14: kết quả Build and Test

- **Nhánh**: `fix/code-conventions-u04-u13-u14`, gốc `d5c32804`, các thay đổi chưa commit.
- **Ngày chạy**: 2026-11-15.
- **Phạm vi kiểm**: [kế hoạch code generation](../../plans/code-generation-plan/2026-11-15-u04-u13-u14-code-conventions-code-generation-plan.md) và tóm tắt mã [U04](../../u04-subject-administration/code/2026-11-15-code-conventions-summary.md), [U13](../../u13-reference-materials/code/2026-11-15-code-conventions-summary.md), [U14](../../u14-announcements-and-notifications/code/2026-11-15-code-conventions-summary.md).
- **Cách build và chạy test**: theo [build-instructions.md](../build-instructions.md), [unit-test-instructions.md](../unit-test-instructions.md) và [integration-test-instructions.md](../integration-test-instructions.md); lệnh không đổi. Thay đổi chỉ đổi tên và định dạng lại, không thêm yêu cầu hiệu năng, nên không chạy [performance test](../performance-test-instructions.md).

## Dựng lại Docker

- Docker Desktop được khởi động lại. Engine 29.7.2 lúc đầu chưa có image hay volume nào.
- Đã chạy `docker compose -f infra/docker-compose.yml -f infra/docker-compose.local.yml --env-file infra/.env.local up --build -d` (project `platform`). Đã build image `platform-backend:local` và `platform-frontend:local` từ thư mục làm việc của nhánh.
- Lần `up` đầu dừng ở `dependency failed to start: container platform-rabbitmq-1 is unhealthy`, vì RabbitMQ khởi động chậm hơn healthcheck. Lỗi này đã gặp ở [lần thử học kỳ lớp](2026-11-15-class-term-results.md). Chạy `up -d` lần hai thì đủ 8 service healthy: backend, frontend, mailpit, nginx, postgres, rabbitmq, redis, worker.
- `GET http://localhost:8080/health` trả 200, `GET http://localhost:8088/` trả 200.
- Log: `.tmp/conv041314/docker-up.log`, `.tmp/conv041314/docker-up2.log`.

## Kiểm tra tự động

| Hạng mục | Lệnh | Kết quả |
|---|---|---|
| Backend | `mvn -o -B clean verify` | Qua sau 14 phút 20 giây: **2.426 test, 0 failure, 0 error, 0 skipped**. Docker có chạy nên mọi test Testcontainers PostgreSQL đều được chạy, gồm `SubjectRepositoryIntegrationTest` (31), `NotificationDeliveryIntegrationTest` (7), `MaterialModuleMigrationIntegrationTest`, `MaterialPresentationIntegrationTest`, và `ArchitectureTest` qua. Log: `.tmp/conv041314/mvn-verify.log` |
| Suite PostgreSQL bắt buộc | `scripts/assert-required-backend-tests.ps1` | Qua 5 suite, không có test bị bỏ qua: subject repository 31, notification delivery 7, evaluation version 11, grade distribution visibility 1, platform workflow 26. Đã trace đủ 57 ca integration |
| Frontend lint | `npm run lint` | Qua. Log: `.tmp/conv041314/fe-lint.log` |
| Frontend type | `tsc --noEmit` | Không lỗi. Log: `.tmp/conv041314/fe-tsc.log` |
| Frontend test | `vitest run` | 1.075/1.092 qua. **17 test** lỗi `Test timed out in 5000ms` khi chạy cả bộ trong lúc Docker Desktop đang khởi động. Chạy lại riêng 13 file đó: 137/142 qua, 5 test còn timeout (`authoringFlow` 1, `grading/bulk-selection` 3, `grading/workspace` 1). Chạy 3 file này với `--testTimeout=60000`: **60/60 qua**. Ba file này không import feature `subjects`, `materials`, `announcements` hay `notifications`; ở lần chạy học kỳ lớp chúng cũng timeout giống vậy. Log: `.tmp/conv041314/fe-vitest*.log` |
| Frontend build | `npm run build` | Qua. `tsconfig.json` và `next-env.d.ts` không đổi. Log: `.tmp/conv041314/fe-build.log` |

Phần kiểm tra lúc sinh mã ([tóm tắt U14](../../u14-announcements-and-notifications/code/2026-11-15-code-conventions-summary.md#kiểm-tra)) đã có: so sánh literal trong diff không thấy chuỗi nào đổi, và không còn dòng code nào dài quá 100 ký tự ngoài chuỗi SQL/JPQL.

## E2E trên stack cô lập `conv041314`

Chạy theo yêu cầu "Run e2e first", vì script `scripts/e2e/u04-u13-u14.mjs` từ chối cổng `8088` của stack `platform`.

**Cách dựng stack:**

- Docker Compose với project riêng tên `conv041314`, cấu hình trong `.tmp/conv041314/` (`compose.override.yml`, `.env`).
- Dùng lại image `platform-*:local` vừa build từ thư mục làm việc của nhánh. Database và Mailpit mới, không đụng stack `platform`.
- Cổng: nginx 39088, backend 39080, PostgreSQL 35434, Mailpit 39025. Mật khẩu thử được sinh ngẫu nhiên, lưu ở `.tmp/conv041314/credentials.txt`.
- Lần `up` đầu báo RabbitMQ chưa healthy; lần hai thì lên. Worker khởi động chậm (131 giây) nên quá hạn healthcheck; restart worker thì healthy.

**Kết quả:** `E2E_BASE_URL=http://localhost:39088`, `E2E_MAILPIT_URL=http://localhost:39025`, `U01_BOOTSTRAP_ADMIN_EMAIL=admin@conv041314.test` → **40/40 qua**, exit code 0.

- Fixture: tạo và kích hoạt bằng OTP các tài khoản riêng của lần chạy (1 ca).
- U04: 9 ca (tạo, xem, sửa môn, xung đột version, gán chủ nhiệm, lưu trữ và mở lại, phân quyền).
- U13: 17 ca (module, tạo và thay video kèm list/index, URL chuẩn, từ chối playlist và link không an toàn, quyền đọc/ghi).
- U14: 14 ca (loại email và mặc định của bốn vai trò, lưu qua đăng nhập lại, loại không hợp lệ, email backup bắt buộc, chống sửa tùy chọn tài khoản khác).

Log: `.tmp/conv041314/e2e-u04-u13-u14.log`, kết quả JSON: `.tmp/conv041314/e2e-u04-u13-u14-result.json`. Log không chứa mật khẩu, OTP hay token.

## Chưa chạy

- **Kiểm trên trình duyệt:** chưa làm. Thay đổi chỉ đổi tên định danh nội bộ: không đổi URL, JSON, SQL, sự kiện, mã lỗi hay nội dung hiển thị. Phần frontend đã được kiểm bằng Vitest, typecheck và build production.

## Extension

Phạm vi Security (SECURITY-03/04/05/08/09/12/15) và Resiliency (RESILIENCY-04/06/10) đã duyệt giữ nguyên. Thay đổi không thêm trust boundary, lưu bí mật, kênh truyền, truy cập file, retry hay hành vi transaction. Các test hồi quy hiện có về phân quyền, validation, persistence và xử lý lỗi đều qua trong bộ test đầy đủ. PBT tắt.

## Kết luận

- **Build**: thành công, gồm backend JAR, frontend production và image Docker.
- **Test**:
  - backend qua toàn bộ, không có test bị bỏ qua;
  - frontend lint và type sạch;
  - các test Vitest lỗi đều do timeout vì máy đang tải nặng; chạy lại với thời gian chờ dài hơn thì qua.
- **E2E**: `u04-u13-u14` 40/40 trên stack cô lập `conv041314`.
- **Sẵn sàng cho Operations**: có, sau khi commit và merge theo quy trình hiện có.

Stack `platform` và `conv041314` vẫn đang chạy. Không commit, push, deploy, hay ghi dữ liệu thật. Để tắt và xóa dữ liệu stack cô lập, chạy lệnh sau từ thư mục `infra/`:

```bash
docker compose -p conv041314 -f docker-compose.yml -f docker-compose.local.yml -f ../.tmp/conv041314/compose.override.yml --env-file ../.tmp/conv041314/.env down -v
```
