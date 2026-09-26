# U07 Payment & AI Credit - Business Logic Model

## F1 - Mua credit
1. Người dùng chọn gói `active`, gửi kèm `Idempotency-Key` (BR-U07-04, 05).
2. Tạo `Payment` `CREATED`, sinh `orderCode` duy nhất, chụp giá (BR-U07-03).
3. Gọi PayOS tạo link (số tiền, mô tả, `returnUrl`, `cancelUrl`, hết hạn 15 phút).
4. Thành công → `PENDING`, trả `checkoutUrl`; lỗi → `FAILED` (BR-U07-07).
5. Frontend chuyển người dùng sang trang PayOS (QR chuyển khoản).

## F2 - Trang quay về
1. PayOS chuyển về `returnUrl` hoặc `cancelUrl` với `orderCode`.
2. Frontend gọi trạng thái giao dịch, poll 3 giây tối đa 2 phút; không cộng credit ở bước này (BR-U07-10).
3. `cancelUrl` → backend gọi PayOS kiểm; nếu chưa trả thì `CANCELLED`.

## F3 - Webhook
1. Kiểm chữ ký; sai → lưu `REJECTED`, audit, trả `401` (BR-U07-10).
2. Tìm `Payment` theo `orderCode`, kiểm số tiền và mã kết quả (BR-U07-11).
3. Một transaction: INSERT `PaymentWebhookEvent` (unique `eventKey`) → nếu trùng thì `DUPLICATE`; `Payment` → `PAID` (khi chưa `PAID`) → ghi sổ `PURCHASE` + tăng `purchasedBalance` (BR-U07-12, 13); audit.
4. Trả `200`.

## F4 - Tự đối soát
1. Job U02 `U07_RECONCILE` mỗi 10 phút (BR-U07-20).
2. Gọi PayOS lấy trạng thái theo `orderCode`.
3. `PAID` → áp dụng như F3 bước 3; `CANCELLED`/`EXPIRED` → cập nhật; lỗi → giữ nguyên (BR-U07-22).

## F5 - Ví và tặng tháng
1. Đọc ví: nếu `freePeriod` khác tháng hiện tại → đặt `freeBalance = monthlyFreeCredits`, ghi sổ `MONTHLY_GRANT` với delta tương ứng (BR-U07-31).
2. Tài khoản chưa có ví → tạo khi đọc lần đầu.

## F6 - Giữ và trừ credit (U05 và U13 gọi)
1. `reserve`: F5 bước 1, khóa ví, kiểm đủ, trừ tặng trước rồi mua (BR-U07-33), tạo `HELD`, ghi sổ `RESERVE`.
2. `settle`: tính chênh lệch với phần giữ, trả lại hoặc trừ thêm (BR-U07-42), ghi sổ `SETTLE`, `SETTLED`.
3. `release`: trả lại phần giữ, ghi sổ `RELEASE`, `RELEASED`.
4. Job quét mỗi 5 phút trả lại phần `HELD` quá hạn (BR-U07-43).

## F7 - Quản trị
1. Gói: tạo/sửa/ẩn (BR-U07-02).
2. Mức tặng tháng: sửa, có hiệu lực từ lần đặt lại kế tiếp.
