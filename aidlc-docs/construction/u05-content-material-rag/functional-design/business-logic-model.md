# U05 Content, Material & RAG - Business Logic Model

## F1 - Quản lý module (Chủ nhiệm môn, trên Subject Detail)
1. Kiểm actor là Chủ nhiệm môn của môn (BR-U05-01).
2. Tạo, đổi tên, đổi thứ tự, lưu trữ module của môn; audit tạo và lưu trữ.
3. Không có bước gắn module vào lớp: Class Detail của mọi lớp thuộc môn (kể cả lớp tạo sau) đọc module của môn qua `listForClass` nên thay đổi có hiệu lực ngay ở mọi lớp.

## F2 - Tải học liệu vào module (nút "Tải tệp" / "Gắn link video" của module)
1. Người dùng bấm nút trên một module (Chủ nhiệm môn trên Subject Detail, giảng viên trên Class Detail); popup Upload Learning Materials mở với module đã chọn (BR-U05-12).
2. Xác định phạm vi học liệu theo nơi tải: Subject Detail → học liệu của môn (`class_id` rỗng); Class Detail → học liệu của lớp (`class_id` = lớp) (BR-U05-02).
3. `FILE`: frontend tải tệp qua U03 (`MATERIAL`) nhận `FileRef` → U05 gọi `ArtifactPort.attach` → tạo `lessons` với `module_id`, `class_id`, `file_id`, `file_name`, `mime_type`, `size_bytes`.
4. `YOUTUBE`: kiểm URL một video (BR-U05-22) → tạo `lessons` với `youtube_url`.
5. Cùng transaction: `scan_status = PENDING`, `scanned_at = now`, gửi việc `LESSON_SCAN` hoặc `YOUTUBE_CAPTION` qua `JobPort.enqueue`; audit. Lesson hiển thị ngay cho người học trong phạm vi (BR-U05-11, 30).

## F3 - Việc `LESSON_SCAN` / `YOUTUBE_CAPTION` (worker)
1. Cập nhật `scan_status = SCANNING` khi đang `PENDING`; trạng thái khác thì bỏ qua (idempotent).
2. Lấy chữ: `FILE` mở qua `ArtifactPort.open` và trích chữ theo trang; `YOUTUBE` lấy phụ đề (BR-U05-32, 33).
3. Không có chữ → `NO_TEXT`; không phụ đề → `NO_CAPTION`; kết thúc.
4. Gọi `AiUsagePort.begin(EMBEDDING, uploader, LESSON, lessonId)`: AI tắt hoặc hết trần → `BUSY`; thiếu credit → `NO_CREDIT`; kết thúc.
5. Gọi `EmbeddingPort` với phần đầu văn bản, rồi `AiUsagePort.complete` theo token đã dùng.
6. Một transaction: ghi `extracted_text`, `embedding`, `scanned_at`, `INDEXED` (BR-U05-35, 36).
7. Lỗi tạm → U03 thử lại; hết lượt hoặc lỗi vĩnh viễn → `onFailed` đặt `FAILED`, `AiUsagePort.fail` trả phần credit đã giữ (BR-U05-37, 39).
8. `PendingSweeper` của U05 gửi lại việc cho lesson còn `PENDING` quá 5 phút.

## F4 - Quét lại thủ công
1. Người quản lý bấm "Quét lại" trên lesson `FAILED`, `BUSY` hoặc `NO_CREDIT` → `PENDING`, gửi việc mới; audit.

## F5 - Người học xem và tải
1. U04 gọi `PublishedContentPort.listForClass(classId)` sau khi đã kiểm ghi danh; trả module `ACTIVE` của môn, mỗi module gồm lesson `ACTIVE` của môn (`class_id` rỗng) và của lớp đó.
2. Popup View Learning Material: tệp PDF xem trực tiếp hoặc tải; video nhúng `youtube-nocookie`.
3. Bấm tải tệp → U05 kiểm `ClassAccessPort.isActiveStudent`, lớp `OPEN`, lesson `ACTIVE` là học liệu của môn hoặc của lớp đó → `issueDownloadToken` (BR-U05-23).

## F6 - `retrieve(scope, query, k, requesterId)`
1. Kiểm phạm vi (BR-U05-40); lấy lesson `ACTIVE`, `INDEXED` trong phạm vi (BR-U05-41).
2. `AiUsagePort.begin(EMBEDDING, requesterId, QUERY, null)`, tạo vector câu hỏi, `complete`; lỗi trước khi gọi Gemini thì `fail` để trả phần credit đã giữ (BR-U05-44).
3. Lấy `k` lesson gần nhất (cosine); trong từng `extracted_text` chọn đoạn chứa nhiều từ của câu hỏi nhất, tổng ≤ 12 000 ký tự; trả kèm nguồn (BR-U05-42).

## F7 - Thông báo (UC 13)
1. U04 kiểm actor là giảng viên lớp `OPEN`; U05 kiểm lại `classId`. Ngoài phạm vi trả `404`.
2. Giảng viên đăng `announcements`: kiểm giới hạn, làm sạch markdown, lưu tác giả và thời gian (BR-U05-60, 62, 63).
3. Sau commit phát `class.announcement-posted` cho U16 (thông báo trong app cho người học đang ghi danh) (BR-U05-64).
4. Danh sách thông báo phân trang, mới nhất trước; mỗi thông báo kèm 2 bình luận mới nhất và tổng số bình luận (BR-U05-65). Giảng viên ẩn thông báo vi phạm với lý do và audit.

## F8 - Bình luận dưới thông báo (UC 14)
1. Kiểm actor là người học `ACTIVE` hoặc giảng viên của lớp `OPEN` chứa thông báo đang hiển thị (BR-U05-61).
2. Ghi `announcement_comments` (văn bản thuần ≤ 2 000 ký tự, tác giả, thời gian) (BR-U05-62, 63). Không phát sự kiện, không tạo thông báo (BR-U05-64).
3. "Xem thêm bình luận": trả toàn bộ bình luận đang hiển thị của thông báo, phân trang cũ → mới (BR-U05-65).
4. Giảng viên lớp ẩn bình luận vi phạm với lý do; audit. Không sửa, không xóa cứng.
