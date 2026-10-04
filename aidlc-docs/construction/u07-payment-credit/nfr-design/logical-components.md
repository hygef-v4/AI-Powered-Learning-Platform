# U07 Payment & AI Credit - Logical Components

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

**Text alternative**: Student, Teacher, Subject Manager và Admin có tài khoản `ACTIVE` đều có ví và mua credit qua `PaymentController`; controller kiểm chủ ví trước khi `PaymentService` tạo giao dịch và gọi PayOS qua `PayosAdapter`. PayOS gửi webhook tới `WebhookController`, chữ ký được kiểm rồi `PaymentSettlement` đánh dấu đã trả và cộng credit qua `BalanceService`. U13 gọi `CreditPortService` để giữ, trừ và trả credit (U05 đi qua U13); Student chỉ được giữ cho `PRACTICE_GRADING` của attempt Text/Diagram Essay hợp lệ. Gói và mức tặng tháng là cấu hình triển khai. Worker đối soát PayOS; trả phần credit giữ quá hạn do scanner của U13.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `PaymentService` | backend | F1, F2; P4 |
| `PayosSignatureVerifier`, `WebhookController` | backend | F3; P3 |
| `PaymentSettlement` | backend, worker | P2 |
| `BalanceService` | backend, worker | F5; P1 |
| `CreditPortService` | backend, worker | F6; P5 |
| `PaymentScanner`, `PayosCheckHandler` | worker | F4; P6 |
| `PayosAdapter`, `FakePayosAdapter` | backend, worker | P7 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY` | Rỗng → adapter giả (không dùng ở prod) |
| `U07_PAYMENT_EXPIRE_MINUTES` | 15 |
| `U07_MAX_PENDING_PER_ACCOUNT` | 3 |
| `U07_MONTHLY_FREE_CREDITS` | 50 (đổi bằng lần triển khai mới) |
| `U07_PACKAGES` | Danh sách gói `name:credits:priceVnd` |
| `U07_TOKENS_PER_CREDIT` | 1000 |
| `APP_PUBLIC_URL` | Dựng `returnUrl`, `cancelUrl` |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log key, payload đầy đủ |
| SECURITY-05 | Compliant | P3 kích thước, chữ ký |
| SECURITY-08 | Compliant | Chỉ chủ tài khoản xem ví/lịch sử; gói và mức tặng cố định, không có API sửa; `CreditPort` nội bộ |
| SECURITY-09 | Compliant | Key trong `.env`; adapter giả không bật ở prod |
| SECURITY-15 | Compliant | P2, P3 fail closed |
| RESILIENCY-10 | Compliant | Timeout PayOS |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
