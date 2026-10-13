# U01 Account & Access — deployment-architecture

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 59, 60, 61, 62, 63. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u01-account-and-access` không quyết định chức năng hiện hành.

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
4. Khôi phục dữ liệu theo runbook U03/shared-infrastructure và backup hợp lệ; chưa đo RPO/RTO hoặc xác nhận restore thực tế.

## Quy tắc tích hợp revision hiện hành

- Tài khoản trường cấp, không public registration. Admin tạo/import không gửi OTP hoặc đặt mật khẩu; người dùng yêu cầu activation ở login đầu và tự đặt mật khẩu sau OTP.
- Account role cao nhất trong STUDENT/TEACHER/SUBJECT_MANAGER/ADMIN; authorization theo action/resource/current state, không chỉ role JWT. Admin chỉ quản trị, không học vụ.
- Profile cá nhân chỉ displayName/phoneNumber, email định danh/role/status bất biến trong /me. Reset không tiết lộ account tồn tại.
- MCP credentials xác thực actor qua U13 và cùng AuthorizationPort; không dùng provider API key làm ALAP authentication. Student không shared quota.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Tạo/import không gửi OTP, activation hợp lệ/sai/hết hạn/rate limit; recovery không lộ account.
- Đăng nhập/đăng xuất/phiên revoked; R1 own profile; Admin bị chặn học vụ; Student shared quota bị từ chối.
