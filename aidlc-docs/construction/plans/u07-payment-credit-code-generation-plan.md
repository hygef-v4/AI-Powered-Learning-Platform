# U07 Payment & AI Credit - Code Generation Plan

> Plan này là nguồn duy nhất cho Code Generation của U07. Mỗi bước xong thì đánh `[x]` ngay.

## 1. Bối cảnh

- **Story**: US-PAY-001, US-PAY-002. **Use case**: UC-PAY-01. Job tự đối soát thuộc US-PAY-002; không có thao tác admin đối soát hoặc điều chỉnh credit thủ công.
- **Thiết kế nguồn**: `construction/u07-payment-credit/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước 1-6 của plan U01**. Unit nào được code trước thì làm; unit sau đánh dấu `[x]`. Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `JobPort`, `JobHandler`, `AuditPort` | U02 | Dùng thật |
| U07 cung cấp `CreditPort` | cho U05 và U13 | U05 dùng cho embedding, U13 dùng cho tạo nội dung AI |
| `PaymentProviderPort` | PayOS | Adapter thật + adapter giả khi không có key (không bật ở prod) |

### Dữ liệu U07 sở hữu

PostgreSQL `credit_packages`, `payments`, `payment_webhook_events`, `credit_ledger`; ghi cột số dư của `accounts` (U01 tạo) và khóa `u07.*` trong `app_settings`; Redis `ratelimit:payos-webhook:*`; queue `jobs.payos` (`PAYOS_RECONCILE`), `jobs.scheduled` (`CREDIT_RESERVATION_SWEEP`).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  u07/
    api/                PaymentController, WebhookController, CreditController,
                        AdminCreditController, DTO
    application/        PaymentService, PaymentSettlement, CreditLedgerService,
                        CreditPortService, PackageService
    domain/             CreditPackage, Payment, PaymentStatus, PaymentWebhookEvent,
                        CreditBalance, CreditLedgerEntry, CreditReservation (suy ra từ sổ cái)
    infrastructure/     JPA repository, PayosAdapter, PayosSignatureVerifier,
                        FakePayosAdapter
    worker/             ReconcileHandler, ReservationSweepHandler
    port/               CreditPort, PaymentProviderPort
/backend/src/main/resources/db/migration/u07/
/frontend/src/app/credits/
/frontend/src/app/admin/credits/
/frontend/src/components/credits/
/contracts/openapi/u07-payment-credit.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án. Chưa có thì thực hiện Bước 1-6 của plan U01 trước rồi đánh dấu ở cả hai plan.
- [ ] **Bước 1** - Biến cấu hình U07 theo `logical-components.md` §3; Compose truyền `PAYOS_*`, `APP_PUBLIC_URL`; Nginx route webhook 16 KB.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain: gói, giao dịch và chuyển trạng thái, webhook event, số dư, sổ cái, lần giữ suy ra từ sổ cái (BR-U07-01…08).
- [ ] **Bước 3** - Port `CreditPort`, `PaymentProviderPort`; `FakePayosAdapter` (chỉ khi không phải prod).
- [ ] **Bước 4** - `CreditLedgerService.apply` với khóa dòng tài khoản, đặt lại tặng tháng, chặn âm (F5, P1, BR-U07-30…34).
- [ ] **Bước 5** - `PackageService` và cấu hình mức tặng tháng qua `app_settings` (F7, BR-U07-02).
- [ ] **Bước 6** - `PaymentService`: idempotency, giới hạn 3 `PENDING`, `orderCode`, gọi PayOS sau commit, `FAILED`, hủy/hết hạn (F1, F2, P4, BR-U07-03…07).
- [ ] **Bước 7** - `PayosSignatureVerifier` và `PaymentSettlement.markPaid` dùng chung (F3, P2, P3, BR-U07-10…13).
- [ ] **Bước 8** - `CreditPortService`: `reserve`/`settle`/`release`/`balance` idempotent (F6, P5, BR-U07-40…43).
- [ ] **Bước 9** - Worker: `ReconcileHandler` (10 phút, ≤ 100 giao dịch) và `ReservationSweepHandler` (5 phút, `SKIP LOCKED`) (F4, P5, P6, BR-U07-20, BR-U07-22).
- [ ] **Bước 10** - Audit sự kiện `PAID`, webhook bị từ chối, job tự đối soát và thay đổi cấu hình gói (BR-U07-51).
- [ ] **Bước 11** - Unit test mọi `BR-U07-xx`: chữ ký sai/đúng, số tiền lệch, webhook trùng, webhook sau `EXPIRED`, `settle` lớn hơn phần giữ, tặng tháng sang tháng mới.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u07-payment-credit/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và PayOS

- [ ] **Bước 13** - Flyway `V20260925_1400__u07_payment_credit.sql` theo `infrastructure-design.md` §4: tạo bảng của U07, thêm cột số dư vào `accounts`, seed khóa `u07.*` trong `app_settings` (cần migration U01 chạy trước).
- [ ] **Bước 14** - JPA repository (khóa `PESSIMISTIC_WRITE`, `SKIP LOCKED`).
- [ ] **Bước 15** - `PayosAdapter` (tạo link, tra cứu, timeout 5/10 s).
- [ ] **Bước 16** - Integration test Testcontainers: 20 webhook trùng song song chỉ cộng một lần; 50 `reserve` song song không làm âm số dư; `settle` gọi hai lần chỉ đóng một lần; tổng sổ cái = số dư; `app` không UPDATE/DELETE được sổ cái. `PayosAdapter` test bằng mock HTTP.
- [ ] **Bước 17** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [ ] **Bước 18** - `/contracts/openapi/u07-payment-credit.yaml` (endpoint theo `frontend-components.md`, gồm webhook).
- [ ] **Bước 19** - Controller + DTO + validation; rate limit webhook.
- [ ] **Bước 20** - Test MockMvc: người dùng không xem giao dịch người khác, không có endpoint `reserve`, webhook không cần đăng nhập nhưng sai chữ ký trả `401`.
- [ ] **Bước 21** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 22** - `CreditBalanceBadge`, `CreditsPage` (`PackageList`, `PaymentHistoryTable`, `LedgerTable`), `PaymentResultPage`.
- [ ] **Bước 23** - Admin: `PackageAdminPage` cho gói và mức tặng; trang thanh toán giả cho local.
- [ ] **Bước 24** - Test frontend: trang kết quả không báo thành công khi chưa `PAID`.
- [ ] **Bước 25** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 26** - Cập nhật `README.md`: đăng ký PayOS, đăng ký webhook, test bằng gói 2 000đ, chạy local với adapter giả, cách U05/U13 dùng `CreditPort`.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-PAY-001 (UC-PAY-01) | 5, 6, 15, 22 |
| US-PAY-002 (UC-PAY-01) | 7, 9, 16, 20 |
| Credit cho U13 | 4, 8, 16 |

## 5. Ngoài phạm vi

- Tính token thực tế, kill-switch và quota AI (U13).
- Hoàn tiền.
