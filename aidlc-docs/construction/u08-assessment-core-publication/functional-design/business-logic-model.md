# U08 Assessment Core & Publication - Business Logic Model

## F1 - Tạo và soạn bài
1. Kiểm quyền lớp (BR-U08-01); tạo `DRAFT` với loại bài.
2. Thêm thành phần: chọn từ ngân hàng (`BankQueryPort.search`, chỉ `ACTIVE`) hoặc tạo câu riêng (kiểm qua `DefinitionValidationPort`) (BR-U08-11).
3. Sắp xếp, đặt điểm, tính `totalPoints` (BR-U08-12).
4. Sửa bài `REVIEWED` → về `DRAFT` (BR-U08-14).

## F2 - Tạo bằng AI
1. Giảng viên nhập yêu cầu (loại, số câu, độ khó, chương/bài) → `AiDraftPort.request` (U13, trừ credit U07).
2. Nhận đề xuất → giảng viên chọn câu giữ lại → thêm vào bài `DRAFT` dưới dạng câu riêng, `origin = AI`, lưu `aiProposalRef` (BR-U08-21).

## F3 - Xem trước và duyệt
1. Xem trước như người học (dùng `QuestionView` của U06).
2. Duyệt: kiểm BR-U08-20 → `REVIEWED`, `reviewedBy/At`; audit.

## F4 - Phát hành cho một lớp
1. Kiểm `REVIEWED` hoặc `LOCKED` (BR-U08-22), quyền và lớp `OPEN` (BR-U08-02, 31).
2. Kiểm lịch, nộp trễ, số lượt (BR-U08-31, 32).
3. Tạo `Publication` `SCHEDULED`; bài `REVIEWED` → `LOCKED` (BR-U08-33); audit.
4. Tạo job mở (tại `opensAt`) và đóng (tại `closesAt` hoặc `lateUntil`) (BR-U08-36).

## F5 - Mở/đóng theo lịch (worker)
1. Job mở: `SCHEDULED` → `OPEN`, phát `ASSIGNMENT_OPENED` (BR-U08-35).
2. Job đóng: `OPEN` → `CLOSED`, phát `ASSIGNMENT_CLOSED`.
3. Lịch đổi → job cũ bỏ qua khi thời điểm không khớp (kiểm lại lúc chạy).

## F6 - Sửa lịch
1. Theo BR-U08-34; tạo lại job; audit.

## F7 - Ngưng giao, nhân bản, lưu trữ
1. Ngưng giao (BR-U08-40); nhân bản (BR-U08-41); lưu trữ (BR-U08-42).

## F8 - Truy vấn
1. Giảng viên: danh sách bài theo lớp/trạng thái, chi tiết, publication (UC-ASM-01).
2. Người học: bài của lớp theo BR-U08-03 (dùng bởi U11 và dashboard U04).
3. `isSubmissionOpen(publicationId, now)` trả `ON_TIME`, `LATE`, `CLOSED` cho U11.
