# U01 Account & Access - Deployment Architecture

## 1. Production (một VPS)

```
                 Internet
                    |
             80/443 | (firewall: chỉ 80, 443, SSH)
                    v
 +----------------------- VPS (Docker Compose) -----------------------+
 |                                                                    |
 |  [nginx + certbot] --- mạng edge ---+---> [frontend]               |
 |                                     +---> [backend]  (u01 ...)     |
 |                                              |                     |
 |                        mạng internal         |                     |
 |        +-------------------+-----------------+-----------+         |
 |        v                   v                             v         |
 |   [postgres]           [redis]                      [rabbitmq]     |
 |        ^                   ^                             |         |
 |        |                   |                             v         |
 |        +-------------------+--------------------- [worker] --------+--> SMTP bên ngoài
 |                                                                    |
 +--------------------------------------------------------------------+
```

**Text alternative**: Internet chỉ vào được cổng 80/443 của Nginx. Nginx nằm trên mạng `edge` cùng frontend và backend. Backend và worker nằm trên mạng `internal` cùng PostgreSQL, Redis và RabbitMQ; ba datastore này không có cổng public. Worker đọc RabbitMQ và gửi mail tới SMTP bên ngoài.

## 2. Local/demo

Cùng `docker-compose.yml` với file override `docker-compose.local.yml`:
- Nginx chạy HTTP, cookie tắt `Secure` bằng profile `local`.
- Thêm `mailpit` (giao diện `localhost:8025`) thay SMTP thật.

## 3. Luồng đăng nhập qua hạ tầng

1. Trình duyệt → Nginx (TLS) → backend `/api/v1/auth/login`.
2. Backend: `RateLimitFilter` → Redis; kiểm mật khẩu → PostgreSQL; tạo refresh → Redis; trả cookie.
3. Request sau: Nginx → backend, `JwtAuthFilter` chỉ kiểm chữ ký, không gọi datastore.

## 4. Luồng OTP qua hạ tầng

1. Trình duyệt → backend `/api/v1/auth/activation-requests` → ghi outbox trong PostgreSQL → trả `202`.
2. Relay của U02 chuyển bản ghi outbox sang RabbitMQ `u01.otp-delivery`.
3. Worker nhận, sinh mã, ghi băm vào Redis, gửi SMTP.
4. Lỗi → queue retry; hết lượt → DLQ + log ERROR.

## 5. Khôi phục khi VPS lỗi

1. Dựng VPS mới, cài Docker, mở firewall.
2. Chạy lại pipeline deploy với tag SHA đang dùng.
3. Trỏ DNS sang IP mới; certbot cấp lại chứng chỉ.
4. **Dữ liệu cũ không khôi phục được** vì không có backup (ngoại lệ REL-004). Tài khoản phải được admin tạo/nhập lại.
