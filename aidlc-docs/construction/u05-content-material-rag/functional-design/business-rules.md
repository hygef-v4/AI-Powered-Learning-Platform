# U05 Content, Material & RAG - Business Rules

## 1. Quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-01 | Nội dung cấp môn: Chủ nhiệm môn của môn và ADMIN được xem/sửa/phát hành. | US-CNT-001 |
| BR-U05-02 | Nội dung cấp lớp: giảng viên của lớp, Chủ nhiệm môn của môn, ADMIN. Giảng viên không sửa nội dung cấp môn. | US-CNT-002 S2 |
| BR-U05-03 | Người quản lý lớp được chọn bài cấp môn đã phát hành để đưa vào lớp và gỡ ra. | Câu 2 |
| BR-U05-04 | Người học chỉ thấy nội dung qua U04 (ghi danh `ACTIVE`, lớp `OPEN`); ngoài quyền → "không tìm thấy". | US-LRN-001 |

## 2. Cấu trúc và phiên bản

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-10 | Cấu trúc Chương → Bài → Mục; thứ tự chỉnh bằng `orderNo`. | Câu 1 |
| BR-U05-11 | Sửa bài đã phát hành tạo bản `DRAFT` mới (sao chép mục); học viên vẫn thấy bản `PUBLISHED` tới khi bản mới được phát hành. | Câu 5 |
| BR-U05-12 | Phát hành: bản `DRAFT` → `PUBLISHED`, bản cũ → `SUPERSEDED`; audit. Phiên bản cũ giữ nguyên để trích dẫn AI còn đúng. | Câu 5, FR-014 |
| BR-U05-13 | Phát hành cần ít nhất 1 mục; mục `FILE`/`YOUTUBE` **không** cần xử lý RAG xong mới phát hành. | Thiết kế |
| BR-U05-14 | Không xóa chương/bài; chỉ lưu trữ (ẩn với học viên, không còn trong RAG). | UC-CNT-01 |
| BR-U05-15 | Lớp liên kết bài cấp môn luôn hiển thị bản `PUBLISHED` mới nhất. | Câu 12 |

## 3. Mục nội dung

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-20 | `TEXT`: markdown ≤ 50 000 ký tự, hiển thị đã làm sạch (không HTML thô, không script). | SEC-003 |
| BR-U05-21 | `FILE`: PDF, DOCX, PPTX qua U03 purpose `MATERIAL` (≤ 50 MB, kiểm magic bytes). | FR-004, US-CNT-001 S2 |
| BR-U05-22 | `YOUTUBE`: URL dạng `youtube.com/watch?v=`, `youtu.be/`, `youtube.com/playlist?list=`; mỗi phiên bản bài tối đa 1 mục YouTube; playlist ≤ 50 video. | FR-004 |
| BR-U05-23 | Học viên tải được mọi file; PDF có thể xem trực tiếp trên trình duyệt. Tải qua token 5 phút của U03 sau khi U05 kiểm quyền. | Câu 14 |
| BR-U05-24 | Video hiển thị bằng iframe `youtube-nocookie.com`. | Thiết kế |

## 4. Xử lý RAG

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-30 | Mục `TEXT`/`FILE` và từng video YouTube có một `SourceDocument`, dùng lại theo `contentKey`; phiên bản mới giữ nguyên mục thì không xử lý lại. | Thiết kế |
| BR-U05-31 | Xử lý chạy nền bằng job U02; request không đợi. | services.md |
| BR-U05-32 | Trích chữ PDF/DOCX/PPTX; không OCR. Ít hơn 50 ký tự mỗi trang trung bình → `NO_TEXT`; file vẫn dùng được cho học viên. | Câu 13 |
| BR-U05-33 | YouTube: chỉ dùng caption có sẵn, ưu tiên caption thủ công tiếng Việt, rồi tiếng Anh, rồi caption tự động; không có → `NO_CAPTION`. Không tự phiên âm. | Câu 3 |
| BR-U05-34 | Cắt đoạn ~3 000 ký tự, chồng lấn ~400 ký tự; giữ số trang hoặc timestamp. | Thiết kế |
| BR-U05-35 | Tạo vector bằng Gemini `gemini-embedding-001` (768 chiều) qua `EmbeddingPort`. | Câu 11 |
| BR-U05-36 | Chỉ ghi đoạn khi toàn bộ tài liệu xử lý xong (thay thế trong một transaction); lỗi giữa chừng không để lại đoạn dở. | US-CNT-001 S3, US-CNT-005 S3 |
| BR-U05-37 | Lỗi tạm (mạng, 429, 5xx) retry theo U02; lỗi vĩnh viễn (URL sai, video riêng tư) → `FAILED` không retry tự động. Người quản lý bấm "Thử lại" được với `FAILED`. | US-CNT-001 S3 |
| BR-U05-38 | Người tải lên và người quản lý phạm vi xem trạng thái từng tài liệu, từng video. | FR-004 |

## 5. Truy xuất (retrieve)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-40 | Chỉ U13 gọi (khi soạn đề bằng AI), sau khi unit yêu cầu AI (U06, U08, U10) đã kiểm quyền người dùng; U05 kiểm lại `classId` thuộc `subjectId`. | components.md |
| BR-U05-41 | Phạm vi tìm: bài `PUBLISHED` hiện tại của môn + của lớp + bài cấp môn liên kết vào lớp; có thể lọc theo danh sách bài. Không tìm trong bản nháp, bài lưu trữ hay phạm vi khác. | US-CNT-003 S2, Câu 7 |
| BR-U05-42 | `k` ≤ 20; kết quả kèm `lessonId`, `lessonVersionId`, tiêu đề, trang hoặc timestamp để trích dẫn. | Câu 7 |
| BR-U05-43 | Gemini lỗi khi tạo vector câu hỏi → trả lỗi "tạm thời không khả dụng", không trả kết quả rỗng giả. | US-CNT-003 S2 |

## 6. Audit và ngoài phạm vi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-50 | Audit: phát hành, lưu trữ chương/bài, liên kết/gỡ bài cấp môn, retry thủ công. Không audit từng lần học viên xem. | FR-014 |
| BR-U05-51 | US-CNT-003 (tìm kiếm/tóm tắt) và US-CNT-004 (thông báo/hỏi đáp) là Phase 2, **chưa thiết kế**, chờ nhóm hội ý. | Câu 15 |
