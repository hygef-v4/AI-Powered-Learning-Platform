# Shared Infrastructure

Hạ tầng dùng chung cho cả 16 unit. Chốt tại U01 Infrastructure Design; unit sau chỉ bổ sung phần riêng của mình.

## 1. Quyết định

| Hạng mục | Chọn | Nguồn |
|---|---|---|
| Production | VPS Linux nhóm đã có, chạy Docker Compose | Câu I1 |
| Local/demo | Cùng file Compose, thêm Mailpit, tắt Nginx TLS | NFR-005 |
| Reverse proxy, HTTPS | Nginx + certbot (Let's Encrypt); tên miền có sẵn hoặc subdomain DuckDNS miễn phí | Câu I6 |
| Database | PostgreSQL container; user `migrator` cho Flyway, user `app` cho runtime | ERD, U02 |
| Cache, phiên, rate limit | Redis container | ERD |
| Queue | RabbitMQ container, vhost `/platform`, user `app`, tắt `guest`; exchange `jobs`, `audit`, `platform.events` | Câu I2, U02 |
| Quan sát | `docker compose logs` + healthcheck; không có monitoring stack | Rút gọn phạm vi đồ án |
| Secret | Biến môi trường trong CI/CD, ghi ra file `.env` quyền 600 trên VPS khi deploy | Câu I4 |
| Registry image | GitHub Container Registry (miễn phí với repo public), tag theo commit SHA, không dùng `latest` | NFR-005 |
| Email | Mailpit khi dev; Gmail SMTP với App Password khi demo (~500 mail/ngày, miễn phí) | REL-005 |
| Backup | **Không có** (ngoại lệ) | Câu I5 |
| Mã hóa at rest | **Không có** (ngoại lệ) | Câu I7 |

## 2. Container

| Container | Image | Cổng public | Giới hạn | Healthcheck |
|---|---|---|---|---|
| `nginx` | nginx + certbot | 80, 443 | 0.25 CPU, 128 MB | Trả `200` trên `/nginx-health` |
| `frontend` | Next.js | Không | 0.5 CPU, 512 MB | `/api/health` |
| `backend` | Spring Boot | Không | 1 CPU, 1 GB | `/health/ready` |
| `worker` | Spring Boot (profile worker) | Không | 1 CPU, 1,5 GB (VPS < 8 GB: 1 GB và `U05_INGEST_CONCURRENCY=2`) | `/health/ready` |
| `postgres` | `pgvector/pgvector:pg16` (PostgreSQL 16 + pgvector) | Không | 1 CPU, 1 GB | `pg_isready` |
| `redis` | redis, phiên bản cố định, `requirepass` | Không | 0.25 CPU, 256 MB | `redis-cli ping` |
| `rabbitmq` | rabbitmq, phiên bản cố định | Không | 0.5 CPU, 512 MB | `rabbitmq-diagnostics ping` |
| `mailpit` | chỉ local | 8025 ở local | - | - |

VPS tối thiểu gợi ý: 2 vCPU, 4 GB RAM, 40 GB SSD.

## 3. Mạng

- Hai mạng Docker: `edge` (nginx ↔ frontend, backend) và `internal` (backend, worker ↔ postgres, redis, rabbitmq). Datastore không nằm trong `edge`.
- Chỉ Nginx publish cổng 80/443. Cổng 80 chỉ chuyển hướng sang 443 và phục vụ ACME của certbot.
- Firewall VPS: chặn mọi cổng trừ 80, 443 và SSH. SSH chỉ bằng khóa, tắt đăng nhập mật khẩu và root.
- RabbitMQ management **không public**; truy cập qua SSH tunnel.
- Nginx thêm header của SEC-004: CSP `default-src 'self'; frame-src https://www.youtube-nocookie.com https://embed.diagrams.net`, HSTS 1 năm, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`.
- Định tuyến: `/api/*` → backend, còn lại → frontend.
- Webhook PayOS `/api/v1/payments/payos/webhook`: `client_max_body_size 16k`.
- Nhập DOCX `/api/v1/assignments/*/skeleton:import-docx`: `client_max_body_size 20m`, `proxy_read_timeout 60s`.
- `client_max_body_size 50m`; riêng `/api/v1/files` tắt đệm request (`proxy_request_buffering off`) và `proxy_read_timeout 120s`.
- Backend và worker cần kết nối ra `www.googleapis.com:443` (Google Drive, YouTube Data API), `generativelanguage.googleapis.com:443` (Gemini), `www.youtube.com:443` (caption) và `api-merchant.payos.vn:443` (PayOS); firewall chỉ chặn chiều vào.

## 4. Secret

- CI/CD giữ: `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`, `APP_PUBLIC_URL`, `GEMINI_API_KEY`, `YOUTUBE_API_KEY`, `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`, `GOOGLE_SHARED_DRIVE_ID`, `U01_JWT_SECRET`, `POSTGRES_PASSWORD`, `POSTGRES_MIGRATOR_PASSWORD`, `POSTGRES_APP_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_PASSWORD`, `SMTP_*`.
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
