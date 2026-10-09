# U07 Payment & AI Credit - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 08, 09, 10, 11, 68, 69, 72 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); mục credit tặng định kỳ của Settings (UC 70–71); primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

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

`db/migration/billing/V20260925_1400__create_credit_packages_payments.sql` (chưa áp dụng nên gộp luôn phần sửa 2026-10-08):
- `credit_packages` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md): `information`, `active`, `version` mặc định 0, `created_at`, `updated_at`, `updated_by` (FK → `accounts`); CHECK `price_vnd > 0`, `credits > 0`.
- `payments`: snapshot `credits`, `amount_vnd`, `package_name`, `package_information`; unique `order_code`, `idempotency_key`; index `(account_id, created_at)`, `(status, created_at)` cho scanner, `(package_id, created_at)` cho Payment History.
- `ALTER TABLE accounts ADD COLUMN free_balance bigint NOT NULL DEFAULT 0, free_period char(7), purchased_balance bigint NOT NULL DEFAULT 0` kèm `CHECK (free_balance >= 0 AND purchased_balance >= 0)` (bảng do U01 tạo; chỉ U07 ghi các cột này).
- Kiểu: tiền `bigint`, credit `bigint`.
- Quyền: `REVOKE DELETE ON payments FROM app`.
- Gói nạp từ `U07_PACKAGES` khi khởi động (chỉ INSERT gói thiếu); mức tặng định kỳ là mục `credit.monthlyFreeCredits` trong `system_settings` của U03; tỷ lệ token đọc từ biến môi trường.

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-04 | Compliant | Webhook qua HTTPS |
| SECURITY-09 | Compliant | 3 key PayOS trong secret CI/CD |
| RESILIENCY-04 | Compliant | Deploy cùng Compose |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
