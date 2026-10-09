# U05 Content, Material & RAG - Business Rules

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-01 | Module thuộc môn; chỉ Chủ nhiệm môn của môn (R2) tạo, đổi tên, đổi thứ tự, lưu trữ module trên Material List (UC 54–55). Mọi lớp của môn dùng chung, không chỉnh theo lớp. | FR-004, R2 |
| BR-U05-02 | Học liệu môn (`class_id` rỗng) do Chủ nhiệm môn quản lý (UC 54–55, R2); học liệu lớp do giảng viên được giao dạy lớp quản lý (UC 33–34, R3/R4). Chủ nhiệm môn không sửa học liệu riêng của lớp mình không dạy; Teacher chỉ đọc học liệu môn; Admin không quản lý học liệu. Kiểm module/môn/lớp cùng phạm vi. | FR-004/005; người dùng chốt 2026-10-09 |
| BR-U05-03 | Quiz gắn với một học liệu (UC 35): Chủ nhiệm môn tạo quiz cho học liệu môn (mọi lớp của môn thấy), Teacher tạo quiz cho học liệu lớp mình dạy. quiz là quiz luyện tập: U08 giữ vòng đời và câu, U09 soạn cài đặt trên Quiz Detail (mở từ tab Evals hoặc Quiz List, người soạn chọn học liệu), U11 cho làm; U05 cung cấp tham chiếu học liệu (`ContentRefPort`) và chừa chỗ trên Learning Material cho danh sách quiz của học liệu (component U11, nút "Làm quiz" → Quiz Taking). Học liệu lưu trữ thì quiz của nó không hiện cho người học. | UC 35; người dùng chốt 2026-10-09 |
| BR-U05-04 | Người học thấy module `ACTIVE` của môn, trong đó học liệu `ACTIVE` của môn và của lớp mình, trên Student Class Detail và Learning Material (UC 15), qua U04 (ghi danh `ACTIVE`, lớp `OPEN`); ngoài quyền → "không tìm thấy". | US-LRN-001, UC 15 |

## 2. Cấu trúc

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-10 | Module là nhóm học liệu của môn; mỗi lesson là đúng một tài liệu (tệp hoặc một video) và thuộc đúng một module. Thứ tự module và lesson chỉnh bằng `order_no`; trong một module, học liệu của môn hiện trước, học liệu của lớp hiện sau, mỗi nhóm theo `order_no` riêng (giảng viên chỉ đổi thứ tự học liệu của lớp mình). | Người dùng chốt 2026-10-04 |
| BR-U05-11 | Không soạn nội dung trực tiếp, không phiên bản, không phát hành: tải lên xong là hiển thị cho người học. Không thay được tài liệu của học liệu đã tải: Sửa trên Material Detail chỉ đổi thông tin (tên; thứ tự đổi trên danh sách), không đổi tệp hay link. Muốn đổi tài liệu thì xóa học liệu rồi tải học liệu mới; quiz của học liệu cũ không tự chuyển, người soạn gắn lại trên Quiz Detail. | Quyết định 2026-10-03; người dùng chốt 2026-10-09 |
| BR-U05-12 | Mỗi module (trên Material List với Chủ nhiệm môn, trên tab Materials của Teacher Class Detail với giảng viên của lớp) có hai nút "Tải tệp" và "Gắn link video"; bấm mở form thêm học liệu hỗ trợ đã chọn sẵn module đó. Không có bước chọn module trong popup. | Người dùng chốt 2026-10-04 |
| BR-U05-14 | Không xóa module/lesson; nút Xóa trên Material Detail là lưu trữ (ẩn với người học, không còn trong RAG). Lưu trữ module ẩn mọi lesson trong module. | UC 34, 55 |

## 3. Tải lên

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-21 | `FILE`: PDF, DOCX, PPTX qua U03 purpose `MATERIAL`; loại và dung lượng tối đa theo Settings (BR-U03-02, mặc định ≤ 50 MB), kiểm magic bytes. | FR-004, US-CNT-001 S2 |
| BR-U05-22 | `YOUTUBE`: URL một video dạng `youtube.com/watch?v=` hoặc `youtu.be/`; không nhận playlist. | FR-004 |
| BR-U05-23 | Người học tải được mọi tệp; PDF xem trực tiếp được. Tải qua token 5 phút của U03 sau khi U05 kiểm quyền. | Câu 14 |
| BR-U05-24 | Video hiển thị bằng iframe `youtube-nocookie.com`. | Thiết kế |

## 4. Quét

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-30 | Mỗi học liệu chỉ quét một lần, ngay khi tải lên: `scan_status = PENDING` và gửi việc `LESSON_SCAN` (tệp) hoặc `YOUTUBE_CAPTION` (video) qua U03; request không đợi. Không có nút Quét lại. | Quyết định 2026-10-03; người dùng chốt 2026-10-09 |
| BR-U05-32 | Trích chữ PDF/DOCX/PPTX; không OCR. Ít hơn 50 ký tự mỗi trang trung bình → `NO_TEXT`; tệp vẫn dùng được cho người học. | Câu 13 |
| BR-U05-33 | YouTube: chỉ dùng phụ đề có sẵn, ưu tiên phụ đề thủ công tiếng Việt, rồi tiếng Anh, rồi phụ đề tự động; không có → `NO_CAPTION`. Không tự phiên âm. | Câu 3 |
| BR-U05-35 | Tạo một vector cho mỗi lesson bằng Gemini `gemini-embedding-001` (768 chiều) từ bản tóm tắt của lesson (BR-U05-45), để RAG tìm theo nội dung chính của cả tài liệu thay vì chỉ phần đầu. | Quyết định 2026-10-03; người dùng chốt 2026-10-09 |
| BR-U05-36 | Ghi `extracted_text`, `embedding`, `INDEXED` trong một transaction; lỗi giữa chừng không để lại kết quả dở. Riêng `summary` được ghi ngay khi tạo xong để lần chạy lại không tóm tắt và trừ credit lần nữa. | US-CNT-001 S3, US-CNT-005 S3 |
| BR-U05-37 | Lỗi thì hệ thống tự quét lại, người dùng không phải bấm: lỗi tạm (mạng, 429, 5xx) thử lại theo U03; hết trần AI hoặc AI bị tắt → `BUSY` ("Hệ thống đang bận, sẽ tự thử lại"), sweeper tự gửi lại việc mỗi 30 phút trong tối đa 24 giờ. Hết lượt thử, quá 24 giờ hoặc lỗi vĩnh viễn (URL sai, video riêng tư) → `FAILED`, trả lại credit đã giữ; học liệu vẫn xem được nhưng không có tóm tắt và không vào RAG. | US-CNT-001 S3; người dùng chốt 2026-10-09 |
| BR-U05-38 | Người tải lên và người quản lý phạm vi xem trạng thái quét của từng lesson. | FR-004 |
| BR-U05-39 | Không đủ credit thì không tải lên được: khi tạo học liệu, U05 giữ trước credit của người tải lên qua `AiUsagePort.hold` của U13 theo mức tối đa của một lần quét (tóm tắt và embedding, tính từ giới hạn đầu vào BR-U05-47); thiếu credit → từ chối "Không đủ credit AI", không tạo học liệu. Form tải lên hiện mức cần giữ và số dư để chặn trước. Quét xong trừ theo token thật (mỗi lần gọi một dòng `ai_suggestions`, `target` là lesson) và trả phần dư; `NO_TEXT`, `NO_CAPTION`, `FAILED` trả lại toàn bộ phần còn giữ; tự thử lại không trừ trùng. | BR-U07-40…43; người dùng chốt 2026-10-09 |

## 4a. Tóm tắt học liệu (thêm 2026-10-09)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-45 | Mỗi lesson mới (tệp có chữ hoặc video có phụ đề) được AI tự tóm tắt trong bước quét, sau khi trích chữ và trước khi tạo vector. Đây là luồng phụ của việc tải học liệu: Add/Update/Delete Learning Material và Add/Update/Delete Subject Material (UC 34, 55 theo bản 73 UC). Người dùng không có nút yêu cầu tóm tắt riêng. | Người dùng chốt 2026-10-09 |
| BR-U05-46 | Người tải lên là người tạo bản tóm tắt và chịu credit. Chỉ trừ credit khi thật sự gọi AI bên ngoài (Gemini): không có chữ (`NO_TEXT`), không có phụ đề (`NO_CAPTION`) hoặc quét thất bại thì không trừ và trả lại phần đã giữ. Không đủ credit thì không tải lên được (BR-U05-39). | Người dùng chốt 2026-10-09, BR-U07-40…43 |
| BR-U05-47 | Đầu vào tóm tắt là tối đa 200 000 ký tự đầu của `extracted_text`, chia đoạn ≤ 30 000 ký tự; AI tóm tắt từng đoạn rồi gộp thành một bản bằng tiếng Việt, ≤ 4 000 ký tự, chỉ dựa trên nội dung học liệu. Model lấy từ mục AI trên Settings (U13 khai báo loại việc `MATERIAL_SUMMARY`). | Thiết kế 2026-10-09 |
| BR-U05-48 | Bản tóm tắt hiện trên Learning Material cho mọi người được xem học liệu đó (Student, Teacher, Subject Manager trong phạm vi), kèm nhãn "Tóm tắt do AI tạo"; người quản lý xem thêm trên Material Detail. Không sửa tay. Mỗi học liệu chỉ được tóm tắt một lần; lần tự thử lại không tóm tắt lại nếu đã có `summary`. | Người dùng chốt 2026-10-09 |

## 5. Truy xuất (retrieve)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-40 | Chỉ U13 gọi, khi Teacher hoặc Chủ nhiệm môn nhờ AI soạn đề (luồng phụ, người dùng chốt giữ 2026-10-09), sau khi unit yêu cầu AI đã kiểm quyền; không có truy xuất RAG theo yêu cầu Người học. U05 kiểm lại `classId` thuộc `subjectId`. | components.md, BR-U13-03 |
| BR-U05-41 | Phạm vi tìm: lesson `ACTIVE`, `INDEXED` trong module `ACTIVE` của môn. Phạm vi lớp (giảng viên soạn bài của lớp, BR-U13-10): học liệu của môn và của lớp đó. Phạm vi môn (Chủ nhiệm môn soạn quiz, câu hỏi hoặc nội dung cấp môn, BR-U13-11): chỉ học liệu của môn (`class_id` rỗng); có thể lọc theo danh sách lesson. Không tìm trong lesson lưu trữ hay phạm vi khác. | FR-004 |
| BR-U05-42 | `k` ≤ 10 lesson gần nhất theo cosine; mỗi lesson trả đoạn trích khớp câu hỏi nhất (tổng ≤ 12 000 ký tự) kèm `lessonId`, tiêu đề để trích dẫn. | Quyết định 2026-10-03 |
| BR-U05-43 | Gemini lỗi khi tạo vector câu hỏi → trả lỗi "tạm thời không khả dụng", không trả kết quả rỗng giả; hết trần → "Hệ thống đang bận". | FR-004 |
| BR-U05-44 | Embedding câu hỏi tính credit cho người yêu cầu AI qua `AiUsagePort`. Không đủ credit → "Không đủ credit AI"; hết trần → "Hệ thống đang bận", không trừ credit. | BR-U07-40…43 |

## 6. Thông báo lớp

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-60 | Teacher hoặc Subject Manager được giao dạy lớp (R3/R4) tạo, sửa, xóa thông báo của lớp `OPEN` (UC 36); Student đang ghi danh và giảng viên lớp đọc (UC 30). Admin không vào lớp. | FR-023, UC 30, 36 |
| BR-U05-62 | Thông báo: tiêu đề ≤ 200, nội dung ≤ 5 000 ký tự, markdown đã làm sạch. Không HTML thô, không tệp đính kèm. | SEC-003 |
| BR-U05-63 | Thông báo giữ tác giả/thời điểm gốc; sửa title/body kiểm version, updated_by/updated_at và audit trước/sau. Xóa mềm `DELETED` với deleted_by/deleted_at, loại khỏi feed, giữ tham chiếu/audit. | FR-023, UC 36 |
| BR-U05-64 | Thông báo mới: sau commit phát sự kiện, U16 báo trong app cho người học đang ghi danh (không email). Sửa hoặc xóa không gửi lại thông báo. | Người dùng chốt 2026-10-04 |
| BR-U05-65 | Không có bình luận dưới thông báo (người dùng chốt bỏ 2026-10-09). Màn Class Announcements mở từ Class Dashboard hiện thông báo của mọi lớp mình học hoặc dạy, mới nhất trước, lọc được theo lớp; mở từ một lớp thì lọc sẵn lớp đó. | UC 30; người dùng chốt 2026-10-09 |

## 7. Audit và ngoài phạm vi

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U05-50 | Audit tạo/lưu trữ module, tải/sửa thông tin/lưu trữ lesson, tạo/sửa/xóa mềm thông báo; che dữ liệu nhạy cảm, không audit từng lần xem. | FR-014/023 |
| BR-U05-51 | Tìm kiếm học liệu theo yêu cầu người dùng nằm ngoài phạm vi. Tóm tắt chỉ tạo tự động khi tải lên (BR-U05-45). Truy xuất RAG nội bộ phục vụ AI soạn đề của U13 vẫn thuộc MVP; thông báo không gọi RAG. | Quyết định phạm vi 2026-09-25; người dùng chốt 2026-10-09 |
