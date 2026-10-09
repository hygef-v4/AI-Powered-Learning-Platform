# U07 Payment & AI Credit - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 08, 09, 10, 11, 68, 69, 72 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); mục credit tặng định kỳ của Settings (UC 70–71); primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - My Credit Package (UC 08)
1. Từ Class Dashboard bấm My Credit Package; backend kiểm tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER` hoặc `SUBJECT_MANAGER` (BR-U07-01).
2. Đọc số dư theo F7 (áp mức tặng của tháng): credit tặng, credit mua, tổng.
3. Hiện lịch sử mua của chính mình và lần dùng credit (qua `CreditUsagePort` của U13), phân trang 20.

## F2 - Public Credit Packages (UC 09)
1. Từ Class Dashboard bấm Public Credit Packages; kiểm như F1 bước 1.
2. Hiện các gói `active`: tên, thông tin, số credit, giá.
3. Bấm Mua một gói → Credit Package Checkout (F3).

## F3 - Mua credit trên Credit Package Checkout (UC 10)
1. Màn Checkout hiện gói đã chọn (tên, credit, giá) và nút Thanh toán.
2. Bấm Thanh toán: frontend gửi kèm `Idempotency-Key`; backend kiểm chủ ví, gói `active`, tối đa 3 `PENDING` (BR-U07-01, 04, 05).
3. Tạo `Payment` `CREATED`, sinh `orderCode` duy nhất, chụp giá, credit, tên và thông tin gói (BR-U07-03).
4. Gọi PayOS tạo link (số tiền, mô tả, `returnUrl` và `cancelUrl` trỏ về Checkout kèm `orderCode`, hết hạn 15 phút).
5. Thành công → `PENDING`, trả `checkoutUrl`, frontend chuyển sang trang PayOS (QR chuyển khoản); lỗi → `FAILED`, màn báo lỗi và cho thử lại (BR-U07-07).

## F4 - Kết quả thanh toán trên Checkout (UC 11)
1. PayOS chuyển về Checkout qua `returnUrl` hoặc `cancelUrl` kèm `orderCode` (BR-U07-14).
2. Frontend đọc trạng thái giao dịch của chính mình, tự cập nhật 3 giây một lần tối đa 2 phút; không cộng credit ở bước này (BR-U07-10).
3. Về qua `cancelUrl` → backend hỏi PayOS; chưa trả thì đặt `CANCELLED`.
4. Màn hiện: đang chờ xác nhận, thành công (kèm số credit nhận), đã hủy, hết hạn hoặc lỗi; thành công có nút về My Credit Package.

## F5 - Webhook
1. Kiểm chữ ký; sai → audit, trả `401` (BR-U07-10).
2. Tìm `payments` theo `order_code`, kiểm số tiền và mã kết quả (BR-U07-11).
3. Một transaction: `UPDATE payments SET status = 'PAID', paid_at, provider_reference WHERE order_code = :code AND status <> 'PAID'`; cập nhật một dòng → khóa dòng tài khoản, tăng `purchased_balance` (BR-U07-12, 13); audit. Không dòng nào → đã cộng trước đó, không làm gì.
4. Sau commit phát `payment.paid`; trả `200`.

## F6 - Tự đối soát
1. `ScheduledScanner` của U07 mỗi 10 phút chọn giao dịch `PENDING` quá 5 phút và `EXPIRED` trong 24 giờ qua, gửi việc `PAYOS_CHECK` cho từng giao dịch (BR-U07-20); cùng scanner đổi `PENDING` quá hạn sang `EXPIRED` (BR-U07-06).
2. Handler gọi PayOS lấy trạng thái theo `order_code`.
3. `PAID` → áp dụng như F5 bước 3; `CANCELLED`/`EXPIRED` → cập nhật; lỗi → giữ nguyên (BR-U07-22).

## F7 - Số dư và tặng định kỳ
1. Kiểm tài khoản `ACTIVE` có ví (ba vai trò) và chủ ví (BR-U07-01, 31).
2. Đọc số dư (khóa dòng tài khoản): nếu `free_period` khác tháng hiện tại → đặt `free_balance` = giá trị `credit.monthlyFreeCredits` đọc qua `SettingsPort` của U03, `free_period` = tháng hiện tại.

## F8 - Giữ và trừ credit (U13 gọi trong transaction của mình)
1. `reserve`: kiểm tài khoản `ACTIVE` có ví, chủ ví và `purpose`; `STUDENT` chỉ hợp lệ cho `PRACTICE_GRADING` của attempt Practice Text/Diagram Essay đã xác minh. Áp tặng định kỳ như F7, kiểm đủ, trừ tặng trước rồi mua (BR-U07-33), trả `{reserved, fromFree}` để U13 lưu vào `ai_suggestions`. Thiếu credit → lỗi `INSUFFICIENT_CREDIT`, không trừ gì (BR-U07-40).
2. `settle`: tính chênh lệch với phần giữ; dư thì trả lại (vào credit tặng trước theo `fromFree` nếu vẫn cùng tháng, phần còn lại vào credit mua), thiếu thì trừ thêm tối đa số dư còn lại, không để âm (BR-U07-42).
3. `release`: trả lại toàn bộ phần giữ theo cùng quy tắc (BR-U07-43).

## F9 - Seed gói và khai báo mục Settings
1. `U07_PACKAGES` chỉ seed gói chưa có; không cập nhật giá/ghi đè gói Admin đã sửa. Quản trị gói dùng F10.
2. U07 khai báo `SettingDefinition` `credit.monthlyFreeCredits` (nhóm Credit) cho U03; Admin xem và sửa trên Setting List/Setting Detail (UC 70–71).

## F10 - Admin quản trị gói (UC 68–69)
1. Từ Admin Dashboard bấm Credit Package List; kiểm ADMIN `ACTIVE`; hiện mọi gói kèm trạng thái bán.
2. Bấm Thêm hoặc một gói → popup Credit Package Detail: tên, thông tin, giá, credit, trạng thái bán; sửa cần `version`; thiếu/sai/version cũ bị từ chối, không ghi một phần.
3. Lưu trong transaction với khóa optimistic và audit actor/thời gian/trước-sau; trả version mới.
4. Payment đã tạo giữ snapshot; không đổi ví. Không xóa gói.

## F11 - Admin Payment History (UC 72)
1. Từ Admin Dashboard bấm Payment History; kiểm ADMIN `ACTIVE`; endpoint riêng khác `/me/payments`.
2. Kiểm bộ lọc tài khoản/gói/thời gian/trạng thái, khoảng thời gian và phân trang hợp lệ, limit ≤ 100; query chỉ đọc.
3. Trả người mua, snapshot gói, số tiền, thời gian, trạng thái; không secret hoặc dữ liệu thẻ. Vai trò khác bị từ chối; không có sửa/xóa payment hay đối soát tay.
