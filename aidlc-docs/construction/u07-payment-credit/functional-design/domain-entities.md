# U07 Payment & AI Credit - Domain Entities

## 1. Phạm vi sở hữu

U07 sở hữu gói credit, giao dịch thanh toán qua PayOS, webhook, ví credit AI, sổ cái credit và phần giữ credit khi dùng AI. U07 **không** sở hữu: gọi AI và tính token thực tế (U13), kill-switch/quota AI toàn hệ thống (U13), quyền vào lớp (U04, không liên quan).

## 2. `CreditPackage`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `name` | chuỗi ≤ 100 | |
| `credits` | số nguyên > 0 | |
| `priceVnd` | số nguyên ≥ 2 000 | Đơn vị VND |
| `active` | bool | Ẩn thay vì xóa |

## 3. `Payment`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `orderCode` | số nguyên 64 bit | Mã gửi PayOS; duy nhất |
| `accountId` | UUID | Người mua |
| `packageId` | UUID | |
| `credits`, `amountVnd` | số | Chụp lại lúc tạo, không đổi khi gói đổi giá |
| `status` | enum | `CREATED`, `PENDING`, `PAID`, `CANCELLED`, `EXPIRED`, `FAILED` |
| `checkoutUrl` | chuỗi | Link PayOS |
| `providerReference` | chuỗi | Mã tham chiếu PayOS khi đã trả |
| `idempotencyKey` | chuỗi | Duy nhất theo `accountId` |
| `expiresAt` | thời gian | Tạo + 15 phút |
| `paidAt`, `createdAt` | thời gian | |

## 4. `PaymentWebhookEvent`

`id`, `orderCode`, `eventKey` (duy nhất: `orderCode` + `reference` của PayOS), `signatureValid`, `result` (`APPLIED`, `DUPLICATE`, `REJECTED`), `receivedAt`. Không lưu dữ liệu thẻ (PayOS là chuyển khoản, không có thẻ).

## 5. `CreditWallet` và `CreditLedgerEntry`

`CreditWallet`: `accountId` (khóa), `freeBalance`, `freePeriod` (`yyyy-MM`), `purchasedBalance`, `version`.

`CreditLedgerEntry`:

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `accountId` | UUID | |
| `type` | enum | `PURCHASE`, `MONTHLY_GRANT`, `RESERVE`, `SETTLE`, `RELEASE`, `ADJUSTMENT` |
| `freeDelta`, `purchasedDelta` | số nguyên | Âm hoặc dương |
| `refType`, `refId` | | `PAYMENT`, `RESERVATION`, `ADMIN` |
| `reason` | chuỗi | Bắt buộc với `ADJUSTMENT` |
| `actorId`, `createdAt` | | |

Sổ cái chỉ thêm, không sửa, không xóa. Số dư ví = tổng sổ cái (kiểm được bằng query).

## 6. `CreditReservation`

`id`, `accountId`, `requestRef` (duy nhất, do U13 đặt), `freeHeld`, `purchasedHeld`, `status` (`HELD`, `SETTLED`, `RELEASED`), `expiresAt` (tạo + 30 phút).

## 7. Trạng thái

```
Payment: CREATED --tạo link OK--> PENDING --webhook/đối soát PAID--> PAID
            |                        |--hủy--> CANCELLED
            +--PayOS lỗi--> FAILED   |--quá hạn--> EXPIRED
Reservation: HELD --settle--> SETTLED
               +---release/hết hạn--> RELEASED
```

**Text alternative**: Giao dịch tạo ở `CREATED`; tạo link PayOS thành công thì `PENDING`, lỗi thì `FAILED`. Từ `PENDING`, xác nhận đã trả (webhook hoặc đối soát) thì `PAID`; người dùng hủy thì `CANCELLED`; quá 15 phút thì `EXPIRED`. Phần giữ credit ở `HELD`, khi AI xong thì `SETTLED`, khi AI lỗi hoặc quá 30 phút thì `RELEASED`.

## 8. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `CreditPort` | U07 cung cấp cho U13 | `reserve(accountId, credits, requestRef)`, `settle(reservationId, actualCredits)`, `release(reservationId)`, `balance(accountId)` |
| `PaymentProviderPort` | U07 dùng, adapter PayOS | Tạo link, lấy trạng thái, kiểm chữ ký webhook |
| `AuthorizationPort` | U07 dùng U01 | Quyền admin |
| `JobPort`, `AuditPort` | U07 dùng U02 | Đối soát, audit |
