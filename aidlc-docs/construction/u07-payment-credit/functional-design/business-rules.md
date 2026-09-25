# U07 Payment & AI Credit - Business Rules

## 1. Gói và mua

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-01 | Mọi tài khoản `ACTIVE` mua được credit; credit dùng cho mọi tính năng AI của chính tài khoản đó. | Câu 1 |
| BR-U07-02 | Chỉ ADMIN tạo/sửa/ẩn gói; không xóa gói đã có giao dịch. Đổi giá chỉ áp dụng giao dịch mới. | Câu 3 |
| BR-U07-03 | Giao dịch chụp `credits`, `amountVnd` lúc tạo. | Câu 3 |
| BR-U07-04 | Tạo giao dịch cần `Idempotency-Key`; gửi lại cùng khóa trả lại giao dịch cũ nếu còn `PENDING`. | SEC-007 |
| BR-U07-05 | Mỗi tài khoản tối đa 3 giao dịch `PENDING` cùng lúc. | Thiết kế |
| BR-U07-06 | Link PayOS hết hạn sau 15 phút; `PENDING` quá hạn → `EXPIRED`. | Thiết kế |
| BR-U07-07 | PayOS lỗi/timeout khi tạo link → `FAILED`, không cộng credit, người dùng thử lại bằng giao dịch mới. | US-PAY-001 S2 |
| BR-U07-08 | Phạm vi MVP hiện chưa có luồng hoàn tiền và chưa có trạng thái `REFUNDED`; chính sách hoàn tiền đang chờ quyết định. Hủy link `PENDING` không phải hoàn giao dịch đã `PAID`. | Quyết định mở 2026-09-25 |

## 2. Xác nhận thanh toán

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-10 | Chỉ webhook có chữ ký hợp lệ (HMAC-SHA256 bằng checksum key PayOS) hoặc đối soát qua API PayOS mới chuyển `PAID`. Trang quay về (return URL) chỉ hiển thị. | US-PAY-002, SEC-007 |
| BR-U07-11 | Webhook phải khớp `orderCode` có thật, `amount` bằng `amountVnd`, mã kết quả thành công; lệch → `REJECTED`, audit sự kiện bảo mật, không cộng. | US-PAY-002 S3 |
| BR-U07-12 | `PAID` và ghi sổ `PURCHASE` trong một transaction, đúng một lần theo `eventKey`/`orderCode`; webhook trùng → `DUPLICATE`, trả thành công, không cộng thêm. | US-PAY-002 S2 |
| BR-U07-13 | Webhook tới sau khi `EXPIRED`/`CANCELLED` nhưng hợp lệ và đã trả tiền → vẫn `PAID` và cộng credit (tiền đã nhận), audit. | Thiết kế |

## 3. Đối soát

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-20 | Job đối soát mỗi 10 phút kiểm giao dịch `PENDING` quá 5 phút và `EXPIRED` trong 24 giờ qua bằng API PayOS; kết quả áp dụng như webhook (BR-U07-12). | US-PAY-003 S1 |
| BR-U07-21 | ADMIN bấm đối soát một giao dịch được. | UC-PAY-02 |
| BR-U07-22 | PayOS không trả lời hoặc dữ liệu không xác minh được → giữ nguyên trạng thái, không cộng, ghi lỗi, retry lần sau. | US-PAY-003 S2 |

## 4. Ví credit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-30 | 1 credit = 1 000 token Gemini, làm tròn lên mỗi lần gọi. | Câu 4 |
| BR-U07-31 | Mọi tài khoản được tặng cùng một số credit mỗi tháng (`monthlyFreeCredits`, admin cấu hình). Tháng mới: `freeBalance` đặt lại bằng mức tặng (không cộng dồn); làm lười khi ví được đọc lần đầu trong tháng. | Câu 4, 5 |
| BR-U07-32 | Credit mua không hết hạn. | Câu 3 |
| BR-U07-33 | Trừ credit tặng trước, credit mua sau. | Câu 4 |
| BR-U07-34 | Số dư không bao giờ âm. | Thiết kế |

## 5. Dùng credit (cho U05 và U13)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-40 | Trước mỗi lời gọi Gemini tạo nội dung hoặc embedding, U05/U13 `reserve` số credit ước tính của tài khoản chịu phí; không đủ → từ chối "không đủ credit", không gọi Gemini. Embedding nền tính cho người tải/phát hành học liệu; embedding truy xuất tính cho người yêu cầu AI. | FR-021, quyết định đồng bộ 2026-09-25 |
| BR-U07-41 | `reserve` idempotent theo `requestRef`. | Thiết kế |
| BR-U07-42 | `settle(actual)`: trừ đúng số thực tế; phần giữ dư trả lại; thực tế lớn hơn phần giữ → trừ thêm tối đa phần còn lại trong ví, không để âm. | Thiết kế |
| BR-U07-43 | `release`: trả lại toàn bộ khi AI lỗi hoặc hết hạn mức hệ thống trước khi gọi provider. Phần giữ quá 30 phút tự trả lại (job quét). | Thiết kế |

## 6. Điều chỉnh và audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-50 | ADMIN cộng/trừ credit mua của một tài khoản, lý do ≥ 10 ký tự, không làm số dư âm; audit. | Câu 6 |
| BR-U07-53 | Sau khi `PAID`, phát event `u07.payment.paid` (sau commit) để U16 báo trong app. | U16 |
| BR-U07-51 | Audit: tạo/sửa/ẩn gói, `PAID`, webhook `REJECTED`, đối soát thủ công, điều chỉnh, đổi mức tặng tháng. | FR-014, SEC-005 |
| BR-U07-52 | Người dùng xem số dư, lịch sử giao dịch và sổ cái của mình; ADMIN xem mọi tài khoản. | UC-PAY-01, 02 |
