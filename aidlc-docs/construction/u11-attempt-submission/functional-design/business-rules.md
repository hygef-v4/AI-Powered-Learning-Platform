# U11 Attempt & Submission - Business Rules

## 1. Bắt đầu lượt

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-01 | Chỉ người học đang ghi danh `ACTIVE` của lớp, lớp `OPEN`, publication đang nhận bài (`ON_TIME` hoặc `LATE`). | FR-007, US-ASM-003 S2 |
| BR-U11-02 | Bấm "Bắt đầu làm" tạo lượt và tính vào số lượt; chụp version bài, cấu hình, chính sách và seed trộn. | Câu 2 |
| BR-U11-03 | Mỗi người học tối đa một lượt `IN_PROGRESS` mỗi publication; bấm lại thì mở lượt đang làm. | Câu 2 |
| BR-U11-04 | Số lượt: bài thường theo `maxAttempts` (U08); thi thử theo U10 (mặc định 3, từ 1 đến 10). Hết lượt → từ chối. | FR-007, U10 |
| BR-U11-05 | Lượt đầu tiên của publication thi thử gọi `SimulationPolicyPort.lock`. | BR-U10-35 |
| BR-U11-06 | `deadlineAt` = sớm nhất giữa `startedAt + timeLimit` (nếu có) và hạn cuối nhận bài (`lateUntil` nếu cho nộp trễ, không thì `closesAt`). | BR-U09-12 |

## 2. Làm bài và lưu nháp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-10 | Tự lưu 10 giây sau lần sửa cuối và khi rời trang; hiện "Đã lưu lúc …". | US-ASM-003 S4 |
| BR-U11-11 | Lưu gửi kèm `contentVersion`; lệch → `409` "bài đang được mở ở nơi khác, tải lại"; không ghi đè im lặng. | Thiết kế |
| BR-U11-12 | Lưu nháp kiểm cấu trúc (`DocumentModelPort.validateForSave` cho tài liệu; câu/lựa chọn tồn tại cho quiz). | BR-U09-36 |
| BR-U11-13 | Bản nháp không phải bài nộp; chỉ chính người học đọc/sửa. | US-ASM-003 S4 |
| BR-U11-14 | Lưu sau `deadlineAt` bị từ chối (trừ lần lưu cuối gửi trong 30 giây ân hạn mạng). | Thiết kế |
| BR-U11-15 | Code Lab: nút "Chạy thử" gọi U13 với test công khai; không tính là nộp. | UC-ASM-13 |

## 3. Nộp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-20 | Nộp tay kiểm: lượt `IN_PROGRESS` của chính mình, trước `deadlineAt` (+30 giây ân hạn), nội dung hợp lệ (`validateForSubmit` cho tài liệu). | US-ASM-003 S1, S2 |
| BR-U11-21 | Nộp: nội dung → bất biến, `submittedAt` theo giờ server, `late` nếu sau `closesAt`, `receiptHash`; phát `SUBMISSION_SUBMITTED`. | FR-007 |
| BR-U11-22 | Biên nhận hiển thị: mã lượt, thời điểm nộp, lượt thứ mấy, trễ hay không, mã băm. | UC-ASM-14 |
| BR-U11-23 | Tự nộp **bài hiện tại** (bản đã lưu gần nhất, kể cả lần lưu cuối client gửi khi hết giờ) khi: hết giới hạn giờ (`AUTO_TIME_LIMIT`), hết hạn (`AUTO_DEADLINE`), giảng viên ngừng giao (`AUTO_RETIRED`). Không kiểm điều kiện nộp; lỗi điều kiện ghi thành cảnh báo cho giảng viên. | Câu 3, 4 |
| BR-U11-24 | Lượt rỗng (chưa lưu gì) khi tự nộp vẫn được nộp với nội dung rỗng. | Câu 3 |

## 4. Lịch sử và lượt được chấm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-30 | Mỗi lượt giữ nguyên, lượt mới không ghi đè. | US-ASM-003 S5 |
| BR-U11-31 | Bài thường: lượt được chấm là **lượt nộp cuối**; giao diện đánh dấu rõ. Thi thử: theo chính sách U10. | Câu 1 |
| BR-U11-32 | Người học chỉ xem lượt của mình; ID của người khác → "không tìm thấy". | US-ASM-003 S3 |
| BR-U11-33 | Hiển thị điểm/đáp án theo cấu hình (`showScoreAfterSubmit`, `showCorrectAnswers`, `answerRelease` thi thử); điểm do U15 cung cấp. | BR-U09-13, BR-U10-33 |
| BR-U11-34 | Người học tải bài tài liệu/bài viết của mình ra DOCX (U09). | BR-U09-52 |

## 5. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-40 | Audit: nộp (tay/tự), từ chối nộp do quá hạn/hết lượt, truy cập bài người khác bị chặn. Không audit từng lần lưu nháp. | FR-014 |
