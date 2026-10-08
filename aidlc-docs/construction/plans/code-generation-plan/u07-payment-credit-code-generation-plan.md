# U07 Payment & AI Credit - Code Generation Plan

**Bản tài liệu 2026-10-08**: UC 08, 09, 10, 67, 68, 69; primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U07. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005.
- **Primary UC hiện hành**: UC 08, 09, 10, 67, 68, 69. Supporting flows theo current-srs-contract.md.
- **Thiết kế nguồn**: `construction/u07-payment-credit/` (functional-design, nfr-requirements, nfr-design, infrastructure-design) và `construction/shared-infrastructure.md`.
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật (tài khoản `ACTIVE`, vai trò, chủ ví) |
| `AuditPort` | U02 | Dùng thật |
| `JobPort`, `JobHandler`, `ScheduledScanner`, `EventPublisherPort` | U03 | Dùng thật |
| U07 cung cấp `CreditPort` | cho U13 | U13 gọi cho mọi lần dùng AI (kể cả embedding của U05 qua `AiUsagePort`) |
| `CreditUsagePort` | U13 (`C`, U07 khai báo) | Adapter rỗng: bảng "Lần dùng credit" trống tới khi U13 (wave 4) cài bản thật đọc `ai_suggestions` |
| `PaymentProviderPort` | PayOS | Adapter thật + adapter giả khi không có key (không bật ở prod) |

### Dữ liệu U07 sở hữu

PostgreSQL `credit_packages`, `payments`; ghi cột số dư của `accounts` (U01 tạo); Redis `ratelimit:payos-webhook:*`; queue `jobs.payos` (`PAYOS_CHECK`). Không có bảng sổ cái, webhook hay cấu hình. Phát event `payment.paid` lên `platform.events` sau commit (BR-U07-53).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  billing/
    api/                PaymentController, WebhookController, CreditController,
                        DTO
    application/        PaymentService, PaymentSettlement, BalanceService,
                        CreditPortService, PackageService
    domain/             CreditPackage, Payment, PaymentStatus, CreditBalance
    infrastructure/     JPA repository, PayosAdapter, PayosSignatureVerifier,
                        FakePayosAdapter
    worker/             PaymentScanner, PayosCheckHandler
    port/               CreditPort, PaymentProviderPort, CreditUsagePort
/backend/src/main/resources/db/migration/billing/
/frontend/src/app/credits/
/frontend/src/components/credits/
/contracts/openapi/billing.yaml
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - Biến cấu hình U07 theo `logical-components.md` §3; Compose truyền `PAYOS_*`, `APP_PUBLIC_URL`; Nginx route webhook 16 KB.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain: gói, giao dịch và chuyển trạng thái, số dư (BR-U07-01…08).
- [ ] **Bước 3** - Port `CreditPort`, `PaymentProviderPort`, `CreditUsagePort` (adapter rỗng tới khi có U13); `FakePayosAdapter` (chỉ khi không phải prod).
- [ ] **Bước 4** - `BalanceService.apply` với khóa dòng tài khoản; cả bốn vai trò `ACTIVE` có ví và được tặng tháng cùng mức; chặn số dư âm (F5, P1, BR-U07-01, 30…34).
- [ ] **Bước 5** - PackageService/AdminService đọc/thêm/sửa gói với version/audit; U07_PACKAGES chỉ INSERT seed thiếu, không ghi đè Admin; monthly grant đọc U07_MONTHLY_FREE_CREDITS (F7–F8).
- [ ] **Bước 6** - `PaymentService`: kiểm tài khoản `ACTIVE` thuộc bốn vai trò hiện hành và chủ ví trước khi tạo giao dịch hoặc gọi PayOS; idempotency, giới hạn 3 `PENDING`, `orderCode`, `FAILED`, hủy/hết hạn (F1, F2, P4, BR-U07-01, 03…07).
- [ ] **Bước 7** - `PayosSignatureVerifier` và `PaymentSettlement.markPaid` dùng chung (F3, P2, P3, BR-U07-10…13); sau commit phát `payment.paid` qua `EventPublisherPort` (BR-U07-53).
- [ ] **Bước 8** - `CreditPortService`: `reserve` (trả `{reserved, fromFree}`), `settle`, `release`, `balance`, chạy trong transaction của U13; `reserve` kiểm purpose/attemptRef, Student chỉ được `PRACTICE_GRADING` Text/Diagram Essay của mình (F6, P5, BR-U07-01, 40…43).
- [ ] **Bước 9** - Worker: `PaymentScanner` (scanner U03, 10 phút, ≤ 100 giao dịch, đổi `PENDING` quá hạn sang `EXPIRED`) và `PayosCheckHandler` (F4, P6, BR-U07-06, 20, 22).
- [ ] **Bước 10** - Audit sự kiện `PAID`, webhook bị từ chối, tự đối soát (BR-U07-51).
- [ ] **Bước 11** - Unit test mọi `BR-U07-xx`: Student có ví, được tặng tháng và mua credit; chỉ reserve cho Practice hợp lệ; chữ ký sai/đúng, số tiền lệch, webhook trùng, webhook sau `EXPIRED`, `settle` lớn hơn phần giữ, tặng tháng sang tháng mới cho cả bốn vai trò.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u07-payment-credit/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và PayOS

- [ ] **Bước 13** - Flyway `V20260925_1400__u07_payment_credit.sql` theo `infrastructure-design.md` §4: tạo `credit_packages`, `payments`, thêm cột số dư và CHECK vào `accounts` (cần migration U01 chạy trước).
- [ ] **Bước 14** - JPA repository (khóa `PESSIMISTIC_WRITE` trên `accounts`).
- [ ] **Bước 15** - `PayosAdapter` (tạo link, tra cứu, timeout 5/10 s).
- [ ] **Bước 16** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers: 20 webhook trùng song song chỉ cộng một lần; 50 `reserve` song song không làm âm số dư; số dư khớp tặng tháng + mua − dùng; `app` không DELETE được `payments`. `PayosAdapter` test bằng mock HTTP.
- [ ] **Bước 17** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 18** - `/contracts/openapi/billing.yaml` (endpoint theo `frontend-components.md`, gồm webhook).
- [ ] **Bước 19** - Controller + DTO + validation; rate limit webhook.
- [ ] **Bước 20** - Test MockMvc: Student `ACTIVE` được xem gói/ví, nhận tặng tháng và mua credit của mình; owner endpoint không xem người khác; ADMIN query riêng xem global history chỉ đọc; `reserve` chỉ nội bộ và Student chỉ dùng cho Practice hợp lệ; webhook không cần đăng nhập nhưng sai chữ ký trả `401`.
- [ ] **Bước 21** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 22** - `CreditBalanceBadge`, `CreditsPage` (`PackageList`, `PaymentHistoryTable`, `CreditUsageTable`), checkout và `PaymentResultPage` cho cả bốn vai trò; Student xem ví và mua credit để chấm Practice (UC 25).
- [ ] **Bước 23** - Trang thanh toán giả local, CreditPackageSettingPage/Admin form và PaymentHistoryPage query toàn hệ thống theo F8–F9.
- [ ] **Bước 24** - Test frontend: trang kết quả không báo thành công khi chưa `PAID`.
- [ ] **Bước 25** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 26** - Cập nhật `README.md`: đăng ký PayOS, đăng ký webhook, test bằng gói 2 000đ, chạy local với adapter giả, cách U13 dùng `CreditPort`.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-PAY-001 (UC 08, 09, 10) | 5, 6, 15, 22 |
| US-PAY-002 (UC 08, 09, 10) | 7, 9, 16, 20 |
| Credit cho U13 | 4, 8, 16 |

## 5. Ngoài phạm vi

- Tính token thực tế, kill-switch và quota AI (U13).
- Hoàn tiền.

## 6. Revision implementation scope - 2026-10-08
- [ ] PackageAdminController/Service GET/POST /admin/credit-packages và PATCH /{id} với version; name/information/price/credits validate, audit atomic, không DELETE.
- [ ] PaymentAdminQueryService GET /admin/payments, account/package/time/status filters, pagination ≤ 100; ADMIN only, trả snapshot-safe DTO không secret.
- [ ] Migration information/version/actor-time cho packages; packageName/information snapshot cho payment mới, tương thích payment cũ. Seed không ghi đè gói đã sửa.
- [ ] Test old PENDING/PAID snapshot không đổi sau edit; version cũ bị 409; role khác không add/edit/query global; /me/result vẫn owner-only.
- [ ] UI Credit Packages/Credit Package Setting/Payment History; không manual balance, reconciliation, refund, package delete hay sửa monthly grant.
