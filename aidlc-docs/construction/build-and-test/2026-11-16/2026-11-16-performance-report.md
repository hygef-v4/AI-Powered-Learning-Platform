# Báo cáo hiệu năng production (alapedu.online)

- Ngày đo: 2026-11-16, khoảng 13:15–13:20 UTC (20:15 giờ VN), lúc hệ thống gần như không có người dùng.
- Phạm vi: site production từ bên ngoài, VPS `vps-alapedu` (chỉ đọc), mã nguồn liên quan.
- Bản đang chạy: `e3825aa6` (production), `d8ac7d21` (staging, cùng VPS).
- Không thay đổi gì trên VPS, không đổi mã ứng dụng.

## Kết luận ngắn

Ở tải hiện tại hệ thống nhanh và còn dư tài nguyên. Không có điểm nghẽn nào đang gây chậm cho người dùng. Có 3 việc đáng làm, xếp theo lợi ích:

1. Mỗi lần deploy gây lỗi 502 khoảng 1–2 phút vì backend khởi động mất 73 giây và không có bước chờ.
2. Không có số đo thời gian phản hồi nào được ghi lại, nên không thể biết API nào chậm khi có tải thật.
3. Trang đăng nhập tải 1,26 MB JavaScript vì toàn bộ feature được nạp ở root layout; chuông thông báo vừa giữ SSE vừa polling 30 giây, chiếm 39% tổng số request.

## Giới hạn của lần đo

- Chưa đo các trang sau đăng nhập (kể cả trang bài tập trong liên kết được gửi). Tôi không tự nhập mật khẩu vào site production. Số liệu trình duyệt bên dưới là của trang `/login`.
- Chưa đo thời gian phản hồi từng API có xác thực: nginx không ghi `$request_time`, actuator yêu cầu đăng nhập, Postgres không bật `pg_stat_statements`.
- Chưa chạy load test. Mọi nhận định về sức chịu tải là suy ra từ cấu hình, không phải đo.
- Đo từ một máy ở Việt Nam, mạng gần VPS (kết nối TCP khoảng 12 ms).

## 1. Đo từ bên ngoài

| Đường dẫn | TTFB (5 lần, kết nối ấm) | Kích thước gzip |
|---|---|---|
| `/login` | 64–92 ms | 6,7 KB |
| `/` | 70–74 ms | 4,7 KB |
| `/api/v1/health` (401) | 40–64 ms | 120 B |

Lần gọi đầu tiên mất 1,35 giây, trong đó 1,1 giây là thiết lập TCP từ máy đo, không phải thời gian server.

Trang `/login` trong trình duyệt (cache trống, HTTP/2):

| Chỉ số | Giá trị |
|---|---|
| TTFB | 196 ms |
| DOMContentLoaded | 395 ms |
| Load | 1 056 ms |
| CLS | 0 |
| Số tài nguyên | 37 (25 script, 3 CSS) |
| Dung lượng truyền | 458 KB |
| Dung lượng sau giải nén | 1,26 MB (JavaScript 1,05 MB) |

Trình duyệt không trả về số FCP/LCP trong lần đo này.

Cấu hình truyền tải:

- HTTP/2 bật, TLS 1.2/1.3. Không có HTTP/3.
- Gzip hoạt động (do Next.js nén, nginx không bật `gzip`). Không có Brotli.
- `/_next/static/*`: `Cache-Control: public, max-age=31536000, immutable`. Đúng.
- Font trong `/fonts/*`: `Cache-Control: public, max-age=0`. Mỗi lần tải trang trình duyệt phải hỏi lại 2 font được preload (27 KB mỗi file).
- Cloudflare chỉ làm DNS, không có CDN đứng trước. Với người dùng trong nước thì không ảnh hưởng.

## 2. VPS

| Hạng mục | Giá trị |
|---|---|
| CPU | 6 vCPU Xeon Gold 6133, load 0,62 / 0,58 / 0,64 |
| CPU steal | 3–5% (máy chủ vật lý bị chia sẻ) |
| RAM | 5,9 GB, dùng 2,9 GB, còn khả dụng 3,1 GB |
| Swap | 4 GB, gần như không dùng |
| Đĩa | 79 GB, dùng 19 GB (25%) |
| OOM kill | 0 |
| Uptime | 10 ngày |

Container production (bộ nhớ dùng / giới hạn, CPU giới hạn):

| Container | RAM | CPU |
|---|---|---|
| backend | 392 MB / 1 GB | 1,0 |
| worker | 417 MB / 1,5 GB | 1,0 |
| frontend | 60 MB / 512 MB | 0,5 |
| postgres | 54 MB / 1 GB | 1,0 |
| rabbitmq | 88 MB / 512 MB | 0,5 |
| redis | 3,5 MB / 256 MB | – |
| nginx | 11 MB / 128 MB | 0,25 |

Không container nào restart. Staging chạy chung máy và dùng thêm khoảng 1,05 GB RAM.

Lưu ý:

- Tổng giới hạn RAM của production (khoảng 5 GB) cộng staging (khoảng 2,5 GB) vượt 5,9 GB RAM thật. Hiện chưa sao vì mức dùng thực tế thấp, nhưng nếu cả hai JVM production cùng dùng hết heap thì máy sẽ swap.
- 61 image Docker, 11,7 GB, chỉ 10 image đang dùng. Chưa gây vấn đề vì đĩa còn 57 GB.
- `rabbitmqctl list_queues` bị timeout 60 giây với giới hạn 0,5 CPU. Healthcheck của RabbitMQ đã phải nới timeout lên 20 giây vì cùng lý do. Chưa xác minh được độ dài hàng đợi.

## 3. Backend (Spring Boot, Java 17)

- Khởi động mất **73 giây** (`Started PlatformApplication in 73.281 seconds`).
- JVM chạy với `-XX:MaxRAMPercentage=75`: heap tối đa 768 MB trong container 1 GB. Vì container chỉ có 1 CPU, JVM tự chọn **SerialGC** và 2 luồng JIT.
- Không cấu hình riêng cho Hikari, Tomcat hay nén response, nên dùng mặc định: pool 10 kết nối, 200 luồng Tomcat, JSON từ API không nén.
- `open-in-view: false`. Đúng.
- Log 7 giờ gần nhất: 0 ERROR, 2 WARN.

## 4. Postgres, Redis

Postgres 16, database 14 MB, 55 bảng:

- Tỉ lệ cache hit 99,99%, 0 deadlock, 0 file tạm, 21 kết nối (1 active).
- `shared_buffers` 128 MB, `work_mem` 4 MB, `random_page_cost` 4: đều là mặc định. Với 14 MB dữ liệu thì chưa cần chỉnh.
- `log_min_duration_statement = -1` và không có `pg_stat_statements`: không ghi lại truy vấn chậm.
- Bảng có nhiều seq scan nhất: `notifications` (36 427 lần, 2,75 triệu dòng đọc), `assignments` (41 571 lần), `ai_tasks` (30 097 lần). Các bảng này chỉ có vài chục đến vài trăm dòng nên seq scan là lựa chọn đúng của planner. Cần xem lại khi `notifications` lớn lên, vì nó được truy vấn mỗi 30 giây cho mỗi tab đang mở.

Redis: 1,2 MB, `keyspace_hits = 0`, `keyspace_misses = 416`. Redis không được dùng làm cache đọc, nên mọi request đều đi thẳng vào Postgres. `maxmemory` chưa đặt, policy `noeviction`.

## 5. Lưu lượng thực tế (log nginx, 05–16/11, 46 300 request)

| Nhóm | Số request | Ghi chú |
|---|---|---|
| `/api/v1/me/notifications/unread-count` | 17 926 (39%) | polling 30 giây |
| `/wp-admin/install.php` | 1 809 | bot dò quét, được chuyển tới Next.js |
| `/api/v1/me/notifications/events` | 1 568 | SSE, 608 lần kết thúc bằng 499 |
| `/api/v1/auth/refresh` | 1 161 | |
| 502 | 352 | xem mục 6.1 |

Ngày cao nhất là 10/11 với 9 160 request, tức trung bình khoảng 0,1 request/giây. Tải này rất thấp so với năng lực máy.

## 6. Vấn đề và đề xuất

### 6.1 Deploy gây 502 (ưu tiên cao)

352 lỗi 502 rơi đúng vào các giờ có deploy (09–13/11, 15/11, 16/11 lúc 06 giờ UTC, trùng lúc backend khởi động lại 06:25). Backend mất 73 giây để sẵn sàng và nginx không có gì để trả trong lúc đó.

Đề xuất:

- Tăng CPU của backend lên 2 trong lúc khởi động, hoặc đặt hẳn `cpus: 2`. Máy có 6 vCPU, load 0,6. Việc này cũng làm JVM chọn G1 thay cho SerialGC.
- Đổi thứ tự deploy: khởi động container mới, chờ healthy, rồi mới chuyển lưu lượng. Nếu giữ một bản sao thì ít nhất thêm trang bảo trì cho 502 ở nginx.
- Xem xét `spring.main.lazy-initialization` hoặc CDS/AppCDS để rút thời gian khởi động.

### 6.2 Không có số đo thời gian phản hồi (ưu tiên cao)

Hiện không trả lời được câu "API nào chậm". Đề xuất:

- Thêm `$request_time` và `$upstream_response_time` vào `log_format` của nginx.
- Bật `pg_stat_statements` và đặt `log_min_duration_statement = 500`.
- Cho phép đọc `/actuator/prometheus` từ mạng nội bộ Docker để lấy `http.server.requests`.

### 6.3 Bundle trang đăng nhập (ưu tiên trung bình)

`frontend/src/app/layout.tsx` và `frontend/src/app/ClientRegistrations.tsx` đều import `@/features/register`, file này nạp phần đăng ký của cả 17 feature. Kết quả là trang `/login` tải 25 script, 1,05 MB JavaScript. Trên mạng nhanh mất 1 giây; trên 3G/4G yếu sẽ lâu hơn nhiều.

Đề xuất: chỉ nạp registry trong layout của vùng đã đăng nhập, hoặc để mỗi `register.ts` chỉ khai báo slot và `dynamic import` component thật.

### 6.4 Polling thông báo trùng với SSE (ưu tiên trung bình)

`frontend/src/features/notifications/components/NotificationBell.tsx:58` gọi `unread-count` mỗi 30 giây kể cả khi SSE đang kết nối tốt. Mỗi tab mở tạo 2 880 request/ngày. Với 500 người dùng cùng lúc là khoảng 17 request/giây chỉ để đếm thông báo, mỗi request một truy vấn Postgres.

Đề xuất: chỉ polling khi SSE lỗi, và dừng khi tab bị ẩn (`document.visibilityState`).

### 6.5 Việc nhỏ

- Đặt `Cache-Control` dài hạn cho `/fonts/*` (thêm `location /fonts/` ở nginx hoặc `headers()` trong Next config).
- Bật `gzip` cho `application/json` và `application/problem+json` ở nginx; hiện JSON từ backend không nén.
- Trả 404 ngay ở nginx cho `/wp-admin`, `/wp-login.php`, `*.php` để bot không chạm tới Next.js.
- Đặt `maxmemory` và policy cho Redis.
- `docker image prune -a` định kỳ để thu hồi image cũ (khoảng 51 image không dùng).
- Timer deploy production chạy khoảng 2 phút một lần; mỗi lần là một `git fetch`. Không ảnh hưởng hiệu năng, chỉ ghi nhận.

## 7. Bước tiếp theo để có số liệu đầy đủ

1. Đo các trang sau đăng nhập: cần người dùng đăng nhập sẵn trong trình duyệt, hoặc đo trên staging.
2. Thực hiện mục 6.2 rồi thu số liệu một tuần.
3. Load test trên staging (k6 hoặc tương đương) với kịch bản 50–200 người dùng đồng thời để tìm giới hạn thật của cấu hình 1 CPU / pool 10 kết nối.
