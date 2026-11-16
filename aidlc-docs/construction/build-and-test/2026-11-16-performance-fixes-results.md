# Kết quả kiểm tra: sửa các điểm hiệu năng 2026-11-16

Nhánh `perf/production-fixes` từ `origin/staging@b121f0ca`. [Kế hoạch](../plans/code-generation-plan/2026-11-16-performance-fixes.md), [báo cáo gốc](2026-11-16-performance-report.md). Chưa commit, chưa push, chưa deploy.

## Đã chạy

| Kiểm tra | Kết quả |
|---|---|
| Frontend ESLint | qua |
| Frontend TypeScript | qua |
| Frontend Vitest | 1.160/1.160 (thêm 1 test cho chuông thông báo) |
| Frontend production build | qua |
| `test-platform-deploy.sh` | 15/15 (thêm 1 test dọn image) |
| `nginx -t` với template local và các snippet | qua |
| nginx không có backend/frontend | `/api/*`, `/mcp`: 503 `application/problem+json`, `Retry-After: 15`; `/login`: 503 trang bảo trì; header bảo mật còn đủ |
| nginx với bot | `/wp-admin/install.php`, `/x/y.php`: 404; `/api/v1/a.php` không bị chặn |
| nginx nén JSON | JSON 3 KB từ backend giả: `Content-Encoding: gzip`, 71 byte |
| nginx log | dòng log có `rt=` và `urt=` |
| Postgres 16 với `command` mới | `shared_preload_libraries=pg_stat_statements`, `CREATE EXTENSION` chạy được, truy vấn 600 ms được ghi log |
| Redis 7.4 với tham số mới | `maxmemory` 96 MB, `noeviction` |

Nginx, Postgres, Redis và test script deploy chạy trong container tạm trên VPS (mạng Docker riêng, thư mục `/tmp`, đã xóa sau khi chạy) vì Docker ở máy phát triển không chạy. Không đụng tới container production hay staging.

## Trang đăng nhập sau khi sửa

Đo trên bản build production chạy ở máy (`next start`), cache trống:

| | Production hiện tại (`e3825aa6`) | Nhánh này |
|---|---|---|
| Số script | 25 | 12 |
| JavaScript sau giải nén | 1,05 MB | 1,19 MB, trong đó 643 KB là chuỗi dịch |
| JavaScript không tính chuỗi dịch | 1,05 MB | 0,55 MB |
| `Cache-Control` của font | `max-age=0` | `max-age=2592000` |

Hai cột không cùng nền mã: production chưa có i18n. Phần bundle của 17 feature đã rời khỏi trang đăng nhập, nhưng `staging` mang thêm một chunk 643 KB chứa chuỗi dịch của cả ba ngôn ngữ (`src/i18n/msg.ts` import tĩnh `en.json`, `vi.json`, `ja.json`), nạp ở mọi trang. Tổng dung lượng JavaScript của trang đăng nhập vì vậy chưa giảm so với production. Việc tách chuỗi dịch theo ngôn ngữ nằm ngoài kế hoạch đã duyệt.

## Chưa kiểm

- `docker compose config` và cả stack chạy cùng nhau với compose mới.
- Template nginx của production và staging (cần chứng chỉ TLS). Hai template này chỉ thêm một dòng `include` giống template local.
- Thời gian khởi động backend với 2 CPU và việc JVM chọn G1.
- Các trang sau đăng nhập trên trình duyệt sau khi dời phần đăng ký slot. Các test dùng registry (menu theo role, ngữ cảnh lớp, tổng quan lớp) qua.
- Hành vi chuông thông báo với SSE thật của backend.
- Backend không đổi mã nên không chạy `mvn verify`.

## Việc người vận hành cần làm sau khi bản này lên VPS

1. Cài lại script deploy, vì deploy không tự thay script đang chạy: `install -m 755 infra/deploy/platform-deploy.sh /usr/local/bin/platform-deploy` (production và staging dùng chung script).
2. Chạy một lần `CREATE EXTENSION IF NOT EXISTS pg_stat_statements` cho mỗi database: [runbook, mục Số đo hiệu năng](../../../docs/runbooks/deploy-vps.md).
3. Lần deploy đầu tạo lại container `postgres` và `redis` do đổi `command`: mọi phiên đăng nhập mất (Redis không lưu đĩa), database gián đoạn vài giây.
