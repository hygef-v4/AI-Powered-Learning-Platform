# U07 Payment & AI Credit - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller mua/ví, `WebhookController`, `CreditPortService` | `backend` |
| `PaymentScanner` (scanner U03), `PayosCheckHandler` | `worker` |
| Bảng `credit_packages`, `payments`; cột số dư trong `accounts` (U01 tạo) | `postgres` |
| Rate limit webhook | `redis`, khóa `ratelimit:payos-webhook:{ip}` |
| Queue | `jobs.payos` (`PAYOS_CHECK`); phát `payment.paid` lên exchange `platform.events` (chỉ cho thông báo U16); trả credit giữ quá hạn do scanner của U13 |

## 2. PayOS

| Mục | Giá trị |
|---|---|
| Tài khoản | Đăng ký PayOS miễn phí, liên kết tài khoản ngân hàng của nhóm, tạo kênh thanh toán lấy 3 key |
| Webhook | `https://<domain>/api/v1/payments/payos/webhook`, đăng ký trong trang quản lý PayOS |
| Kết nối ra | Backend, worker tới `api-merchant.payos.vn:443` |
| Thử nghiệm | Gói 2 000đ, chuyển khoản thật vào tài khoản của nhóm (không mất tiền); local dùng adapter giả |

## 3. Nginx

- `location = /api/v1/payments/payos/webhook`: `client_max_body_size 16k`, không cần cookie; header `X-Forwarded-For` để rate limit đúng IP.

## 4. Migration

`V20260925_1400__u07_payment_credit.sql`:
- `credit_packages`, `payments` theo [database](../../../../docs/database.md). Unique: `payments.order_code`, `payments.idempotency_key`; index `payments (account_id, created_at)`, `(status, created_at)` cho scanner.
- `ALTER TABLE accounts ADD COLUMN free_balance bigint NOT NULL DEFAULT 0, free_period char(7), purchased_balance bigint NOT NULL DEFAULT 0` kèm `CHECK (free_balance >= 0 AND purchased_balance >= 0)` (bảng do U01 tạo; chỉ U07 ghi các cột này).
- Kiểu: tiền `bigint`, credit `bigint`.
- Quyền: `REVOKE DELETE ON payments FROM app`.
- Gói nạp từ `U07_PACKAGES` khi khởi động; mức tặng tháng và tỷ lệ token đọc từ biến môi trường, không có bảng cấu hình.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | Webhook qua HTTPS |
| SECURITY-09 | Compliant | 3 key PayOS trong secret CI/CD |
| RESILIENCY-04 | Compliant | Deploy cùng Compose |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
