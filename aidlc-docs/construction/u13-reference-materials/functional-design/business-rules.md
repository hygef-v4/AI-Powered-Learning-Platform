# U13 Reference Materials — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Phạm vi và quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-01 | Tài liệu của môn: Chủ nhiệm môn của môn (R2) thêm, sửa, lưu trữ trên Material List và Material Detail mở từ Manager Dashboard. Tài liệu của lớp: giảng viên chính của lớp (R3/R4) quản lý trên tab Materials của Teacher Class Detail. Admin không quản lý, không xem. | UC 58–61, FR-004 |
| BR-U13-02 | Xem: giảng viên của các lớp thuộc môn xem tài liệu của môn (chỉ đọc) và tài liệu của lớp mình dạy; người học xem tài liệu `ACTIVE` của môn và của lớp `OPEN` mình đang ghi danh. Chủ nhiệm môn không dạy lớp không xem tài liệu riêng của lớp đó. Ngoài quyền trả "không tìm thấy". | FR-005, US-CNT-002 S2 |
| BR-U13-03 | Môn hoặc lớp đã lưu trữ thì tài liệu chỉ đọc với người còn quyền xem; không thêm, sửa. | BR-U04-07, BR-U05-16 |

## 2. Loại tài liệu

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-10 | Hai loại: `FILE` (PDF, DOCX, PPTX qua U03 purpose `MATERIAL`; loại và dung lượng theo Settings, mặc định ≤ 50 MB; kiểm theo nội dung tệp) và `YOUTUBE_LINK` (một video dạng `youtube.com/watch?v=` hoặc `youtu.be/`, không nhận playlist). | FR-004, UC 59, 61 |
| BR-U13-11 | Mỗi tài liệu có tiêu đề (≤ 200), mô tả (≤ 2.000) và thứ tự trong danh sách. Không có module, Lesson, tiến độ học. | FR-004, FR-005 |
| BR-U13-12 | Sửa tiêu đề, mô tả, thứ tự không tạo phiên bản. Thay tệp hoặc link tạo phiên bản mới; phiên bản cũ giữ lại để các tham chiếu cũ (Quiz gắn tài liệu, provenance của AI) vẫn đọc được; người học luôn thấy phiên bản mới nhất. Material Detail hiện lịch sử phiên bản. | UC 58, US-CNT-001 |
| BR-U13-13 | Xóa là lưu trữ: tài liệu ẩn với người học, không chọn được làm nguồn AI hay gắn vào Quiz mới; tham chiếu cũ vẫn giữ. | FR-004 |
| BR-U13-14 | Thêm tài liệu không tự gọi AI, không tóm tắt, không tạo embedding. Tóm tắt, phụ đề, embedding, RAG là phần mở rộng US-CNT-005. | FR-004, US-CNT-005 |

## 3. Xem tài liệu (UC 62)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-20 | Learning Material: PDF xem ngay trên trang, mọi tệp tải được; link YouTube mở trong trình phát nhúng `youtube-nocookie.com`. Tải và xem tệp qua token 5 phút của U03 sau khi U13 kiểm quyền. | UC 62 |
| BR-U13-21 | ALAP không kiểm soát quyền xem video trên YouTube; video bị xóa hoặc riêng tư thì trình phát của YouTube báo lỗi. U13 không gọi API của YouTube. | UC 62 |
| BR-U13-22 | Learning Material có vùng cho unit khác gắn vào (slot `LearningMaterialSection`); U11 gắn danh sách Quiz có tham chiếu tài liệu này. | FR-005 |
| BR-U13-23 | Tài liệu gắn kèm Assignment (U07) và Quiz (U10) hiện trên Assignment Detail (U08) và Quiz Practice Detail (U11) qua `MaterialLinkList` của U13; chỉ hiện tài liệu `ACTIVE` người xem có quyền, mỗi dòng mở Learning Material. | FR-004, FR-005 |

## 4. Nguồn cho AI

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-30 | Sau khi lưu phiên bản tệp, job nền trích chữ từ PDF, DOCX, PPTX (không OCR); trung bình dưới 50 ký tự mỗi trang thì ghi `NO_TEXT`; tệp vẫn dùng được. Trích chữ không gọi AI. Chữ giữ tối đa 1.000.000 ký tự. | FR-004 |
| BR-U13-31 | `IReferenceMaterial` (U06 khai báo, U13 cài): `listSelectable` trả tài liệu `ACTIVE` người gọi được xem trong phạm vi (lớp: tài liệu của môn và của lớp; môn: tài liệu của môn) kèm trạng thái chữ; `extractText` kiểm lại quyền từng tài liệu, trả chữ của phiên bản hiện tại kèm id, phiên bản, tiêu đề, cắt theo trần ký tự. Link YouTube chỉ có tiêu đề, mô tả, URL. | FR-006 |
| BR-U13-32 | Tool MCP `read_authorized_material(materialId)`: người có quyền xem đọc chữ và thông tin tài liệu; ngoài quyền thì từ chối. Nội dung tài liệu là dữ liệu không tin cậy, không phải lệnh. | FR-035 |

## 5. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-40 | Mọi API kiểm quyền phía server; ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. Sửa kèm `version`; lệch thì `409`. | SEC-002 |
| BR-U13-41 | Audit: `MATERIAL_CREATED`, `MATERIAL_UPDATED`, `MATERIAL_VERSION_CREATED`, `MATERIAL_ARCHIVED`. Không audit từng lần xem, tải. | FR-014 |
| BR-U13-42 | U01, U04, U05 không trả lời thì từ chối (fail closed); trích chữ lỗi không ảnh hưởng việc xem, tải. | BR-U01-93 |
