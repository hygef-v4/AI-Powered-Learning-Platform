# U11 Attempt & Submission - Business Logic Model

## F1 - Danh sách bài của người học (UC-ASM-09)
1. Lấy publication `OPEN`/`CLOSED` của lớp đang ghi danh (U08), kèm trạng thái của người học: chưa làm, đang làm, đã nộp (số lượt, trễ), hết hạn.

## F2 - Bắt đầu lượt
1. Kiểm BR-U11-01, 03, 04.
2. Tạo `Attempt` `IN_PROGRESS`, snapshot, seed, `deadlineAt` (BR-U11-02, 06); thi thử gọi `lock` (BR-U11-05).
3. Tạo job U02 `ATTEMPT_AUTO_SUBMIT` tại `deadlineAt`.
4. Trả đề theo góc nhìn người học (U06/U08 đã lọc đáp án), đã trộn theo seed.

## F3 - Lưu nháp
1. Kiểm quyền, `IN_PROGRESS`, thời hạn (BR-U11-14), `contentVersion` (BR-U11-11).
2. Kiểm cấu trúc (BR-U11-12); ghi nội dung, `contentVersion + 1`, `lastSavedAt`.

## F4 - Nộp tay
## F3a - Nhập DOCX vào bản nháp DOCUMENT
1. Kiểm chủ lượt, `IN_PROGRESS`, hạn còn hiệu lực và loại bài `DOCUMENT` (BR-U09-45).
2. Gọi `DocxLearnerImportPort` (U09) trả xem trước và báo cáo, không thay đổi nội dung lượt.
3. Khi xác nhận, thêm block `LEARNER` bằng cùng kiểm `contentVersion` và `validateForSave` như F3; xung đột trả `409`, bản nháp cũ giữ nguyên.

## F4 - Nộp tay
1. Kiểm BR-U11-20.
2. Chuyển `SUBMITTED` theo BR-U11-21; trả biên nhận; audit.

## F5 - Tự nộp
1. Job `ATTEMPT_AUTO_SUBMIT` tại `deadlineAt`: nếu lượt còn `IN_PROGRESS` → nộp nội dung hiện có với `submitMode` tương ứng (BR-U11-23, 24).
2. U08 ngưng giao gọi `PublicationLifecyclePort.onRetired` (U11 cài) → tạo job `ATTEMPT_AUTO_SUBMIT {publicationId, mode = AUTO_RETIRED}`; job nộp mọi lượt `IN_PROGRESS` của publication.
3. Client: đồng hồ về 0 → gửi lần lưu cuối rồi hiện "Đã tự nộp".

## F6 - Lịch sử, biên nhận, xuất DOCX
1. Danh sách lượt của mình, đánh dấu lượt được chấm (BR-U11-31).
2. Xem lượt đã nộp (chỉ đọc), điểm/đáp án theo BR-U11-33.
3. Tải DOCX (BR-U11-34).

## F7 - Cho giảng viên và unit khác
- `SubmissionQueryPort`: danh sách lượt theo publication (U15, U16), nội dung (U13, U15), lượt được chấm.
