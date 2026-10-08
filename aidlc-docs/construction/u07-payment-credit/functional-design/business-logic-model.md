# U07 Payment & AI Credit - Business Logic Model

**Bản tài liệu 2026-10-08**: UC 08, 09, 10, 67, 68, 69; primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - Mua credit
1. Backend kiểm tài khoản `ACTIVE` thuộc một trong bốn vai trò hiện hành và quyền trên chính ví trước khi hiển thị gói hay tạo thanh toán. Người mua hợp lệ chọn gói `active`, gửi kèm `Idempotency-Key` (BR-U07-01, 04, 05).
2. Tạo `Payment` `CREATED`, sinh `orderCode` duy nhất, chụp giá (BR-U07-03).
3. Gọi PayOS tạo link (số tiền, mô tả, `returnUrl`, `cancelUrl`, hết hạn 15 phút).
4. Thành công → `PENDING`, trả `checkoutUrl`; lỗi → `FAILED` (BR-U07-07).
5. Frontend chuyển người dùng sang trang PayOS (QR chuyển khoản).

## F2 - Màn Payment Result
1. PayOS chuyển về `returnUrl` hoặc `cancelUrl` (màn Payment Result `/credits/result`) với `orderCode`.
2. Frontend gọi trạng thái giao dịch, poll 3 giây tối đa 2 phút; không cộng credit ở bước này (BR-U07-10).
3. `cancelUrl` → backend gọi PayOS kiểm; nếu chưa trả thì `CANCELLED`.

## F3 - Webhook
1. Kiểm chữ ký; sai → audit, trả `401` (BR-U07-10).
2. Tìm `payments` theo `order_code`, kiểm số tiền và mã kết quả (BR-U07-11).
3. Một transaction: `UPDATE payments SET status = 'PAID', paid_at, provider_reference WHERE order_code = :code AND status <> 'PAID'`; cập nhật một dòng → khóa dòng tài khoản, tăng `purchased_balance` (BR-U07-12, 13); audit. Không dòng nào → đã cộng trước đó, không làm gì.
4. Sau commit phát `payment.paid`; trả `200`.

## F4 - Tự đối soát
1. `ScheduledScanner` của U07 mỗi 10 phút chọn giao dịch `PENDING` quá 5 phút và `EXPIRED` trong 24 giờ qua, gửi việc `PAYOS_CHECK` cho từng giao dịch (BR-U07-20); cùng scanner đổi `PENDING` quá hạn sang `EXPIRED` (BR-U07-06).
2. Handler gọi PayOS lấy trạng thái theo `order_code`.
3. `PAID` → áp dụng như F3 bước 3; `CANCELLED`/`EXPIRED` → cập nhật; lỗi → giữ nguyên (BR-U07-22).

## F5 - Số dư và tặng tháng
1. Kiểm tài khoản `ACTIVE` và chủ ví; cả bốn vai trò được đọc số dư và được tặng tháng (BR-U07-01, 31).
2. Đọc số dư (khóa dòng tài khoản): nếu `free_period` khác tháng hiện tại → đặt `free_balance = U07_MONTHLY_FREE_CREDITS`, `free_period` = tháng hiện tại. Áp dụng như nhau cho cả bốn vai trò.

## F6 - Giữ và trừ credit (U13 gọi trong transaction của mình)
1. `reserve`: kiểm tài khoản `ACTIVE`, chủ ví và `purpose`; `STUDENT` chỉ hợp lệ cho `PRACTICE_GRADING` của attempt Practice Text/Diagram Essay đã xác minh. Áp tặng tháng như F5, kiểm đủ, trừ tặng trước rồi mua (BR-U07-33), trả `{reserved, fromFree}` để U13 lưu vào `ai_suggestions`. Thiếu credit → lỗi `INSUFFICIENT_CREDIT`, không trừ gì (BR-U07-40).
2. `settle`: tính chênh lệch với phần giữ; dư thì trả lại (vào credit tặng trước theo `fromFree` nếu vẫn cùng tháng, phần còn lại vào credit mua), thiếu thì trừ thêm tối đa số dư còn lại, không để âm (BR-U07-42).
3. `release`: trả lại toàn bộ phần giữ theo cùng quy tắc (BR-U07-43).

## F7 - Seed ban đầu và cấu hình tặng tháng
1. U07_PACKAGES chỉ seed gói chưa có; không cập nhật giá/ghi đè gói Admin đã sửa. Quản trị gói dùng F8.
2. Mức tặng tháng: `U07_MONTHLY_FREE_CREDITS`; đổi bằng lần triển khai mới, có hiệu lực từ lần đặt lại kế tiếp.

## F8 - Admin quản trị gói (UC 67–68)
1. authorize ADMIN ACTIVE; GET danh sách/chi tiết cấu hình bán.
2. POST/PATCH name/information/priceVnd/credits hợp lệ; patch cần version; thiếu/sai/version cũ từ chối, không ghi một phần.
3. Khóa optimistic, lưu và audit actor/thời gian/trước-sau trong transaction; trả version mới.
4. Payment đã tạo giữ credits/amount/name snapshot; không đổi ví hoặc mức tặng tháng. Không delete gói.

## F9 - Admin Payment History (UC 69)
1. authorize ADMIN ACTIVE, endpoint riêng khác /me/payments và Payment Result.
2. Kiểm bộ lọc account/package/time/status, khoảng thời gian và pagination hợp lệ, limit ≤ 100; query read-only.
3. Trả người mua/snapshot gói/số tiền/thời gian/trạng thái; không secret hoặc dữ liệu thẻ. Vai trò khác từ chối; không có update/delete payment hay đối soát tay.
