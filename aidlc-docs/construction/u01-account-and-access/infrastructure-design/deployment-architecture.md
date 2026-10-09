# U01 Account & Access - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 01, 02, 03, 04, 05, 06, 07, 59, 60, 61, 62, 63 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Production (một VPS)

```
                 Internet
                    |
             80/443 | (firewall: chỉ 80, 443, SSH)
                    v
 +----------------------- VPS (Docker Compose) -----------------------+
 |                                                                    |
 |  [nginx + certbot] --- mạng edge ---+---> [frontend]               |
 |                                     +---> [backend]  (identity ...)|
 |                                              |                     |
 |                        mạng internal         |                     |
 |        +-------------------+-----------------+-----------+         |
 |        v                   v                             v         |
 |   [postgres]           [redis]                      [rabbitmq]     |
 |        ^                   ^                             |         |
 |        |                   |                             v         |
 |        +-------------------+--------------------- [worker] --------+--> Brevo SMTP
 |                                                                    |
 +--------------------------------------------------------------------+
```

**Text alternative**: Internet chỉ vào được cổng 80/443 của Nginx. Nginx nằm trên mạng `edge` cùng frontend và backend. Backend và worker nằm trên mạng `internal` cùng PostgreSQL, Redis và RabbitMQ; ba datastore này không có cổng public. Worker đọc RabbitMQ và gửi mail tới Brevo SMTP.

## 2. Local/demo

Cùng `docker-compose.yml` với file override `docker-compose.local.yml`:
- Nginx chạy HTTP, cookie tắt `Secure` bằng profile `local`.
- Thêm `mailpit` (giao diện `localhost:8025`) thay SMTP thật.

## 3. Luồng đăng nhập qua hạ tầng

1. Trình duyệt → Nginx (TLS) → backend `/api/v1/auth/login`.
2. Backend: `RateLimitFilter` → Redis; kiểm mật khẩu → PostgreSQL; tạo refresh → Redis; trả cookie.
3. Request sau: Nginx → backend, `JwtAuthFilter` chỉ kiểm chữ ký, không gọi datastore.

## 4. Luồng OTP qua hạ tầng

1. Trình duyệt → backend `/api/v1/auth/activation-requests` → kiểm tài khoản, trả `202`.
2. U03 gửi `JobMessage` `OTP_DELIVERY` sang RabbitMQ ngay (yêu cầu không ghi database), vào queue `jobs.email`.
3. Worker nhận, sinh mã, ghi băm vào Redis, gửi SMTP.
4. Lỗi → U03 gửi lại qua queue thử lại theo backoff; hết lượt → log ERROR, người dùng yêu cầu OTP lại.

## 5. Khôi phục khi VPS lỗi

1. Dựng VPS mới, cài Docker, mở firewall.
2. Chạy lại pipeline deploy với tag SHA đang dùng.
3. Trỏ DNS sang IP mới; certbot cấp lại chứng chỉ.
4. **Dữ liệu cũ không khôi phục được** vì không có backup (ngoại lệ REL-004). Tài khoản phải được admin tạo/nhập lại.
