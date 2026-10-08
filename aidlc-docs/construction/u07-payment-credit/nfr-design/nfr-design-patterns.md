# U07 Payment & AI Credit - NFR Design Patterns

**Bản tài liệu 2026-10-08**: UC 08, 09, 10, 67, 68, 69; primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Một đường ghi số dư (BalanceService)
- `apply(accountId, freeDelta, purchasedDelta, reason)`:
  1. Kiểm tài khoản `ACTIVE` và chủ ví; cả bốn vai trò được tặng tháng; Student chỉ được giữ credit cho `PRACTICE_GRADING` hợp lệ.
  2. Khóa dòng `accounts` của tài khoản (`SELECT ... FOR UPDATE`); đặt lại tặng tháng nếu sang tháng mới.
  3. Kiểm số dư sau thay đổi ≥ 0 (CHECK trong DB là chốt chặn cuối).
  4. UPDATE cột số dư trong `accounts`.
- Mọi luồng (mua, giữ, trừ, trả) gọi hàm này trong transaction cùng dòng nghiệp vụ gây ra thay đổi (`payments` hoặc `ai_suggestions`) (NFR-U07-01).

## P2 - Áp dụng thanh toán idempotent (PaymentSettlement)
- `markPaid(orderCode, providerReference, source)` dùng chung cho webhook và tự đối soát:
  1. `UPDATE payments SET status = 'PAID', paid_at = now(), provider_reference = :ref WHERE order_code = :code AND status <> 'PAID' AND amount_vnd = :amount`.
  2. Không dòng nào bị cập nhật → đã cộng trước đó hoặc lệch số tiền (kiểm lại để audit), trả thành công.
  3. Một dòng → `BalanceService.apply(purchasedDelta = credits)` trong cùng transaction; audit.

## P3 - Webhook
1. Filter giới hạn kích thước 16 KB và rate limit Bucket4j theo IP.
2. `PayosSignatureVerifier`: sắp khóa `data` theo alphabet, nối `key=value&...`, HMAC-SHA256 với checksum key, so bằng `MessageDigest.isEqual`.
3. Gọi P2 (không lưu webhook riêng).
4. Trả `200 {"success": true}` cho cả lần áp dụng và lần trùng; chữ ký sai → `401` và audit.

## P4 - Tạo link và idempotency
- Unique `idempotency_key`; gặp trùng của cùng tài khoản → trả giao dịch cũ (kèm `checkout_url` nếu còn `PENDING`).
- `orderCode` = số dương 53 bit ngẫu nhiên (`SecureRandom`), thử lại khi trùng unique (PayOS yêu cầu số nguyên, JavaScript an toàn).
- Gọi PayOS **sau** khi commit `CREATED`; lỗi → cập nhật `FAILED` trong transaction riêng.

## P5 - Giữ credit
- `CreditPort` không lưu gì ngoài số dư: U13 gọi `reserve`/`settle`/`release` trong transaction đổi `ai_suggestions.credit_status` (`NONE → RESERVED → SETTLED | RELEASED`) bằng câu lệnh có điều kiện, nên thử lại không giữ hay trừ trùng (BR-U07-41).
- `reserve` trả `{reserved, fromFree}`; U13 lưu vào `credits_reserved`, `free_credits_reserved`. `settle`/`release` nhận lại hai số này để trả đúng phần tặng và phần mua.

## P6 - Tự đối soát
- `PaymentScanner` (scanner U03) mỗi 10 phút lấy tối đa 100 giao dịch cần kiểm, gửi việc `PAYOS_CHECK`; handler gọi PayOS; `PAID` → P2 với `source = RECONCILE`; lỗi mạng → thử lại theo U03 rồi để lần quét sau.

## P7 - Adapter giả
- `FakePayosAdapter`: `checkoutUrl` trỏ về `/credits/fake-checkout?orderCode=...` có nút "Đã trả" gọi webhook giả có chữ ký bằng key test. Chỉ bật khi profile khác `prod` và không có `PAYOS_*` (NFR-U07-15).
