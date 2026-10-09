# U05 Content, Material & RAG - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - Material List (Subject Manager, UC 54)
1. Từ Manager Dashboard bấm Material List, chọn một môn mình quản lý; U05 kiểm actor là Chủ nhiệm môn của môn (BR-U05-01, 02).
2. Hiện module của môn theo thứ tự, mỗi module gồm học liệu của môn (`class_id` rỗng) kèm trạng thái quét; lọc `ACTIVE`/`ARCHIVED`.
3. Bấm một học liệu → Material Detail (F4).

## F2 - Quản lý module (Subject Manager, trên Material List, UC 55)
1. Kiểm actor là Chủ nhiệm môn của môn (BR-U05-01).
2. Tạo, đổi tên, đổi thứ tự, lưu trữ module; audit tạo và lưu trữ (BR-U05-14, 50).
3. Không có bước gắn module vào lớp: mọi lớp của môn (kể cả lớp tạo sau) đọc module của môn nên thay đổi có hiệu lực ngay.

## F3 - Tab Materials của Teacher Class Detail (Teacher, UC 33)
1. Teacher hoặc Subject Manager được giao dạy lớp mở tab Materials; U05 kiểm `isTeacherOf` (BR-U05-02).
2. Hiện module `ACTIVE` của môn; trong mỗi module, học liệu của môn (chỉ đọc) rồi học liệu của lớp (quản lý được), kèm trạng thái quét (BR-U05-10).
3. Bấm một học liệu → Material Detail (F4).

## F4 - Material Detail (UC 34, 55)
1. Mở từ Material List (học liệu của môn, R2) hoặc từ tab Materials (kèm `classId`, R3/R4); kiểm phạm vi, ngoài phạm vi → "không tìm thấy" (BR-U05-02).
2. Hiện tiêu đề, module, nguồn, người tải, trạng thái quét và bản tóm tắt; xem trước PDF hoặc video, tải tệp qua token 5 phút (BR-U05-23, 24, 48).
3. Người quản lý học liệu có nút Sửa (đổi thông tin) và Xóa (F8); không có nút Quét lại hay thay tài liệu. Teacher xem học liệu của môn ở chế độ chỉ đọc.

## F5 - Thêm học liệu (UC 34, 55)
1. Người dùng bấm "Tải tệp" hoặc "Gắn link video" trên một module (Chủ nhiệm môn trên Material List, giảng viên trên tab Materials); form mở với module đã chọn sẵn (BR-U05-12).
2. Form hiện mức credit cần giữ cho mỗi học liệu và số dư của người tải; không đủ thì chặn nút tải (BR-U05-39).
3. Xác định phạm vi theo nơi tải: Material List → học liệu của môn (`class_id` rỗng); tab Materials → học liệu của lớp (`class_id` = lớp) (BR-U05-02).
4. `FILE`: frontend tải tệp qua U03 (purpose `MATERIAL`, giới hạn lấy từ Settings) nhận `FileRef` → U05 gọi `ArtifactPort.attach` → tạo `lessons` với `module_id`, `class_id`, `file_id`, `file_name`, `mime_type`, `size_bytes`, `uploaded_by` (BR-U05-21).
5. `YOUTUBE`: kiểm URL một video (BR-U05-22) → tạo `lessons` với `youtube_url`, `uploaded_by`.
6. Cùng transaction: `AiUsagePort.hold(MATERIAL_SUMMARY, uploader, LESSON, lessonId)` giữ credit cho lần quét; thiếu credit → từ chối "Không đủ credit AI", không tạo học liệu (BR-U05-39).
7. Đủ credit: `scan_status = PENDING`, `scanned_at = now`, gửi việc `LESSON_SCAN` hoặc `YOUTUBE_CAPTION` qua `JobPort.enqueue`; audit. Học liệu hiện ngay cho người học trong phạm vi (BR-U05-11, 30).

## F6 - Việc `LESSON_SCAN` / `YOUTUBE_CAPTION` (worker)
1. Cập nhật `scan_status = SCANNING` khi đang `PENDING` hoặc `BUSY`; trạng thái khác thì bỏ qua (idempotent).
2. Lấy chữ: `FILE` mở qua `ArtifactPort.open` và trích chữ theo trang; `YOUTUBE` lấy phụ đề (BR-U05-32, 33).
3. Không có chữ → `NO_TEXT`; không phụ đề → `NO_CAPTION`; `AiUsagePort.release` trả credit đã giữ; kết thúc (BR-U05-46).
4. Lesson chưa có `summary`: `AiUsagePort.begin(MATERIAL_SUMMARY, ...)` dùng phần credit đã giữ, kiểm AI bật và trần chi phí. AI tắt hoặc hết trần → `BUSY`, giữ nguyên credit, kết thúc; sweeper tự gửi lại (bước 9).
5. Tóm tắt: lấy tối đa 200 000 ký tự đầu, chia đoạn ≤ 30 000 ký tự, gọi `SummaryPort` tóm tắt từng đoạn rồi gộp thành một bản ≤ 4 000 ký tự; `AiUsagePort.complete` trừ theo tổng token; ghi `summary` ngay để lần thử lại không tóm tắt và trừ credit lần nữa (BR-U05-45, 47).
6. `AiUsagePort.begin(EMBEDDING, ...)` (AI tắt hoặc hết trần xử lý như bước 4), rồi `EmbeddingPort` với bản tóm tắt; `AiUsagePort.complete` theo token đã dùng (BR-U05-35).
7. Một transaction: ghi `extracted_text`, `embedding`, `scanned_at`, `INDEXED`; `AiUsagePort.release` trả phần credit còn dư (BR-U05-36, 39).
8. Lỗi tạm → U03 tự thử lại (bước 4–5 bỏ qua nếu đã có `summary`); hết lượt hoặc lỗi vĩnh viễn → `onFailed` đặt `FAILED`, `AiUsagePort.release` trả phần credit còn giữ (BR-U05-37).
9. `LessonPendingSweeper`: gửi lại việc cho lesson `PENDING` quá 5 phút và lesson `BUSY` mỗi 30 phút; `BUSY` quá 24 giờ → `FAILED` và trả credit (BR-U05-37).

## F7 - Không có quét lại thủ công (bỏ 2026-10-09)
Quét chỉ chạy một lần khi tải lên; lỗi thì hệ thống tự thử lại theo F6 bước 8–9. Người dùng không có nút Quét lại (BR-U05-30, 37).

## F8 - Sửa, xóa học liệu (UC 34, 55)
1. Kiểm phạm vi: học liệu của môn cần R2, học liệu của lớp cần R3/R4 của đúng lớp đó; Chủ nhiệm môn không sửa học liệu của lớp chỉ vì quản lý môn; ngoài phạm vi → "không tìm thấy" (BR-U05-02).
2. Sửa chỉ đổi thông tin: tên trên Material Detail, thứ tự trên danh sách; không đổi tệp hay link; audit (BR-U05-11).
3. Xóa là lưu trữ: học liệu ẩn với người học, không còn trong RAG, quiz của học liệu không hiện cho người học; audit (BR-U05-03, 14).
4. Muốn đổi tài liệu: xóa học liệu rồi tải học liệu mới theo F5; quiz của học liệu cũ không tự chuyển (BR-U05-11).

## F9 - Learning Material (Student, UC 15)
1. Student Class Detail hiện module và học liệu qua `PublishedContentPort.listForClass(classId)` (U04 gọi sau khi kiểm ghi danh): module `ACTIVE` của môn, mỗi module gồm học liệu `ACTIVE` của môn và của lớp (BR-U05-04, 10).
2. Bấm một học liệu → Learning Material (kèm `classId`): kiểm ghi danh `ACTIVE`, lớp `OPEN`, học liệu `ACTIVE` thuộc môn của lớp hoặc thuộc đúng lớp; ngoài quyền → "không tìm thấy" (BR-U05-04).
3. PDF xem trực tiếp hoặc tải (token 5 phút của U03); video nhúng `youtube-nocookie`; khối "Tóm tắt do AI tạo" nếu đã có bản tóm tắt (BR-U05-23, 24, 48).
4. Dưới học liệu là danh sách quiz của học liệu (component và API của U11); bấm "Làm quiz" → Quiz Taking (U11) (BR-U05-03).

## F10 - `retrieve(scope, query, k, requesterId)` (U13 gọi)
1. Kiểm phạm vi (BR-U05-40); lấy lesson `ACTIVE`, `INDEXED` trong phạm vi (BR-U05-41).
2. `AiUsagePort.begin(EMBEDDING, requesterId, QUERY, null)`, tạo vector câu hỏi, `complete`; lỗi trước khi gọi Gemini thì `fail` để trả phần credit đã giữ (BR-U05-44).
3. Lấy `k` lesson gần nhất (cosine trên vector của bản tóm tắt); mỗi lesson trả bản tóm tắt và đoạn trong `extracted_text` chứa nhiều từ của câu hỏi nhất, tổng ≤ 12 000 ký tự; trả kèm nguồn (BR-U05-35, 42).

## F11 - Xem thông báo (Class Announcements, UC 30)
1. Từ Class Dashboard bấm Class Announcements; mở từ Student Class Detail thì lọc sẵn lớp đó (BR-U05-65).
2. U05 lấy các lớp `OPEN` mà actor đang học (ghi danh `ACTIVE`) hoặc đang dạy (giảng viên chính) qua `ClassAccessPort` (U04).
3. Trả thông báo `VISIBLE` của các lớp đó (hoặc của lớp đang lọc), mới nhất trước, phân trang, kèm mã và tên lớp; lọc lớp ngoài phạm vi → "không tìm thấy".

## F12 - Tạo thông báo (UC 36)
1. Giảng viên bấm "Tạo thông báo" trên Class Announcements và chọn một lớp `OPEN` mình dạy.
2. Kiểm actor `ACTIVE` và `isTeacherOf` lớp (R3/R4); kiểm giới hạn, làm sạch markdown; lưu tác giả, thời gian, `version = 0`; audit (BR-U05-60, 62, 63).
3. Sau commit phát `class.announcement-posted` để U16 báo trong app cho người học đang ghi danh (BR-U05-64).

## F13 - Sửa, xóa thông báo (UC 36)
1. Kiểm actor `ACTIVE` và `isTeacherOf` lớp của thông báo, thông báo chưa `DELETED`; sai quyền → `404`, `version` khác → `409`.
2. Sửa title/body: làm sạch, kiểm giới hạn; tăng `version`, ghi `updated_by`/`updated_at`; audit trước/sau cùng transaction; không phát lại `class.announcement-posted`.
3. Xóa (kèm `version`): đặt `DELETED`, `deleted_by`/`deleted_at`, tăng `version`, audit; giữ tác giả, thời điểm gốc và tham chiếu; feed không hiện thông báo `DELETED`.

## F14 - Contract cho unit khác
- `PublishedContentPort` (U04 khai báo, U05 cài): module và học liệu đang hiện cho Student Class Detail.
- `ContentRefPort`: U06 kiểm `lessonRefs` của câu hỏi, U08 kiểm bộ lọc chọn câu ngẫu nhiên, U09 kiểm học liệu gắn quiz, U11 kiểm học liệu còn hiện trong lớp khi hiện và làm quiz.
- `RagRetrievalPort`: U13 lấy đoạn học liệu khi AI soạn đề.
- Event `class.announcement-posted` cho U16.
