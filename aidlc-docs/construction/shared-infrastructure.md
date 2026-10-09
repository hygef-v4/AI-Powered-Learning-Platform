# Shared Infrastructure

Hạ tầng dùng chung cho cả 16 unit. Chốt tại U01 Infrastructure Design; khung dự án (Docker Compose, CI) do U03 dựng vì U03 code đầu tiên (2026-10-04); unit sau chỉ bổ sung phần riêng của mình.

## 1. Quyết định

| Hạng mục | Chọn | Nguồn |
|---|---|---|
| Production | VPS Linux nhóm đã có, chạy Docker Compose | Câu I1 |
| Local/test | Cùng file Compose, thêm Mailpit, tắt Nginx TLS | NFR-005 |
| Reverse proxy, HTTPS | Nginx + certbot (Let's Encrypt); tên miền có sẵn hoặc subdomain DuckDNS miễn phí | Câu I6 |
| Database | PostgreSQL container; user `migrator` cho Flyway, user `app` cho runtime (tạo ở khung dự án, plan U03 K5); `app` không sửa/xóa `audit_logs` | U03, U02 |
| Cache, phiên, rate limit | Redis container; 12 nhóm key đặt tên theo mục đích: `otp:*`, `ticket:*`, `session:refresh:*`, `file:download-token:*`, `gemini:daily-cost:*`, `email:daily-count:*` và 6 nhóm `ratelimit:*` (`auth`, `payos-webhook`, `attempt-save`, `section-save`, `ai-request`, `code-try`). Mọi key có TTL; mất Redis chỉ làm đăng xuất và reset bộ đếm | U01, U03, U07, U11, U13, U14, U16 |
| Queue | RabbitMQ container, vhost `/platform`, user `app`, tắt `guest`. Exchange: `jobs` (direct), `jobs.retry` (direct), `platform.events` (topic, chỉ cho thông báo) do U03 khai báo; `platform.realtime` (fanout, U14 tạo, U16 dùng chung cho SSE). Queue: 7 queue việc nền (`jobs.triggered`, `jobs.email`, `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.drive`, `jobs.payos`) cùng 5 queue thử lại `jobs.retry.*` (U03); `jobs.notification` (U16); mỗi backend một queue tạm `jobs.realtime.{instanceId}` (U14). Audit không đi qua RabbitMQ | Câu I2, U03, U14, U16 |
| Quan sát | `docker compose logs` + healthcheck; không có monitoring stack | Rút gọn phạm vi đồ án |
| Secret | Biến môi trường trong CI/CD, ghi ra file `.env` quyền 600 trên VPS khi deploy | Câu I4 |
| Registry image | GitHub Container Registry (miễn phí với repo public), tag theo commit SHA, không dùng `latest` | NFR-005 |
| Email | Mailpit khi dev; Brevo SMTP khi demo/production (SMTP login + SMTP key; dùng trong hạn mức gói miễn phí) | REL-005 |
| Backup | **Không có** (ngoại lệ) | Câu I5 |
| Mã hóa at rest | **Không có** (ngoại lệ) | Câu I7 |

## 2. Container

| Container | Image | Cổng public | Giới hạn | Healthcheck |
|---|---|---|---|---|
| `nginx` | nginx + certbot | 80, 443 | 0.25 CPU, 128 MB | Trả `200` trên `/nginx-health` |
| `frontend` | Next.js | Không | 0.5 CPU, 512 MB | `/api/health` |
| `backend` | Spring Boot | Không | 1 CPU, 1 GB | `/health/ready` |
| `worker` | Spring Boot (profile worker) | Không | 1 CPU, 1,5 GB (VPS < 8 GB: 1 GB và `U05_SCAN_CONCURRENCY=2`); 12 luồng xử lý việc nền chia theo 7 queue (U03 P11), cộng listener `jobs.notification` của U16 và các scanner mỗi phút | `/health/ready` |
| `postgres` | `pgvector/pgvector:pg16` (PostgreSQL 16 + pgvector) | Không | 1 CPU, 1 GB | `pg_isready` |
| `redis` | redis, phiên bản cố định, `requirepass` | Không | 0.25 CPU, 256 MB | `redis-cli ping` |
| `rabbitmq` | rabbitmq, phiên bản cố định | Không | 0.5 CPU, 512 MB | `rabbitmq-diagnostics ping` |
| `mailpit` | chỉ local | 8025 ở local | - | - |
| `judge0-server` | `judge0/judge0:1.13.1` | Không | 0.5 CPU, 512 MB | `GET /languages` |
| `judge0-workers` | `judge0/judge0:1.13.1` | Không | 1.5 CPU, 1,5 GB | - |
| `judge0-db` | `postgres:16-alpine` | Không | 0.25 CPU, 256 MB | `pg_isready` |
| `judge0-redis` | `redis:7-alpine` | Không | 0.1 CPU, 128 MB | `redis-cli ping` |

VPS gợi ý: 4 vCPU, 8 GB RAM, 60 GB SSD (có Judge0). VPS nhỏ hơn: xem U13 infrastructure-design §5.

## 3. Mạng

- Ba mạng Docker: `edge` (nginx ↔ frontend, backend), `internal` (backend, worker ↔ postgres, redis, rabbitmq) và `sandbox` (`internal: true`, không ra Internet: backend, worker ↔ 4 container Judge0). Datastore không nằm trong `edge`; Judge0 không chạm được datastore của hệ thống.
- Judge0 chạy `privileged: true` (isolate cần cgroup); không mount thư mục host, tắt mạng cho submission.
- Chỉ Nginx publish cổng 80/443. Cổng 80 chỉ chuyển hướng sang 443 và phục vụ ACME của certbot.
- Firewall VPS: chặn mọi cổng trừ 80, 443 và SSH. SSH chỉ bằng khóa, tắt đăng nhập mật khẩu và root.
- RabbitMQ management **không public**; truy cập qua SSH tunnel.
- Header của SEC-004: Nginx thêm HSTS 1 năm, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin` cho mọi phản hồi, và CSP `default-src 'self'; frame-src https://www.youtube-nocookie.com https://embed.diagrams.net` cho `/api/*`. CSP của trang do Next.js tự gắn (`frontend/src/proxy.ts`): `default-src 'self'`, script/style chỉ chạy khi có nonce của request, cùng `frame-src` YouTube và Draw.io; CSP tĩnh `default-src 'self'` chặn script inline của Next.js nên trang không chạy được (phát hiện khi chạy thử stack 2026-10-05).
- Định tuyến: `/api/*` → backend, còn lại → frontend.
- Webhook PayOS `/api/v1/payments/payos/webhook`: `client_max_body_size 16k`.
- Lưu nháp bài làm `PUT /api/v1/attempts/*/content` và mục bài nhóm `PUT /api/v1/group-docs/*/sections/*/draft`: `client_max_body_size 12m`.
- SSE tài liệu nhóm `GET /api/v1/group-docs/*/events` và chuông thông báo `GET /api/v1/me/notifications/stream`: `proxy_buffering off`, `proxy_read_timeout 1h`, `proxy_http_version 1.1`.
- Nhập DOCX `/api/v1/assignments/*/skeleton:import-docx` (khung giảng viên) và `/api/v1/attempts/*/docx:preview` (người học, bài DOCUMENT): `client_max_body_size 20m`, `proxy_read_timeout 60s`.
- `client_max_body_size 50m`; riêng `/api/v1/files` tắt đệm request (`proxy_request_buffering off`) và `proxy_read_timeout 120s`.
- Backend và worker cần kết nối ra `www.googleapis.com:443` (Google Drive, YouTube Data API), `generativelanguage.googleapis.com:443` (Gemini), `www.youtube.com:443` (caption), `api-merchant.payos.vn:443` (PayOS) và `smtp-relay.brevo.com:587` (Brevo SMTP, STARTTLS); firewall chỉ chặn chiều vào.

## 4. Secret

- CI/CD giữ: `JUDGE0_AUTH_TOKEN`, `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`, `APP_PUBLIC_URL`, `GEMINI_API_KEY`, `YOUTUBE_API_KEY`, `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`, `GOOGLE_SHARED_DRIVE_ID`, `U01_JWT_SECRET`, `POSTGRES_PASSWORD`, `POSTGRES_MIGRATOR_PASSWORD`, `POSTGRES_APP_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_PASSWORD`, `SMTP_*`.
- Khi deploy, pipeline ghi `.env` qua SSH, quyền `600`, chủ là user deploy. Không commit, không in ra log pipeline.
- Xoay secret thủ công: đổi trong CI/CD rồi deploy lại. Đổi `U01_JWT_SECRET` làm mọi access token hiện có mất hiệu lực.

## 5. Triển khai và rollback

1. Push nhánh `main` → CI chạy test (gồm Testcontainers) → build image, gắn tag SHA → đẩy lên GHCR.
2. CI ghi `.env` và `IMAGE_TAG` lên VPS qua SSH.
3. `docker compose pull && docker compose up -d`. Downtime ngắn khi thay container (direct/in-place theo REL-001).
4. Migration Flyway chạy lúc backend khởi động, phải tương thích ngược (REL-001).
5. Rollback: đặt `IMAGE_TAG` về SHA trước rồi `up -d`. Migration phá vỡ phải có kịch bản khôi phục riêng.

## 6. Quan sát

- Xem log: `docker compose logs -f <container>`. Docker giới hạn mỗi container 10 MB x 3 file log.
- Healthcheck trong Compose; `docker compose ps` cho biết container nào không healthy.
- Không có metric, dashboard, log tập trung hay cảnh báo tự động (ngoài phạm vi đồ án).

## 7. Ngoài phạm vi đồ án

Theo phạm vi rút gọn ở `requirements.md` mục 12-13: không multi-zone, không backup, không mã hóa at rest, không TLS giữa các container, không monitoring/alerting. Hệ quả cần biết: VPS hỏng thì hệ thống dừng và **mất toàn bộ dữ liệu**.

## 8. Email provider - Brevo (quyết định 2026-10-04)

- Mail Port của U01/U16 dùng Brevo SMTP cho OTP và email thông báo ở demo/production; local/test tiếp tục dùng Mailpit.
- Cấu hình: SMTP_HOST=smtp-relay.brevo.com, SMTP_PORT=587, SMTP_USER là SMTP login trong Brevo, SMTP_PASSWORD là SMTP key (không phải API key), SMTP_STARTTLS_ENABLED=true, SMTP_FROM là địa chỉ người gửi đã xác minh trong Brevo. Các secret chỉ lấy từ môi trường/secret store.
- Trần email thông báo của U16 là giới hạn ứng dụng, không phải cam kết quota nhà cung cấp. OTP và thông báo cùng sử dụng quota của tài khoản Brevo; cấu hình trần thông báo phải chừa dung lượng cho OTP và phù hợp hạn mức tài khoản. Khi provider từ chối hoặc hết quota, áp dụng timeout/retry hữu hạn đã chốt, không ghi nhận SENT giả.
- SENT chỉ nghĩa là SMTP chấp nhận gửi; không bảo đảm thư đã tới hộp thư người nhận. Không thêm tích hợp delivery webhook trong thay đổi này.
- Tham chiếu cấu hình: [Brevo SMTP](https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP).
