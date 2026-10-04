# U05 Content, Material & RAG - Business Rules

## 1. Quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-01 | Module thuộc môn. Chỉ Chủ nhiệm môn của môn tạo, đổi tên, đổi thứ tự, lưu trữ module, trên Subject Detail (trang quản lý môn của Chủ nhiệm môn). Mọi lớp của môn, kể cả lớp tạo sau, tự dùng đúng bộ module này (không sao chép, không chỉnh theo từng lớp); Class Detail không sửa được module. | Người dùng chốt 2026-10-04 |
| BR-U05-02 | Học liệu tải vào module: Chủ nhiệm môn tải trên Subject Detail là **học liệu của môn** (`class_id` rỗng), mọi lớp của môn thấy; giảng viên của lớp (kể cả Chủ nhiệm môn đang dạy lớp đó) tải trên Class Detail là **học liệu của lớp** (`class_id` = lớp), chỉ lớp đó thấy. Giảng viên không tạo/sửa module và không sửa học liệu của môn; Chủ nhiệm môn sửa, lưu trữ được mọi học liệu trong môn. | Người dùng chốt 2026-10-04 |
| BR-U05-04 | Người học thấy module `ACTIVE` của môn, trong đó học liệu `ACTIVE` của môn và của lớp mình, qua U04 (ghi danh `ACTIVE`, lớp `OPEN`); ngoài quyền → "không tìm thấy". | US-LRN-001 |

## 2. Cấu trúc

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-10 | Module là nhóm học liệu của môn; mỗi lesson là đúng một tài liệu (tệp hoặc một video) và thuộc đúng một module. Thứ tự module và lesson chỉnh bằng `order_no`; trong một module, học liệu của môn hiện trước, học liệu của lớp hiện sau, mỗi nhóm theo `order_no` riêng (giảng viên chỉ đổi thứ tự học liệu của lớp mình). | Người dùng chốt 2026-10-04 |
| BR-U05-11 | Không soạn nội dung trực tiếp, không phiên bản, không phát hành: tải lên xong là hiển thị cho người học; thay tài liệu thì tải lesson mới và lưu trữ lesson cũ. | Quyết định 2026-10-03 |
| BR-U05-12 | Mỗi module (trên Subject Detail với Chủ nhiệm môn, trên Class Detail với giảng viên của lớp) có hai nút "Tải tệp" và "Gắn link video"; bấm mở popup Upload Learning Materials đã chọn sẵn module đó. Không có bước chọn module trong popup. | Người dùng chốt 2026-10-04 |
| BR-U05-14 | Không xóa module/lesson; chỉ lưu trữ (ẩn với người học, không còn trong RAG). Lưu trữ module ẩn mọi lesson trong module. | UC 11 |

## 3. Tải lên

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-21 | `FILE`: PDF, DOCX, PPTX qua U03 purpose `MATERIAL` (≤ 50 MB, kiểm magic bytes). | FR-004, US-CNT-001 S2 |
| BR-U05-22 | `YOUTUBE`: URL một video dạng `youtube.com/watch?v=` hoặc `youtu.be/`; không nhận playlist. | FR-004 |
| BR-U05-23 | Người học tải được mọi tệp; PDF xem trực tiếp được. Tải qua token 5 phút của U03 sau khi U05 kiểm quyền. | Câu 14 |
| BR-U05-24 | Video hiển thị bằng iframe `youtube-nocookie.com`. | Thiết kế |

## 4. Quét

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-30 | Mỗi lesson mới hoặc bấm Quét lại thì `scan_status = PENDING` và gửi việc `LESSON_SCAN` (tệp) hoặc `YOUTUBE_CAPTION` (video) qua U03; request không đợi. | Quyết định 2026-10-03 |
| BR-U05-32 | Trích chữ PDF/DOCX/PPTX; không OCR. Ít hơn 50 ký tự mỗi trang trung bình → `NO_TEXT`; tệp vẫn dùng được cho người học. | Câu 13 |
| BR-U05-33 | YouTube: chỉ dùng phụ đề có sẵn, ưu tiên phụ đề thủ công tiếng Việt, rồi tiếng Anh, rồi phụ đề tự động; không có → `NO_CAPTION`. Không tự phiên âm. | Câu 3 |
| BR-U05-35 | Tạo một vector cho mỗi lesson bằng Gemini `gemini-embedding-001` (768 chiều) từ phần đầu `extracted_text` trong giới hạn đầu vào của model. | Quyết định 2026-10-03 |
| BR-U05-36 | Ghi `extracted_text`, `embedding`, `INDEXED` trong một transaction; lỗi giữa chừng không để lại kết quả dở. | US-CNT-001 S3, US-CNT-005 S3 |
| BR-U05-37 | Lỗi tạm (mạng, 429, 5xx) thử lại theo U03; hết lượt → `FAILED`. Hết trần AI hoặc AI bị tắt → `BUSY` ("Hệ thống đang bận"); lỗi vĩnh viễn (URL sai, video riêng tư) → `FAILED`. Người quản lý bấm "Quét lại" được với `FAILED`, `BUSY`, `NO_CREDIT`. | US-CNT-001 S3 |
| BR-U05-38 | Người tải lên và người quản lý phạm vi xem trạng thái quét của từng lesson. | FR-004 |
| BR-U05-39 | Embedding khi quét tính credit cho người tải lên qua `AiUsagePort` của U13 (một dòng `ai_suggestions` mỗi lần gọi, `target` là lesson); thử lại không trừ trùng. Không đủ credit → `NO_CREDIT`, học liệu vẫn xem được. | BR-U07-40…43 |

## 5. Truy xuất (retrieve)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-40 | Chỉ U13 gọi (khi người có vai trò Giảng viên/Chủ nhiệm môn soạn đề bằng AI), sau khi unit yêu cầu AI đã kiểm quyền; không có truy xuất RAG theo yêu cầu Người học. U05 kiểm lại `classId` thuộc `subjectId`. | components.md, BR-U13-03 |
| BR-U05-41 | Phạm vi tìm: lesson `ACTIVE`, `INDEXED` trong module `ACTIVE` của môn. Phạm vi lớp (giảng viên soạn bài của lớp, BR-U13-10): học liệu của môn và của lớp đó. Phạm vi môn (Chủ nhiệm môn soạn template hoặc câu hỏi cấp môn, BR-U13-11): chỉ học liệu của môn (`class_id` rỗng); có thể lọc theo danh sách lesson. Không tìm trong lesson lưu trữ hay phạm vi khác. | FR-004 |
| BR-U05-42 | `k` ≤ 10 lesson gần nhất theo cosine; mỗi lesson trả đoạn trích khớp câu hỏi nhất (tổng ≤ 12 000 ký tự) kèm `lessonId`, tiêu đề để trích dẫn. | Quyết định 2026-10-03 |
| BR-U05-43 | Gemini lỗi khi tạo vector câu hỏi → trả lỗi "tạm thời không khả dụng", không trả kết quả rỗng giả; hết trần → "Hệ thống đang bận". | FR-004 |
| BR-U05-44 | Embedding câu hỏi tính credit cho người yêu cầu AI qua `AiUsagePort`. Không đủ credit → "Không đủ credit AI"; hết trần → "Hệ thống đang bận", không trừ credit. | BR-U07-40…43 |

## 6. Thông báo và bình luận

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-60 | Chỉ giảng viên được phân công lớp đăng thông báo (UC 13) cho lớp `OPEN`; chỉ thành viên đang ghi danh và người quản lý có quyền đọc. Chủ nhiệm môn chỉ đăng khi cũng là giảng viên lớp. | US-CNT-004, UC 13 |
| BR-U05-61 | Người học `ACTIVE` trong lớp `OPEN` và giảng viên lớp bình luận dưới thông báo (UC 14), như bình luận bài đăng; không có chủ đề hỏi đáp riêng, người học không tạo bài đăng. Ngoài phạm vi trả `404`. | Người dùng chốt 2026-10-04 |
| BR-U05-62 | Thông báo: tiêu đề ≤ 200, nội dung ≤ 5 000 ký tự, markdown đã làm sạch. Bình luận: văn bản thuần ≤ 2 000 ký tự. Không HTML thô, không tệp đính kèm. | SEC-003 |
| BR-U05-63 | Thông báo và bình luận giữ tác giả và thời điểm; không sửa sau khi gửi, không xóa cứng; giảng viên lớp ẩn nội dung vi phạm với lý do. | FR-014, SEC-005 |
| BR-U05-64 | Thông báo mới: sau commit phát sự kiện, U16 báo trong app cho người học đang ghi danh (không email). Bình luận **không** tạo thông báo. | Người dùng chốt 2026-10-04 |
| BR-U05-65 | Dưới mỗi thông báo hiện 2 bình luận mới nhất và tổng số bình luận; bấm "Xem thêm bình luận" mở popup hiện toàn bộ (phân trang, cũ → mới) kèm ô viết bình luận. | Người dùng chốt 2026-10-04 |

## 7. Audit và ngoài phạm vi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-50 | Audit: tạo/lưu trữ module, tải lên, lưu trữ lesson, quét lại thủ công, ẩn thông báo hoặc bình luận. Không audit từng lần người học xem. | FR-014 |
| BR-U05-51 | Tìm kiếm/tóm tắt học liệu cho người dùng nằm ngoài phạm vi dự án. Truy xuất RAG nội bộ phục vụ U13 vẫn thuộc MVP; thông báo và bình luận không gọi RAG. | Quyết định phạm vi 2026-09-25 |
