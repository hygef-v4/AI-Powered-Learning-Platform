# U07 Payment & AI Credit - Logical Components

**Bản tài liệu 2026-10-09**: UC 08, 09, 10, 11, 68, 69, 72 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); mục credit tặng định kỳ của Settings (UC 70–71); primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Sơ đồ

```
 Trình duyệt                 PayOS (Internet)              U13 (nội bộ)
   |  mua, xem credit          |  webhook   ^ tạo link/tra cứu     |
   v                           v            |                      v
 +--------------------------------- backend -----------------------------------+
 | PaymentController --> PaymentService --> PayosAdapter (PaymentProviderPort) |
 | WebhookController --> PayosSignatureVerifier --> PaymentSettlement          |
 | CreditController ---------------------------+                               |
 | CreditPortService (reserve/settle/release) --+--> BalanceService            |
 |                                              (khóa dòng accounts)         |
 +-----------------------------------------------------------------------------+
            | việc PAYOS_CHECK
            v
 worker: PaymentScanner --> PAYOS_CHECK --> PayosCheckHandler --> PayosAdapter --> PaymentSettlement
```

**Text alternative**: Student, Teacher và Subject Manager có tài khoản `ACTIVE` có ví, xem số dư và mua credit qua `PaymentController` (Admin không có ví); controller kiểm chủ ví trước khi `PaymentService` tạo giao dịch và gọi PayOS qua `PayosAdapter`. PayOS gửi webhook tới `WebhookController`, chữ ký được kiểm rồi `PaymentSettlement` đánh dấu đã trả và cộng credit qua `BalanceService`; màn Checkout đọc lại trạng thái. U13 gọi `CreditPortService` để giữ, trừ và trả credit (U05 đi qua U13); Student chỉ được giữ cho `PRACTICE_GRADING` của attempt Text/Diagram Essay hợp lệ. Admin thêm/sửa gói; mức tặng định kỳ đọc từ Settings của U03. Worker đối soát PayOS; trả phần credit giữ quá hạn do scanner của U13.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `PaymentService` | backend | F2, F3, F4; P4 |
| `PayosSignatureVerifier`, `WebhookController` | backend | F5; P3 |
| `PaymentSettlement` | backend, worker | P2 |
| `BalanceService` | backend, worker | F1, F7; P1 |
| `CreditPortService` | backend, worker | F8; P5 |
| `PaymentScanner`, `PayosCheckHandler` | worker | F6; P6 |
| `CreditSettingDefinition` | backend | F9: khai báo `credit.monthlyFreeCredits` cho Settings |
| `PayosAdapter`, `FakePayosAdapter` | backend, worker | P7 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY` | Rỗng → adapter giả (không dùng ở prod) |
| `U07_PAYMENT_EXPIRE_MINUTES` | 15 |
| `U07_MAX_PENDING_PER_ACCOUNT` | 3 |
| `credit.monthlyFreeCredits` | 100; mục Settings của U03, Admin đổi trên Setting Detail |
| `U07_PACKAGES` | Danh sách gói `name:credits:priceVnd` |
| `U07_TOKENS_PER_CREDIT` | 1000 |
| `APP_PUBLIC_URL` | Dựng `returnUrl`, `cancelUrl` |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log key, payload đầy đủ |
| SECURITY-05 | Compliant | P3 kích thước, chữ ký |
| SECURITY-08 | Compliant | Owner R1 cho me/result; ADMIN riêng cho quản trị gói/history, version/audit/snapshot; mức tặng trên Settings, CreditPort nội bộ |
| SECURITY-09 | Compliant | Key trong `.env`; adapter giả không bật ở prod |
| SECURITY-15 | Compliant | P2, P3 fail closed |
| RESILIENCY-10 | Compliant | Timeout PayOS |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

## Admin components
PackageAdminController/PackageAdminService (F10) ghi gói với validation/version/U02 audit. PaymentAdminQueryService (F11) đọc phân trang an toàn. PackageSeeder chỉ INSERT thiếu theo ID/code ổn định, không ghi đè; dùng repository U07, AuthorizationPort U01, không thêm service triển khai độc lập.
