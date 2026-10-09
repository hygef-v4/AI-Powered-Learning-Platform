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
6. Cùng transaction: `AiUsagePort.hold(MATERIAL_SUMMARY, uploader, LESSON, lessonId, lessonId:HOLD)` giữ credit cho lần quét; thiếu credit → từ chối "Không đủ credit AI", không tạo học liệu (BR-U05-39).
7. Đủ credit: `scan_status = PENDING`, `scanned_at = now`, `scan_expires_at = now + 24 giờ`, retry count 0, gửi việc `LESSON_SCAN` hoặc `YOUTUBE_CAPTION` qua `JobPort.enqueue`; audit. Học liệu hiện ngay cho người học trong phạm vi (BR-U05-11, 30).

## F6 - Việc LESSON_SCAN / YOUTUBE_CAPTION (worker)
1. Trong transaction ngắn claim bằng UPDATE có điều kiện: PENDING/BUSY, scan_retry_at đã tới, scan_expires_at còn hạn, không lease đang giữ. Đặt SCANNING, scan_claim_id mới, lease 5 phút; không claim được thì bỏ message trùng. Mọi ghi kết quả và gia hạn lease kiểm claim/hạn; worker bị thay claim bỏ kết quả muộn.
2. Trích chữ hoặc caption. NO_TEXT/NO_CAPTION chuyển trạng thái cuối và chốt AiUsagePort.release(holdId), chưa gọi AI trả toàn bộ.
3. Chưa có summary: dùng requestRef riêng ổn định `lessonId:SUMMARY:chunkIndex` cho từng đoạn và `lessonId:SUMMARY:MERGE` cho gộp. AiUsagePort.begin(..., holdId, scanClaimId) dưới claim hợp lệ trả RUN với ticket hoặc REPLAY với CallSnapshot/checkpoint READY; REPLAY không gọi provider. BUSY giữ credit, đặt scan_retry_at = now + 30 phút; IN_PROGRESS không gọi trùng, CLOSED kết thúc quét an toàn. Worker lấy kết quả qua port, không đọc ai_suggestions trực tiếp.
4. SummaryPort tóm tắt tối đa 200 000 ký tự theo đoạn ≤ 30 000, gộp ≤ 4 000 ký tự. complete(ticket, tokens, cost, checkpoint) kiểm/khóa claim lesson rồi ghi kết quả/usage qua U13 cùng transaction; ticket/claim cũ rollback, READY replay không cộng lại; khi gộp xong ghi lessons.summary ngay. Summary đã có thì bỏ toàn bộ bước này khi retry.
5. Embedding dùng `lessonId:EMBEDDING`, AiUsagePort.begin(..., holdId, scanClaimId): REPLAY nhận vector checkpoint, RUN mới embed(summary) rồi complete với checkpoint EMBEDDING. Lời gọi bị chặn trước provider không trừ credit. Giữ kết quả đã complete làm checkpoint; chưa chắc provider đã xử lý khi timeout thì xử lý lỗi hữu hạn, không tuyên bố exactly-once cho lời gọi bên ngoài.
6. Transaction kiểm claim: ghi extracted_text, embedding, INDEXED, scanned_at và chốt hold (settle phần dùng thật, trả dư). Terminal NO_TEXT/NO_CAPTION/FAILED cũng chốt hold idempotent. Embedding FAILED không xóa summary đã lưu; lesson chỉ vào RAG khi INDEXED.
7. Lỗi tạm: nếu còn claim và chưa hết hạn/retry, tăng scan_retry_count, chuyển SCANNING → PENDING, xóa claim/lease, đặt scan_retry_at theo U03 backoff rồi ném RetryableJobException. Hết 5 retry, lỗi vĩnh viễn hoặc tới scan_expires_at → FAILED và chốt hold. onFailed dùng cùng kiểm claim/trạng thái, không thay trạng thái cuối đã ghi bởi worker khác.
8. LessonPendingSweeper mỗi phút: PENDING quá 5 phút và retry_at tới thì gửi lại; BUSY chỉ gửi khi retry_at tới (30 phút). SCANNING lease hết hạn: CAS thu hồi claim, tăng retry/recovery count, chuyển PENDING hoặc FAILED khi hết giới hạn; worker cũ không ghi được. Mọi trạng thái chưa cuối tới scan_expires_at (24 giờ cố định từ tạo lesson) → FAILED và chốt hold.
9. HoldId tra qua AiUsagePort.findHold(LESSON, lessonId, lessonId:HOLD) trả HoldSnapshot, không đọc repository U13; không dựa vào bộ nhớ worker. Scanner 25 giờ của U13 là phương án dự phòng trả credit nếu luồng terminal thất lạc.

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
