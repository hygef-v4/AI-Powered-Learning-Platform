# Shared Infrastructure

Hạ tầng dùng chung cho cả 16 unit. Chốt tại U01 Infrastructure Design; unit sau chỉ bổ sung phần riêng của mình.

## 1. Quyết định

| Hạng mục | Chọn | Nguồn |
|---|---|---|
| Production | Một VPS Linux chạy Docker Compose | Câu I1 |
| Local/demo | Cùng file Compose, thêm Mailpit, tắt Nginx TLS | NFR-005 |
| Reverse proxy, HTTPS | Nginx + certbot (Let's Encrypt) | Câu I6 |
| Database | PostgreSQL container | ERD |
| Cache, phiên, rate limit | Redis container | ERD |
| Queue | RabbitMQ container | Câu I2 |
| Quan sát | Prometheus + Grafana + Loki + Promtail | Câu I3 |
| Secret | Biến môi trường trong CI/CD, ghi ra file `.env` quyền 600 trên VPS khi deploy | Câu I4 |
| Registry image | GitHub Container Registry, tag theo commit SHA, không dùng `latest` | NFR-005 |
| Backup | **Không có** (ngoại lệ) | Câu I5 |
| Mã hóa at rest | **Không có** (ngoại lệ) | Câu I7 |

## 2. Container

| Container | Image | Cổng public | Giới hạn | Healthcheck |
|---|---|---|---|---|
| `nginx` | nginx + certbot | 80, 443 | 0.25 CPU, 128 MB | Trả `200` trên `/nginx-health` |
| `frontend` | Next.js | Không | 0.5 CPU, 512 MB | `/api/health` |
| `backend` | Spring Boot | Không | 1 CPU, 1 GB | `/health/ready` |
| `worker` | Spring Boot (profile worker) | Không | 0.5 CPU, 768 MB | `/health/ready` |
| `postgres` | postgres, phiên bản cố định | Không | 1 CPU, 1 GB | `pg_isready` |
| `redis` | redis, phiên bản cố định, `requirepass` | Không | 0.25 CPU, 256 MB | `redis-cli ping` |
| `rabbitmq` | rabbitmq, phiên bản cố định | Không | 0.5 CPU, 512 MB | `rabbitmq-diagnostics ping` |
| `prometheus` | prometheus | Không | 0.25 CPU, 512 MB | `/-/healthy` |
| `loki` + `promtail` | grafana/loki, promtail | Không | 0.5 CPU, 512 MB | `/ready` |
| `grafana` | grafana | Không | 0.25 CPU, 256 MB | `/api/health` |
| `mailpit` | chỉ local | 8025 ở local | - | - |

VPS tối thiểu gợi ý: 4 vCPU, 8 GB RAM, 80 GB SSD.

## 3. Mạng

- Hai mạng Docker: `edge` (nginx ↔ frontend, backend) và `internal` (backend, worker ↔ postgres, redis, rabbitmq, loki). Datastore không nằm trong `edge`.
- Chỉ Nginx publish cổng 80/443. Cổng 80 chỉ chuyển hướng sang 443 và phục vụ ACME của certbot.
- Firewall VPS: chặn mọi cổng trừ 80, 443 và SSH. SSH chỉ bằng khóa, tắt đăng nhập mật khẩu và root.
- Grafana, Prometheus, RabbitMQ management **không public**; truy cập qua SSH tunnel.
- Nginx thêm header của SEC-004: CSP `default-src 'self'`, HSTS 1 năm, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`.
- Định tuyến: `/api/*` → backend, còn lại → frontend.

## 4. Secret

- CI/CD giữ: `U01_JWT_SECRET`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_PASSWORD`, `SMTP_*`, `GRAFANA_ADMIN_PASSWORD`.
- Khi deploy, pipeline ghi `.env` qua SSH, quyền `600`, chủ là user deploy. Không commit, không in ra log pipeline.
- Xoay secret thủ công: đổi trong CI/CD rồi deploy lại. Đổi `U01_JWT_SECRET` làm mọi access token hiện có mất hiệu lực.

## 5. Triển khai và rollback

1. Push nhánh `main` → CI chạy test (gồm Testcontainers) → build image, gắn tag SHA → đẩy lên GHCR.
2. CI ghi `.env` và `IMAGE_TAG` lên VPS qua SSH.
3. `docker compose pull && docker compose up -d`. Downtime ngắn khi thay container (direct/in-place theo REL-004).
4. Migration Flyway chạy lúc backend khởi động, phải tương thích ngược (REL-004).
5. Rollback: đặt `IMAGE_TAG` về SHA trước rồi `up -d`. Migration phá vỡ phải có kịch bản khôi phục riêng.

## 6. Quan sát

- Backend và worker phơi `/actuator/prometheus`; Prometheus scrape mỗi 15 giây.
- Promtail đọc log container, đẩy vào Loki. Giữ log 90 ngày (SEC-005).
- Grafana alerting gửi email tới nhóm khi: container không healthy > 2 phút; đĩa, RAM hoặc CPU > 80%; chứng chỉ TLS còn < 14 ngày; các cảnh báo nghiệp vụ của từng unit.

## 7. Ngoại lệ được chấp nhận

| Yêu cầu | Không đạt | Hệ quả |
|---|---|---|
| REL-002, REL-006, RESILIENCY-08 | Không multi-zone, một VPS | VPS lỗi thì toàn hệ thống dừng tới khi dựng lại thủ công |
| REL-008, RESILIENCY-11, RESILIENCY-12 | Không backup | VPS mất hoặc dữ liệu bị xóa thì **mất toàn bộ dữ liệu vĩnh viễn** |
| SEC-001, SECURITY-01 | Không mã hóa at rest; container nói chuyện không TLS trên mạng Docker nội bộ | Ai có quyền truy cập đĩa hoặc host đọc được dữ liệu |
| SEC-005 (log tamper-evident) | Loki trên cùng VPS, không append-only thật | Người có quyền root sửa được log |
