# U07 Payment & AI Credit - Business Rules

**Bản tài liệu 2026-10-09**: UC 08, 09, 10, 11, 68, 69, 72 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); mục credit tặng định kỳ của Settings (UC 70–71); primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ví, gói và mua

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-01 | Tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER` hoặc `SUBJECT_MANAGER` có ví credit của chính mình: xem trên My Credit Package (UC 08), xem gói trên Public Credit Packages (UC 09), mua trên Credit Package Checkout (UC 10–11), mở từ Class Dashboard. Admin không có ví, không mua, không được tặng credit (Admin không dùng chức năng AI). Backend kiểm quyền chủ ví trước khi tạo giao dịch/link PayOS. Credit đã mua chỉ thuộc tài khoản mua. | FR-010; người dùng chốt 2026-10-09 |
| BR-U07-02 | ADMIN xem gói trên Credit Package List (UC 68), thêm/sửa trong popup Credit Package Detail (UC 69): tên, thông tin, giá, số credit, trạng thái bán (`active`). Giá dương theo trần tối thiểu PayOS, credit nguyên dương; kiểm version và audit trước/sau. Giao dịch cũ giữ snapshot. Không xóa gói (ngừng bán bằng `active = false`); seed ban đầu không ghi đè sửa của Admin. Mức tặng định kỳ chỉnh trên Settings (BR-U07-31), không thuộc màn này. | FR-031, US-PAY-004, UC 68–69 |
| BR-U07-03 | Giao dịch chụp `credits`, `amountVnd`, tên và thông tin gói lúc tạo. | Câu 3 |
| BR-U07-04 | Tạo giao dịch cần `Idempotency-Key`; gửi lại cùng khóa trả lại giao dịch cũ nếu còn `PENDING`. | SEC-007 |
| BR-U07-05 | Mỗi tài khoản tối đa 3 giao dịch `PENDING` cùng lúc. | Thiết kế |
| BR-U07-06 | Link PayOS hết hạn sau 15 phút; `PENDING` quá hạn → `EXPIRED`. | Thiết kế |
| BR-U07-07 | PayOS lỗi/timeout khi tạo link → `FAILED`, không cộng credit, người dùng thử lại bằng giao dịch mới. | US-PAY-001 S2 |
| BR-U07-08 | MVP không có hoàn tiền: không có màn hình hay trạng thái `REFUNDED`; credit đã mua không thu hồi. Hủy link `PENDING` không phải hoàn giao dịch đã `PAID`. | Quyết định 2026-10-03 theo screen flow |

## 2. Xác nhận và kết quả thanh toán

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-10 | Chỉ webhook có chữ ký hợp lệ (HMAC-SHA256 bằng checksum key PayOS) hoặc tự đối soát qua API PayOS mới chuyển `PAID`. Trang quay về chỉ hiển thị. | US-PAY-002, SEC-007 |
| BR-U07-11 | Webhook phải khớp `orderCode` có thật, `amount` bằng `amountVnd`, mã kết quả thành công; lệch → từ chối, audit sự kiện bảo mật, không cộng. | US-PAY-002 S3 |
| BR-U07-12 | Chuyển `PAID` và cộng `purchased_balance` trong một transaction, đúng một lần theo `order_code` (cập nhật có điều kiện `status <> 'PAID'`); webhook trùng trả thành công, không cộng thêm. | US-PAY-002 S2 |
| BR-U07-13 | Webhook tới sau khi `EXPIRED`/`CANCELLED` nhưng hợp lệ và đã trả tiền → vẫn `PAID` và cộng credit (tiền đã nhận), audit. | Thiết kế |
| BR-U07-14 | Kết quả thanh toán (UC 11) hiện trên chính màn Credit Package Checkout: `returnUrl` và `cancelUrl` của PayOS trỏ về màn này kèm `orderCode`; màn tự cập nhật trạng thái (đang chờ, thành công, đã hủy, hết hạn, lỗi) của giao dịch của chính mình; không có màn Payment Result riêng. | UC 11; người dùng chốt 2026-10-09 |

## 3. Tự đối soát

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-20 | Scanner đối soát mỗi 10 phút kiểm giao dịch `PENDING` quá 5 phút và `EXPIRED` trong 24 giờ qua bằng API PayOS; kết quả áp dụng như webhook (BR-U07-12). | US-PAY-002 S4 |
| BR-U07-22 | PayOS không trả lời hoặc dữ liệu không xác minh được → giữ nguyên trạng thái, không cộng, ghi lỗi, retry lần sau. | US-PAY-002 S5 |

## 4. Số dư credit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-30 | 1 credit = 1 000 token Gemini, làm tròn lên mỗi lần gọi. | Câu 4 |
| BR-U07-31 | Mọi tài khoản `ACTIVE` có vai trò `STUDENT`, `TEACHER` hoặc `SUBJECT_MANAGER` được tặng cùng một số credit mỗi tháng. Mức tặng là mục `credit.monthlyFreeCredits` của Settings (nhóm Credit, U07 khai báo `SettingDefinition`, số nguyên 0–10 000, mặc định 100, đủ cho mức giữ tối đa khi yêu cầu tóm tắt một học liệu); Admin đổi trên Setting Detail (UC 71), có hiệu lực từ lần đặt lại kế tiếp. Tháng mới `freeBalance` đặt lại bằng mức tặng (không cộng dồn) khi đọc số dư lần đầu trong tháng. Student dùng credit tặng như credit mua, cho `PRACTICE_GRADING` hợp lệ hoặc `MATERIAL_SUMMARY` của học liệu được xem. | Câu 4, 5; người dùng chốt 2026-10-04, 2026-10-09 |
| BR-U07-32 | Credit mua không hết hạn. | Câu 3 |
| BR-U07-33 | Trừ credit tặng trước, credit mua sau. | Câu 4 |
| BR-U07-34 | Số dư không bao giờ âm. | Thiết kế |

## 5. Dùng credit (cho U05 và U13)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-40 | Trước Gemini cần khoản giữ của actor ACTIVE chịu phí. Tóm tắt học liệu: U05 kiểm quyền xem, U13 reserve một HOLD tổng summary+embedding khi người xem yêu cầu; upload không reserve. Student được PRACTICE_GRADING hợp lệ hoặc MATERIAL_SUMMARY cho lesson có quyền xem; child EMBEDDING chỉ dùng cùng HOLD học liệu đã được xác minh, không cho Student reserve embedding độc lập/soạn đề/chấm Graded. Teacher/Subject Manager theo scope. Thiếu credit không nhận yêu cầu AI nhưng học liệu vẫn xem/tải được; requester chịu phí, retry/duplicate không reserve thêm. | Revision View Material 2026-10-09 |
| BR-U07-41 | Mỗi lần giữ gắn với đúng một dòng `ai_suggestions` của U13; U13 chỉ gọi `reserve`, `settle`, `release` khi chuyển `credit_status` của dòng đó trong cùng transaction, nên thử lại không giữ hay trừ trùng. | Quyết định 2026-10-03 |
| BR-U07-42 | `settle(actual)`: trừ đúng số thực tế; phần giữ dư trả lại; thực tế lớn hơn phần giữ → trừ thêm tối đa phần số dư còn lại, không để âm. | Thiết kế |
| BR-U07-43 | `CreditPort.release(accountId, reserved, fromFree)` chỉ hoàn toàn bộ khi lượng dùng thật bằng 0; lỗi hoặc quá hạn sau khi đã dùng AI phải gọi `settle(actualCredits)` và trả dư. `AiUsagePort.release(holdId)` của U13 là thao tác chốt: chọn settle nếu đã dùng, CreditPort.release nếu chưa dùng. Scanner U13 áp cùng quy tắc cho phần giữ quá hạn (30 phút dòng thường; 25 giờ HOLD học liệu dự phòng cho deadline scan 24 giờ). Child call credit_status NONE không tự hoàn HOLD và không kéo dài hạn HOLD (BR-U05-37, BR-U13-52/53). | Thiết kế; người dùng chốt 2026-10-09 |

## 6. Audit và quyền xem

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U07-51 | Audit: `PAID`, webhook bị từ chối (chữ ký/số tiền sai), đối soát tự động, thêm/sửa gói. Đổi mức tặng định kỳ được U03 audit `SETTING_UPDATED`. | FR-014, SEC-005 |
| BR-U07-52 | Endpoint ví/lịch sử cá nhân/kết quả giao dịch chỉ chủ tài khoản (R1). ADMIN có query lịch sử toàn nền tảng riêng trên Payment History (UC 72) theo tài khoản/gói/thời gian/trạng thái, phân trang, chỉ đọc, không secret/dữ liệu thẻ và không sửa payment/credit. Quyền Admin history không bypass owner check endpoint `/me`. | FR-032, US-PAY-005, UC 72 |
| BR-U07-53 | Sau khi `PAID`, phát event `payment.paid` (sau commit) để U16 báo trong app. | U16 |
