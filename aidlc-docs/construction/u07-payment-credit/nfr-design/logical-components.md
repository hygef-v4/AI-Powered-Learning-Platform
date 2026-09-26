# U07 Payment & AI Credit - Logical Components

## 1. Sơ đồ

```
 Trình duyệt                 PayOS (Internet)              U05/U13 (nội bộ)
   |  mua, xem ví              |  webhook   ^ tạo link/tra cứu     |
   v                           v            |                      v
 +--------------------------------- backend -----------------------------------+
 | PaymentController --> PaymentService --> PayosAdapter (PaymentProviderPort) |
 | WebhookController --> PayosSignatureVerifier --> PaymentSettlement          |
 | CreditController ---------------------------+                               |
 | CreditPortService (reserve/settle/release) --+--> CreditLedgerService       |
 | AdminCreditController (gói, mức tặng) -----+       (khóa ví, sổ cái)        |
 +-----------------------------------------------------------------------------+
            | job U07_RECONCILE, U07_RESERVATION_SWEEP
            v
 worker: ReconcileHandler --> PayosAdapter --> PaymentSettlement
         ReservationSweepHandler --> CreditPortService.release
```

**Text alternative**: Người dùng mua credit qua `PaymentController`; `PaymentService` tạo giao dịch và gọi PayOS qua `PayosAdapter`. PayOS gửi webhook tới `WebhookController`, chữ ký được kiểm rồi `PaymentSettlement` đánh dấu đã trả và cộng credit qua `CreditLedgerService`. U05 và U13 gọi `CreditPortService` để giữ, trừ, trả credit cho embedding và tạo nội dung; admin quản lý gói và mức tặng tháng. Trong worker, job đối soát tra PayOS và áp dụng kết quả, job quét trả lại phần credit giữ quá hạn.

## 2. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `PaymentService` | backend | F1, F2; P4 |
| `PayosSignatureVerifier`, `WebhookController` | backend | F3; P3 |
| `PaymentSettlement` | backend, worker | P2 |
| `CreditLedgerService` | backend, worker | F5; P1 |
| `CreditPortService` | backend, worker | F6; P5 |
| `ReconcileHandler`, `ReservationSweepHandler` | worker | F4, F6; P5, P6 |
| `PayosAdapter`, `FakePayosAdapter` | backend, worker | P7 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY` | Rỗng → adapter giả (không dùng ở prod) |
| `U07_PAYMENT_EXPIRE_MINUTES` | 15 |
| `U07_MAX_PENDING_PER_ACCOUNT` | 3 |
| `U07_MONTHLY_FREE_CREDITS` | 50 (admin sửa trong DB, biến này là giá trị khởi tạo) |
| `U07_RESERVATION_TTL_MINUTES` | 30 |
| `U07_TOKENS_PER_CREDIT` | 1000 |
| `APP_PUBLIC_URL` | Dựng `returnUrl`, `cancelUrl` |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log key, payload đầy đủ |
| SECURITY-05 | Compliant | P3 kích thước, chữ ký |
| SECURITY-08 | Compliant | Chỉ chủ tài khoản xem ví/lịch sử; chỉ ADMIN sửa gói và mức tặng; `CreditPort` nội bộ |
| SECURITY-09 | Compliant | Key trong `.env`; adapter giả không bật ở prod |
| SECURITY-15 | Compliant | P2, P3 fail closed |
| RESILIENCY-10 | Compliant | Timeout PayOS |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
