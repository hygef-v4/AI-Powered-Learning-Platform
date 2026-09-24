# U07 Payment & AI Credit - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller mua/ví/admin, `WebhookController`, `CreditPortService` | `backend` |
| `ReconcileHandler`, `ReservationSweepHandler` | `worker` |
| Bảng `credit_packages`, `payments`, `payment_webhook_events`, `credit_wallets`, `credit_ledger`, `credit_reservations`, `credit_settings` | `postgres` |
| Rate limit webhook | `redis`, khóa `u07:webhook:{ip}` |
| Queue | `jobs.u07.reconcile`, `jobs.u07.reservation-sweep` |

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
- Unique: `payments.order_code`, `(account_id, idempotency_key)`, `payment_webhook_events.event_key`, `credit_reservations.request_ref`, partial unique `credit_ledger (ref_id) WHERE type = 'PURCHASE'`.
- Kiểu: tiền `bigint`, credit `bigint`.
- Quyền: `REVOKE UPDATE, DELETE ON credit_ledger, payment_webhook_events FROM app`.
- `credit_settings` một dòng: `monthly_free_credits` khởi tạo từ `U07_MONTHLY_FREE_CREDITS`.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | Webhook qua HTTPS |
| SECURITY-09 | Compliant | 3 key PayOS trong secret CI/CD |
| RESILIENCY-04 | Compliant | Deploy cùng Compose |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
