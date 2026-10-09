# U07 Payment & AI Credit - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 08, 09, 10, 11, 68, 69, 72 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); mục credit tặng định kỳ của Settings (UC 70–71); primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U07. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005.
- **Primary UC hiện hành**: UC 08, 09, 10, 11, 68, 69, 72 theo bản 73 UC; mục credit tặng định kỳ của Settings (UC 70–71). Supporting flows theo current-srs-contract.md.
- **Quyết định 2026-10-09**: chỉ Student, Teacher, Subject Manager có ví (Admin không); kết quả thanh toán hiện trên Credit Package Checkout, không có màn Payment Result riêng; mức tặng định kỳ là mục Settings `credit.monthlyFreeCredits`; giữ credit khi tải học liệu (`MATERIAL_SUMMARY`) tối đa 25 giờ.
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
| `JobPort`, `JobHandler`, `ScheduledScanner`, `EventPublisherPort`, `SettingsPort`, `SettingDefinition` | U03 | Dùng thật |
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
- [ ] **Bước 4** - `BalanceService.apply` với khóa dòng tài khoản; Student, Teacher, Subject Manager `ACTIVE` có ví và được tặng định kỳ cùng mức đọc từ `SettingsPort`; Admin không có ví; chặn số dư âm (F1, F7, P1, BR-U07-01, 30…34).
- [ ] **Bước 5** - `PackageService`/`PackageAdminService` đọc/thêm/sửa gói với version/audit (F10); `U07_PACKAGES` chỉ INSERT seed thiếu, không ghi đè Admin; `CreditSettingDefinition` khai báo `credit.monthlyFreeCredits` (0–10 000, mặc định 100) (F9).
- [ ] **Bước 6** - `PaymentService`: kiểm tài khoản `ACTIVE` có ví và chủ ví trước khi tạo giao dịch hoặc gọi PayOS; idempotency, giới hạn 3 `PENDING`, `orderCode`, snapshot gói, `returnUrl`/`cancelUrl` về Checkout, hủy khi về qua `cancelUrl`, `FAILED`, hết hạn (F3, F4, P4, BR-U07-01, 03…07, 14).
- [ ] **Bước 7** - `PayosSignatureVerifier` và `PaymentSettlement.markPaid` dùng chung (F3, P2, P3, BR-U07-10…13); sau commit phát `payment.paid` qua `EventPublisherPort` (BR-U07-53).
- [ ] **Bước 8** - `CreditPortService`: `reserve` (trả `{reserved, fromFree}`), `settle`, `release`, `balance`, chạy trong transaction của U13; `reserve` kiểm purpose/attemptRef, Admin bị từ chối, Student chỉ được `PRACTICE_GRADING` Text/Diagram Essay của mình; thêm purpose `MATERIAL_SUMMARY` (F8, P5, BR-U07-01, 40…43).
- [ ] **Bước 9** - Worker: `PaymentScanner` (scanner U03, 10 phút, ≤ 100 giao dịch, đổi `PENDING` quá hạn sang `EXPIRED`) và `PayosCheckHandler` (F4, P6, BR-U07-06, 20, 22).
- [ ] **Bước 10** - Audit sự kiện `PAID`, webhook bị từ chối, tự đối soát (BR-U07-51).
- [ ] **Bước 11** - Unit test mọi `BR-U07-xx`: Student có ví, được tặng định kỳ và mua credit; Admin không có ví; mức tặng đổi trên Settings chỉ áp tháng sau; chỉ reserve cho Practice hợp lệ; chữ ký sai/đúng, số tiền lệch, webhook trùng, webhook sau `EXPIRED`, `settle` lớn hơn phần giữ, tặng định kỳ sang tháng mới.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u07-payment-credit/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu và PayOS

- [ ] **Bước 13** - Flyway `db/migration/billing/V20260925_1400__create_credit_packages_payments.sql` theo `infrastructure-design.md` §4: tạo `credit_packages`, `payments` (kèm snapshot), thêm cột số dư và CHECK vào `accounts` (cần migration U01 chạy trước).
- [ ] **Bước 14** - JPA repository (khóa `PESSIMISTIC_WRITE` trên `accounts`).
- [ ] **Bước 15** - `PayosAdapter` (tạo link, tra cứu, timeout 5/10 s).
- [ ] **Bước 16** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers: 20 webhook trùng song song chỉ cộng một lần; 50 `reserve` song song không làm âm số dư; số dư khớp tặng tháng + mua − dùng; `app` không DELETE được `payments`. `PayosAdapter` test bằng mock HTTP.
- [ ] **Bước 17** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 18** - `/contracts/openapi/billing.yaml` (endpoint theo `frontend-components.md`, gồm webhook).
- [ ] **Bước 19** - Controller + DTO + validation; rate limit webhook.
- [ ] **Bước 20** - Test MockMvc: Student/Teacher/Subject Manager `ACTIVE` được xem gói/ví, nhận tặng định kỳ và mua credit của mình; Admin gọi API ví/mua bị `403`; owner endpoint không xem người khác; ADMIN query riêng xem lịch sử toàn nền tảng chỉ đọc; `reserve` chỉ nội bộ và Student chỉ dùng cho Practice hợp lệ; webhook không cần đăng nhập nhưng sai chữ ký trả `401`.
- [ ] **Bước 21** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 22** - Màn My Credit Package (`MyCreditPackagePage`, `CreditBalanceSummary`, `PaymentHistoryTable`, `CreditUsageTable`), Public Credit Packages (`PackageCard`), Credit Package Checkout (`CheckoutSummary`, `PaymentStatusPanel` hiện kết quả khi PayOS quay về) và `CreditBalanceBadge` cho Student, Teacher, Subject Manager (UC 08–11).
- [ ] **Bước 23** - Trang thanh toán giả local; màn Admin Credit Package List (`CreditPackageListPage`, popup `CreditPackageDialog`) và Payment History (`PaymentHistoryPage`, `PaymentFilters`) theo F10–F11 (UC 68, 69, 72).
- [ ] **Bước 24** - Test frontend: màn Checkout không báo thành công khi chưa `PAID`; Admin không thấy badge và menu credit.
- [ ] **Bước 25** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 26** - Cập nhật `README.md`: đăng ký PayOS, đăng ký webhook, test bằng gói 2 000đ, chạy local với adapter giả, cách U13 dùng `CreditPort`.
- [ ] **Bước 27** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-PAY-001 (UC 08, 09, 10) | 4, 5, 6, 15, 22 |
| US-PAY-002 (UC 10, 11) | 7, 9, 16, 20, 22 |
| US-PAY-004 (UC 68, 69) | 5, 23 |
| US-PAY-005 (UC 72) | 23 |
| Credit cho U13 | 4, 8, 16 |


## 5. Ngoài phạm vi

- Tính token thực tế, kill-switch và quota AI (U13).
- Hoàn tiền.

## 6. Revision implementation scope - 2026-10-08
- [ ] PackageAdminController/Service GET/POST /admin/credit-packages và PATCH /{id} với version; name/information/price/credits validate, audit atomic, không DELETE.
- [ ] PaymentAdminQueryService GET /admin/payments, account/package/time/status filters, pagination ≤ 100; ADMIN only, trả snapshot-safe DTO không secret.
- [ ] Migration information/version/actor-time cho packages; packageName/information snapshot cho payment mới, tương thích payment cũ. Seed không ghi đè gói đã sửa.
- [ ] Test old PENDING/PAID snapshot không đổi sau edit; version cũ bị 409; role khác không add/edit/query global; /me/result vẫn owner-only.
- [ ] UI Public Credit Packages/Credit Package List/Payment History; không manual balance, reconciliation, refund hay package delete; mức tặng sửa trên Settings của U03.

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] Admin không có ví: `reserve`, `/me/credits`, `/me/payments`, `/me/credit-usage`, `POST /payments` từ chối `ADMIN`; `x-roles` của `billing.yaml` chỉ còn `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` cho các API này.
- [ ] Bỏ màn Payment Result `/credits/result`: `returnUrl`, `cancelUrl` trỏ về `/credits/checkout?orderCode=`; thêm `POST /api/v1/payments/{orderCode}:cancel`.
- [ ] Mức tặng định kỳ: bỏ biến `U07_MONTHLY_FREE_CREDITS`, thêm `CreditSettingDefinition` (`credit.monthlyFreeCredits`) và đọc qua `SettingsPort`.
- [ ] Thêm API Admin vào `billing.yaml`: `GET`, `POST /api/v1/admin/credit-packages`, `PATCH /api/v1/admin/credit-packages/{id}`, `GET /api/v1/admin/payments`.
- [ ] `CreditPurpose` thêm `MATERIAL_SUMMARY`; phần giữ cho quét học liệu tối đa 25 giờ (scanner của U13, ghi vào plan U13 khi sửa U13).
- [ ] Màn theo screen flow: My Credit Package, Public Credit Packages, Credit Package Checkout (Class Dashboard); Credit Package List, popup Credit Package Detail, Payment History (Admin Dashboard).

## Bổ sung sau recheck 2026-10-09 — credit cho HOLD học liệu

- [ ] Đồng bộ contract/code U07 với BR-U07-40/43: một reserve cho HOLD khi tạo lesson; child summary/merge/embedding không reserve thêm. CreditPort.release chỉ hoàn toàn bộ khi usage = 0; đã dùng thì settle(actualCredits), kể cả terminal lỗi/quá hạn.
- [ ] Unit scenarios: hoàn toàn bộ trước AI, summary thành công rồi embedding lỗi chỉ tính summary, HOLD quá hạn trả dư, retry/replay không reserve/charge lại; bảo toàn phân bổ credit tặng/mua theo fromFree.
- [ ] Rà tích hợp scanner U13 qua cùng settlement policy; không sửa completed steps hoặc coi contract/code hiện tại đã được cập nhật bởi revision tài liệu.
