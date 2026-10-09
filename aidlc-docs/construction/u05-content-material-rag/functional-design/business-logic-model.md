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
3. Người quản lý học liệu có nút Sửa (đổi thông tin) và Xóa (F8); không có nút Quét lại hay thay tài liệu. Teacher xem học liệu của môn chỉ đọc nội dung; vẫn có nút Tóm tắt tài liệu theo quyền xem (F7).

## F5 - Thêm học liệu (UC 34, 55)
1. Người quản lý tải tệp qua U03 purpose MATERIAL hoặc gắn một link YouTube; kiểm R2 cho môn hoặc R3/R4 cho lớp, Settings loại/dung lượng, module và nguồn hợp lệ.
2. Gắn FileRef và tạo lesson với uploaded_by; không quote/hold/reserve AI, không chặn upload khi thiếu credit.
3. Cùng transaction đặt scan_status PENDING, scanned_at now, scan_expires_at now + 24 giờ, retry count 0, summary_requested_by/at rỗng; enqueue LESSON_SCAN (jobs.triggered) hoặc YOUTUBE_CAPTION (jobs.youtube) và ghi audit. Học liệu hiện ngay trong phạm vi.

## F6 - Trích chữ/phụ đề sau upload (worker)
1. CAS claim PENDING; lease 5 phút, deadline từ upload, mọi ghi kiểm claim/hạn. Trích chữ/phụ đề theo giới hạn, không gọi AI/credit.
2. Thành công lưu extracted_text và EXTRACTED, kết thúc giai đoạn. Không chữ/phụ đề thành NO_TEXT/NO_CAPTION; nguồn vẫn xem/tải được.
3. Lỗi tạm hoặc lease mất phục hồi PENDING qua sweeper mỗi phút, tối đa 5 retry/recovery/backoff U03; hết hạn/lỗi vĩnh viễn thành FAILED. Không có HOLD để hoàn.

## F7 - Yêu cầu Tóm tắt tài liệu từ View Material
1. Student, Teacher hoặc Subject Manager bấm nút; server kiểm lại quyền xem như F4/F9, actor/module/lesson ACTIVE, Student ghi danh ACTIVE và lớp OPEN. Teacher được tóm tắt học liệu môn đang xem dù không được sửa; Admin bị từ chối.
2. Khóa lesson trong transaction. Có summary hoặc yêu cầu đã nhận: trả trạng thái/kết quả hiện có, không giữ thêm credit. Chưa EXTRACTED hoặc NO_TEXT/NO_CAPTION/FAILED: báo chưa thể tóm tắt. Gọi AiUsagePort.checkAvailability (U13, Settings/kill-switch/trần/rate) trước nhận; bị chặn hoặc thiếu credit giữ EXTRACTED, không lưu yêu cầu/HOLD, có thể bấm sau.
3. GET summary-credit lấy quote; POST summary với Idempotency-Key: hold(MATERIAL_SUMMARY, requester, LESSON, lessonId, lessonId:HOLD). Cùng transaction ghi summary_requested_by/at, scan_status PENDING, scanned_at now, scan_expires_at now + 24 giờ, reset retry/claim, enqueue MATERIAL_SUMMARY; audit MATERIAL_SUMMARY_REQUESTED (lessonId, actorId, thời điểm, không nội dung/tệp/prompt) cùng transaction. Ràng buộc một yêu cầu/lesson và lock bảo đảm hai người bấm chỉ một payer; lỗi reserve rollback toàn bộ bước nhận yêu cầu.
4. Worker MATERIAL_SUMMARY lấy payer từ summary_requested_by và HOLD (không uploaded_by), CAS claim/lease. Dùng extracted_text đã lưu; requestRef lessonId:SUMMARY:chunkIndex, lessonId:SUMMARY:MERGE; begin trả RUN/REPLAY/BUSY/IN_PROGRESS/CLOSED. READY replay không gọi provider; BUSY chờ 30 phút, không kéo dài hạn. Không đọc repository U13 trực tiếp.
5. SummaryPort chia tối đa 200 000 ký tự thành đoạn ≤ 30 000, gộp tiếng Việt ≤ 4 000. complete/checkpoint cùng transaction kiểm claim; ghi summary ngay khi gộp xong. Retry bỏ lời gọi đã có checkpoint, không tóm tắt lại nếu có summary.
6. Embedding dùng requestRef lessonId:EMBEDDING, cùng HOLD (không reserve thêm); hoàn tất ghi embedding/INDEXED và chốt HOLD. Embedding lỗi giữ summary; chưa INDEXED không vào RAG.
7. Retry/recovery hữu hạn như F6; deadline 24 giờ từ summary_requested_at. Lỗi/hết hạn chốt lượng AI thật và trả dư. findHold phục hồi qua DTO; scanner U13 25 giờ từ yêu cầu dự phòng. Complete/release serialize khóa HOLD; worker cũ mất claim không ghi được. Không hứa exactly-once provider khi timeout/crash.
8. Không có nút Quét lại hoặc tạo lại sau yêu cầu đã nhận; lỗi tạm tự retry. Người khác xem dùng cùng trạng thái/summary, không trả thêm credit. Lưu trữ trước worker bắt đầu hủy yêu cầu/chốt HOLD; nếu đã chạy, kiểm lại trạng thái trước ghi, chốt lượng dùng thật và không đưa lesson lưu trữ vào RAG.

## F8 - Sửa, xóa học liệu (UC 34, 55)
1. Kiểm phạm vi: học liệu của môn cần R2, học liệu của lớp cần R3/R4 của đúng lớp đó; Chủ nhiệm môn không sửa học liệu của lớp chỉ vì quản lý môn; ngoài phạm vi → "không tìm thấy" (BR-U05-02).
2. Sửa chỉ đổi thông tin: tên trên Material Detail, thứ tự trên danh sách; không đổi tệp hay link; audit (BR-U05-11).
3. Xóa là lưu trữ: học liệu ẩn với người học, không còn trong RAG, quiz của học liệu không hiện cho người học; audit (BR-U05-03, 14).
4. Muốn đổi tài liệu: xóa học liệu rồi tải học liệu mới theo F5; quiz của học liệu cũ không tự chuyển (BR-U05-11).

## F9 - Learning Material (Student, UC 15)
1. Student Class Detail hiện module và học liệu qua `PublishedContentPort.listForClass(classId)` (U04 gọi sau khi kiểm ghi danh): module `ACTIVE` của môn, mỗi module gồm học liệu `ACTIVE` của môn và của lớp (BR-U05-04, 10).
2. Bấm một học liệu → Learning Material (kèm `classId`): kiểm ghi danh `ACTIVE`, lớp `OPEN`, học liệu `ACTIVE` thuộc môn của lớp hoặc thuộc đúng lớp; ngoài quyền → "không tìm thấy" (BR-U05-04).
3. PDF xem trực tiếp hoặc tải (token 5 phút của U03); video nhúng youtube-nocookie; nút Tóm tắt tài liệu/trạng thái/kết quả dùng chung theo F7 (BR-U05-23, 24, 48).
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
