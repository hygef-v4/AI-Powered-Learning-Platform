# U07 Payment & AI Credit - Business Rules

## 1. Gói và mua

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-01 | Tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` hoặc `ADMIN` có ví credit của chính mình, được xem gói, số dư, lịch sử mua và lần dùng credit, và mua credit. Backend kiểm quyền chủ ví trước khi tạo giao dịch/link PayOS. Credit đã mua chỉ thuộc tài khoản mua. | FR-010, quyết định 2026-09-29 |
| BR-U07-02 | Gói credit cố định, nạp bằng migration/cấu hình khi triển khai; không có màn hay API tạo/sửa/ẩn gói. Gói đã có giao dịch không bị xóa; đổi giá ở lần triển khai sau chỉ áp dụng giao dịch mới. | Câu 3, quyết định 2026-10-03 |
| BR-U07-03 | Giao dịch chụp `credits`, `amountVnd` lúc tạo. | Câu 3 |
| BR-U07-04 | Tạo giao dịch cần `Idempotency-Key`; gửi lại cùng khóa trả lại giao dịch cũ nếu còn `PENDING`. | SEC-007 |
| BR-U07-05 | Mỗi tài khoản tối đa 3 giao dịch `PENDING` cùng lúc. | Thiết kế |
| BR-U07-06 | Link PayOS hết hạn sau 15 phút; `PENDING` quá hạn → `EXPIRED`. | Thiết kế |
| BR-U07-07 | PayOS lỗi/timeout khi tạo link → `FAILED`, không cộng credit, người dùng thử lại bằng giao dịch mới. | US-PAY-001 S2 |
| BR-U07-08 | MVP không có hoàn tiền: không có màn hình hay trạng thái `REFUNDED` (screen flow không có luồng này); credit đã mua không thu hồi. Hủy link `PENDING` không phải hoàn giao dịch đã `PAID`. | Quyết định 2026-10-03 theo screen flow |

## 2. Xác nhận thanh toán

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-10 | Chỉ webhook có chữ ký hợp lệ (HMAC-SHA256 bằng checksum key PayOS) hoặc tự đối soát qua API PayOS mới chuyển `PAID`. Trang quay về (return URL) chỉ hiển thị. | US-PAY-002, SEC-007 |
| BR-U07-11 | Webhook phải khớp `orderCode` có thật, `amount` bằng `amountVnd`, mã kết quả thành công; lệch → từ chối, audit sự kiện bảo mật, không cộng. | US-PAY-002 S3 |
| BR-U07-12 | Chuyển `PAID` và cộng `purchased_balance` trong một transaction, đúng một lần theo `order_code` (cập nhật có điều kiện `status <> 'PAID'`); webhook trùng trả thành công, không cộng thêm. | US-PAY-002 S2 |
| BR-U07-13 | Webhook tới sau khi `EXPIRED`/`CANCELLED` nhưng hợp lệ và đã trả tiền → vẫn `PAID` và cộng credit (tiền đã nhận), audit. | Thiết kế |

## 3. Tự đối soát

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-20 | Scanner đối soát mỗi 10 phút kiểm giao dịch `PENDING` quá 5 phút và `EXPIRED` trong 24 giờ qua bằng API PayOS; kết quả áp dụng như webhook (BR-U07-12). | US-PAY-002 S4 |
| BR-U07-22 | PayOS không trả lời hoặc dữ liệu không xác minh được → giữ nguyên trạng thái, không cộng, ghi lỗi, retry lần sau. | US-PAY-002 S5 |

## 4. Số dư credit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-30 | 1 credit = 1 000 token Gemini, làm tròn lên mỗi lần gọi. | Câu 4 |
| BR-U07-31 | Mọi tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER`, `SUBJECT_MANAGER` hoặc `ADMIN` được tặng cùng một số credit mỗi tháng (`U07_MONTHLY_FREE_CREDITS`, cấu hình triển khai, không có màn sửa). Student dùng credit tặng như credit mua, chỉ cho `PRACTICE_GRADING` hợp lệ. Tháng mới `freeBalance` đặt lại bằng mức tặng (không cộng dồn) khi đọc số dư lần đầu trong tháng. | Câu 4, 5; người dùng chốt 2026-10-04 (Student cũng được tặng) |
| BR-U07-32 | Credit mua không hết hạn. | Câu 3 |
| BR-U07-33 | Trừ credit tặng trước, credit mua sau. | Câu 4 |
| BR-U07-34 | Số dư không bao giờ âm. | Thiết kế |

## 5. Dùng credit (cho U05 và U13)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-40 | Trước mỗi lời gọi Gemini, U13 (cả khi U05 quét học liệu hay truy xuất, qua `AiUsagePort`) `reserve` credit của tài khoản `ACTIVE` chịu phí và kèm `purpose`. `STUDENT` chỉ được reserve với `purpose = PRACTICE_GRADING` sau khi U11/U13 xác minh attempt của chính mình, dạng Text/Diagram Essay và chế độ `PRACTICE`; mọi purpose khác bị từ chối. Teacher/Subject Manager/Admin tiếp tục dùng AI theo phạm vi nghiệp vụ. Thiếu credit → không gọi Gemini; với Practice, bài đã nộp vẫn ở trạng thái chưa chấm AI và Student bấm chấm lại sau khi mua credit. Embedding nền tính cho người tải học liệu có quyền; embedding truy xuất tính cho người yêu cầu AI có quyền. | FR-021, FR-030 |
| BR-U07-41 | Mỗi lần giữ gắn với đúng một dòng `ai_suggestions` của U13; U13 chỉ gọi `reserve`, `settle`, `release` khi chuyển `credit_status` của dòng đó trong cùng transaction, nên thử lại không giữ hay trừ trùng. | Quyết định 2026-10-03 |
| BR-U07-42 | `settle(actual)`: trừ đúng số thực tế; phần giữ dư trả lại; thực tế lớn hơn phần giữ → trừ thêm tối đa phần số dư còn lại, không để âm. | Thiết kế |
| BR-U07-43 | `release`: trả lại toàn bộ khi AI lỗi hoặc hết hạn mức hệ thống trước khi gọi provider. Phần giữ quá 30 phút tự trả lại (scanner của U13). | Thiết kế |

## 6. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-51 | Audit: `PAID`, webhook bị từ chối (chữ ký/số tiền sai), đối soát tự động. | FR-014, SEC-005 |
| BR-U07-52 | Cả bốn vai trò hiện hành được xem số dư, lịch sử mua (`payments`) và lần dùng credit (`ai_suggestions`) của chính mình. Không ai xem giao dịch của tài khoản khác. | UC 37 |
| BR-U07-53 | Sau khi `PAID`, phát event `payment.paid` (sau commit) để U16 báo trong app. | U16 |
