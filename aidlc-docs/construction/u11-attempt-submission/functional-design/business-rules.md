# U11 Attempt & Submission - Business Rules

**Bản tài liệu 2026-10-08**: UC 17, 18, 19, 20, 21, 22, 24, 25; primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Bắt đầu lượt

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-01 | Chỉ người học đang ghi danh `ACTIVE` của lớp, lớp `OPEN`, bài đang nhận bài nộp (`ON_TIME` hoặc `LATE`). | FR-007, US-ASM-003 S2 |
| BR-U11-02 | Bấm "Bắt đầu làm" tạo lượt và tính vào số lượt; chụp version bài, cấu hình, chính sách và seed trộn. | Câu 2 |
| BR-U11-03 | Mỗi người học tối đa một lượt `IN_PROGRESS` mỗi bài; bấm lại thì mở lượt đang làm. | Câu 2 |
| BR-U11-04 | Số lượt theo `maxAttempts` của U08 cho cả `GRADED` và `PRACTICE`; hết lượt → từ chối. | FR-007 |
| BR-U11-06 | `deadlineAt` = sớm nhất giữa `startedAt + timeLimit` (nếu có) và hạn cuối nhận bài (`lateUntil` nếu cho nộp trễ, không thì `closesAt`). | BR-U09-12 |

## 2. Làm bài và lưu nháp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-10 | Tự lưu 10 giây sau lần sửa cuối và khi rời trang; hiện "Đã lưu lúc …". | US-ASM-003 S4 |
| BR-U11-11 | Lưu gửi kèm `contentVersion`; lệch → `409` "bài đang được mở ở nơi khác, tải lại"; không ghi đè im lặng. | Thiết kế |
| BR-U11-12 | Lưu nháp kiểm cấu trúc (`DocumentModelPort.validateForSave` cho tài liệu; câu/lựa chọn tồn tại cho quiz). | BR-U09-36 |
| BR-U11-13 | Bản nháp không phải bài nộp; chỉ chính người học đọc/sửa. | US-ASM-003 S4 |
| BR-U11-14 | Lưu sau `deadlineAt` bị từ chối (trừ lần lưu cuối gửi trong 30 giây ân hạn mạng). | Thiết kế |
| BR-U11-15 | Code Lab: nút "Chạy thử" gọi U13 với test công khai; không tính là nộp. | UC 21 |

## 3. Nộp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-20 | Nộp tay kiểm: lượt `IN_PROGRESS` của chính mình, trước `deadlineAt` (+30 giây ân hạn), nội dung hợp lệ (`validateForSubmit` cho tài liệu). | US-ASM-003 S1, S2 |
| BR-U11-21 | Nộp: nội dung → bất biến, `submittedAt` theo giờ server, `late` nếu sau `closesAt`, `receiptHash`. Chỉ `GRADED` gọi `SubmissionSubmittedPort` để U15 tạo dòng đánh giá chờ chấm; `PRACTICE` Quiz gọi `PracticeResultPort.scoreQuiz` (U15 chấm bằng `QuizScorer`, cùng hàm với bài `GRADED`), `PRACTICE` Code Lab gọi `CodeRunPort.grade` (U13 chạy mọi test rồi ghi kết quả qua U15); `PRACTICE` Text/Diagram Essay **không tự chấm khi nộp**: lượt ở trạng thái chưa chấm AI, chờ Student bấm chấm (BR-U11-35). | FR-007, FR-030 |
| BR-U11-22 | Biên nhận hiển thị: mã lượt, thời điểm nộp, lượt thứ mấy, trễ hay không, mã băm. | UC 24 |
| BR-U11-23 | Tự nộp **bài hiện tại** (bản đã lưu gần nhất, kể cả lần lưu cuối client gửi khi hết giờ) khi: hết giới hạn giờ (`AUTO_TIME_LIMIT`), hết hạn (`AUTO_DEADLINE`), giảng viên ngừng giao (`AUTO_RETIRED`). Không kiểm điều kiện nộp; lỗi điều kiện ghi thành cảnh báo cho giảng viên. | Câu 3, 4 |
| BR-U11-24 | Lượt rỗng (chưa lưu gì) khi tự nộp vẫn được nộp với nội dung rỗng. | Câu 3 |

## 4. Lịch sử và lượt được chấm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-30 | Mỗi lượt giữ nguyên, lượt mới không ghi đè. | US-ASM-003 S5 |
| BR-U11-31 | Bài `GRADED`: lượt được chấm chính thức là **lượt nộp cuối**; giao diện đánh dấu rõ. Bài `PRACTICE`: mỗi attempt giữ kết quả luyện tập riêng, không chọn lượt cho sổ điểm. | FR-007, FR-030 |
| BR-U11-32 | Người học chỉ xem lượt của mình; ID của người khác → "không tìm thấy". | US-ASM-003 S3 |
| BR-U11-33 | `GRADED` hiển thị điểm/đáp án theo cấu hình và quyền công bố của U15; `PRACTICE` hiển thị kết quả từng attempt riêng, hoặc trạng thái "chưa chấm AI" kèm nút chấm. | BR-U09-13, FR-030 |
| BR-U11-34 | Người học tải bài tài liệu/bài viết của mình ra DOCX (U09). | BR-U09-52 |
| BR-U11-35 | Chấm AI bài Practice: từ Assignment Detail, Student chọn lượt đã nộp của mình và bấm "Chấm với AI"; U11 kiểm chủ lượt, lượt đã nộp, bài `PRACTICE` Text/Diagram Essay rồi gọi `PracticeGradingPort` (U13); U13 kiểm credit tại lúc bấm. Thiếu credit thì báo "Không đủ credit AI", không gọi AI; mua thêm rồi bấm lại được. Mỗi lượt tối đa một kết quả AI hợp lệ; quá 5 phút chưa có kết quả thì báo lỗi (BR-U13-24) và được bấm lại. | FR-008, UC 25; giữ quy tắc vận hành đã chốt 2026-10-04 |
| BR-U11-36 | Năm màn Quiz Assignments, Codelab Assignments, Text Essay Assignments, Diagram Essay Assignments và Group Essay Assignments hiện lượt gần nhất của Student cho mỗi bài (trạng thái, trễ, điểm nếu được hiện), mở Assignment Detail. Submission History mở từ chi tiết bài để xem/chuyển các lượt đã nộp; không đổi quyền chủ lượt. | UC 17, 18, 24; screen flow 2026-10-08 |

## 5. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U11-40 | Audit: nộp (tay/tự), từ chối nộp do quá hạn/hết lượt, truy cập bài người khác bị chặn. Không audit từng lần lưu nháp. | FR-014 |
