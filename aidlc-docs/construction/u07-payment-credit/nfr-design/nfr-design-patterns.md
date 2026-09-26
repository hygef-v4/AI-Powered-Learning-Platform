# U07 Payment & AI Credit - NFR Design Patterns

## P1 - Một đường ghi số dư (CreditLedgerService)
- `apply(accountId, type, freeDelta, purchasedDelta, ref, actor)`:
  1. Khóa ví `PESSIMISTIC_WRITE` (tạo ví nếu chưa có).
  2. Đặt lại tặng tháng nếu sang tháng mới (ghi `MONTHLY_GRANT` trước).
  3. Kiểm số dư sau thay đổi ≥ 0.
  4. INSERT sổ cái, UPDATE ví.
- Mọi luồng (mua, giữ, trừ, trả) gọi hàm này trong transaction của mình (NFR-U07-01).

## P2 - Áp dụng thanh toán idempotent (PaymentSettlement)
- `markPaid(orderCode, providerReference, source)` dùng chung cho webhook và job tự đối soát:
  1. Khóa `Payment` theo `orderCode`.
  2. Đã `PAID` → trả `DUPLICATE`.
  3. Kiểm số tiền; → `PAID`, `paidAt`.
  4. `CreditLedgerService.apply(PURCHASE, refId = paymentId)`; unique `(ref_type, ref_id)` cho `PURCHASE` là chốt chặn cuối.
  5. Audit.

## P3 - Webhook
1. Filter giới hạn kích thước 16 KB và rate limit Bucket4j theo IP.
2. `PayosSignatureVerifier`: sắp khóa `data` theo alphabet, nối `key=value&...`, HMAC-SHA256 với checksum key, so bằng `MessageDigest.isEqual`.
3. INSERT `PaymentWebhookEvent` (unique `eventKey`) cùng transaction với P2.
4. Trả `200 {"success": true}` cho cả `APPLIED` và `DUPLICATE`; chữ ký sai → `401`.

## P4 - Tạo link và idempotency
- Unique `(account_id, idempotency_key)`; gặp trùng → trả giao dịch cũ.
- `orderCode` = số dương 53 bit ngẫu nhiên (`SecureRandom`), thử lại khi trùng unique (PayOS yêu cầu số nguyên, JavaScript an toàn).
- Gọi PayOS **sau** khi commit `CREATED`; lỗi → cập nhật `FAILED` trong transaction riêng.

## P5 - Giữ credit
- `reserve` INSERT `CreditReservation` unique `requestRef`; trùng → trả bản cũ.
- `settle`/`release` khóa reservation, chỉ xử lý khi `HELD`; gọi lại sau khi đã xử lý → trả kết quả cũ.
- Sweeper chọn `HELD` quá `expiresAt` bằng `FOR UPDATE SKIP LOCKED`, `release` từng cái.

## P6 - Tự đối soát
- Job lấy tối đa 100 giao dịch mỗi lần; gọi PayOS tuần tự; `PAID` → P2 với `source = RECONCILE`; lỗi mạng → bỏ qua, lần sau thử lại.

## P7 - Adapter giả
- `FakePayosAdapter`: `checkoutUrl` trỏ về `/credits/fake-checkout?orderCode=...` có nút "Đã trả" gọi webhook giả có chữ ký bằng key test. Chỉ bật khi profile khác `prod` và không có `PAYOS_*` (NFR-U07-15).
