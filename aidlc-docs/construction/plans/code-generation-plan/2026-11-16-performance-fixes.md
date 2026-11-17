# Kế hoạch sửa các điểm trong báo cáo hiệu năng 2026-11-16

Nguồn: [báo cáo hiệu năng](../../build-and-test/2026-11-16-performance-report.md). Người dùng yêu cầu "sửa toàn bộ".

## Phạm vi và cách làm

- Brownfield, Requirements Analysis mức Minimal: yêu cầu là các mục 6.1–6.5 của báo cáo. Không có thành phần, API, schema hay quy tắc nghiệp vụ mới, nên bỏ qua User Stories, Application Design, Units Generation, Functional/NFR/Infrastructure Design. Chỉ chạy Code Generation và Build and Test.
- Nhánh mới `perf/production-fixes` tạo từ `origin/staging@b121f0ca` với `--no-track`. PR vào `staging`, không vào `main`.
- Mọi thay đổi đi qua repo. Không sửa tay trên VPS, trừ một lệnh `CREATE EXTENSION` sau khi bản mới lên (bước 9), do người vận hành quyết định.
- Giữ nguyên API, JSON, schema, URL. Tuân theo [quy ước mã nguồn](../../code-conventions.md).

## Các bước

Frontend:

- [x] 1. `NotificationBell.tsx`: chỉ polling `unread-count` khi SSE không kết nối; dừng khi tab ẩn, làm mới một lần khi tab hiện lại. Thêm test.
- [x] 2. Đưa `@/features/register` ra khỏi root layout, chỉ nạp trong vùng đã đăng nhập, để `/login`, `/activate`, `/reset-password` không tải bundle của 17 feature. Đo lại số script và dung lượng của `/login` sau khi build.
- [x] 3. `next.config.ts`: `Cache-Control` 30 ngày cho `/fonts/*` (tên file font không có hash nên không đặt `immutable`).

Nginx (`infra/nginx`, áp cho production, staging và local):

- [x] 4. `log_format` có `$request_time` và `$upstream_response_time`.
- [x] 5. Bật `gzip` cho `application/json` và `application/problem+json`. Không áp cho `text/event-stream`.
- [x] 6. Trả 404 tại nginx cho `*.php` và `/wp-*`.
- [x] 7. Khi backend/frontend chưa sẵn sàng (502/503/504): API nhận JSON 503 kèm `Retry-After`, trang nhận một trang bảo trì tĩnh, thay cho trang lỗi mặc định của nginx.

Compose và deploy:

- [x] 8. Backend production `cpus: 1` lên `2` (khởi động nhanh hơn, JVM chọn G1). Worker giữ nguyên.
- [x] 9. Postgres: nạp `pg_stat_statements`, `log_min_duration_statement=500`. Ghi vào runbook lệnh `CREATE EXTENSION pg_stat_statements` chạy một lần.
- [x] 10. Redis: `maxmemory 192mb`, giữ `noeviction` (Redis giữ phiên và OTP, không được tự xóa).
- [x] 11. `platform-deploy.sh`: sau khi deploy thành công, xóa image backend/frontend không còn nằm trong các bản quay về được. Cập nhật `test-platform-deploy.sh`.

Kiểm tra và tài liệu:

- [x] 12. Frontend: ESLint, TypeScript, Vitest, production build. Infra: `nginx -t` trong container, `docker compose config`, test của script deploy.
- [x] 13. Cập nhật runbook deploy, `aidlc-state.md`, kết quả kiểm tra.

Người dùng chọn phương án A (2026-11-16). Khác kế hoạch ở hai điểm: bước 7 chỉ áp cho 502 và 503, giữ nguyên 504 vì đó là quá thời gian chờ chứ không phải dịch vụ chưa sẵn sàng; bước 10 đặt `maxmemory 96mb` thay cho 192 MB vì container Redis của staging chỉ có 128 MB. [Kết quả kiểm tra](../../build-and-test/2026-11-16-performance-fixes-results.md).

## Không làm trong lượt này

- Mở `/actuator/prometheus` không cần đăng nhập. Đây là thay đổi cấu hình bảo mật trong lúc đang có một lượt rà soát bảo mật khác; bước 4 và 9 đã cho số đo thời gian phản hồi.
- HTTP/3, Brotli, CDN: lợi ích nhỏ với người dùng trong nước.
- Chỉnh `shared_buffers`, Hikari, Tomcat: chưa có số đo cho thấy cần.
- Load test: cần môi trường và kịch bản riêng.

## Quyết định cần người dùng chọn

Bước 7 và 8 rút ngắn và làm êm khoảng gián đoạn khi deploy nhưng không loại bỏ nó: backend cũ vẫn dừng trước khi backend mới sẵn sàng.

- **A (đề xuất):** chỉ làm bước 7 và 8 trong lượt này. Sau khi có số đo thật, xử lý deploy không gián đoạn thành một thay đổi riêng.
- **B:** làm thêm deploy cuốn chiếu trong `platform-deploy.sh` (khởi động container mới, chờ healthy, rồi gỡ container cũ). Loại bỏ 502, nhưng hai phiên bản backend chạy song song trong khoảng một phút, kể cả lúc Flyway của bản mới đang đổi schema; cần kiểm kỹ trên staging trước khi dùng cho production.
