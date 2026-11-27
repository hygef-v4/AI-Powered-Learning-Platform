# U13 Reference Materials — business-rules

**AS-BUILT sync 2026-10-30:** [as-built-baseline.md](../code/as-built-baseline.md); source-observed, không phải fresh test/runtime acceptance.


> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

**Yêu cầu đã chốt 2026-11-08:** Áp dụng các [quyết định U04/U13/U14](../../../inception/requirements/2026-11-07-u04-u13-u14-reconciliation-questions.md). Đây là baseline yêu cầu để thiết kế test; không phải xác nhận code/runtime hoặc coverage đã đầy đủ. Các snapshot và số kiểm thử có ngày cũ là lịch sử.

## 1. Phạm vi và quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-01 | Tài liệu của môn: Chủ nhiệm môn của môn (R2) thêm, sửa, lưu trữ trên Material List và Material Detail mở từ Manager Dashboard. Tài liệu của lớp: giảng viên chính của lớp (R3/R4) quản lý trên tab Materials của Teacher Class Detail. Admin không quản lý, không xem. | UC 58–61, FR-004 |
| BR-U13-02 | Xem: giảng viên của các lớp thuộc môn xem tài liệu của môn (chỉ đọc) và tài liệu của lớp mình dạy; người học xem tài liệu `ACTIVE` của môn và của lớp `OPEN` mình đang ghi danh. Chủ nhiệm môn không dạy lớp không xem tài liệu riêng của lớp đó. Ngoài quyền trả "không tìm thấy". | FR-005, US-CNT-002 S2 |
| BR-U13-03 | Môn hoặc lớp đã lưu trữ thì tài liệu chỉ đọc với người còn quyền xem; không thêm, sửa. | BR-U04-07, BR-U05-16 |

## 2. Loại tài liệu

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-10 | Hai loại: `FILE` (PDF, DOCX, PPTX, XLSX qua U03 purpose `MATERIAL`; danh sách định dạng cố định, Admin chỉ chỉnh dung lượng qua Settings, mặc định ≤ 50 MB; kiểm theo nội dung tệp; XLSX phải được nhận dạng là bảng tính OOXML thường, tệp do công cụ ghi khác chuẩn Excel mà bị nhận là XLSB/XLSM thì bị từ chối `415`, người dùng lưu lại bằng Excel; quyết định system test WF05 2026-11-25) và `YOUTUBE_LINK` (một video thuộc host YouTube cho phép: watch, youtu.be, Shorts, embed, live, bản mobile/music/nocookie). Nhận HTTP hoặc thiếu scheme và ngữ cảnh list/index, chuẩn hóa về URL HTTPS của một video. Không nhận playlist-only, host giả, userinfo/port khác chuẩn, ID sai hoặc nhiều tham số v. | FR-004, UC59/61; đồng bộ staging 2026-11-02 |
| BR-U13-11 | Mỗi tài liệu có tiêu đề (≤ 200), mô tả (≤ 2.000) và thứ tự trong danh sách. Có module cơ bản của môn; Lesson nâng cao và tiến độ học chưa thuộc scope implementation này. | FR-004, FR-005 |
| BR-U13-12 | Sửa tiêu đề, mô tả, thứ tự không tạo phiên bản. Thay tệp hoặc link tạo phiên bản mới; phiên bản cũ giữ lại để các tham chiếu cũ (provenance của AI) vẫn đọc được; người học luôn thấy phiên bản mới nhất. Trang học liệu hiện nhãn "Phiên bản N" của bản hiện hành cho mọi người xem; không hiện bảng lịch sử và không tải bản cũ trên giao diện, lịch sử chỉ giữ trong dữ liệu (yêu cầu 2026-11-25). | UC 58, US-CNT-001 |
| BR-U13-13 | Xóa là lưu trữ: tài liệu ẩn với người học, không chọn được làm nguồn AI hay gắn vào Quiz mới; tham chiếu cũ vẫn giữ. Teacher/Subject Manager còn quyền hợp lệ vẫn xem/tải được, kể cả đọc học liệu môn kế thừa qua lớp; kiểm phân công hiện hành mỗi lần. Danh sách Đang dùng chỉ hiện ACTIVE không làm mất quyền truy cập trực tiếp của staff. Không cho sửa học liệu ARCHIVED. | FR-004 |
| BR-U13-14 | Thêm tài liệu không tự gọi AI, không tóm tắt, không tạo embedding. Tóm tắt, phụ đề, embedding, RAG là phần mở rộng US-CNT-005. | FR-004, US-CNT-005 |

## 3. Xem tài liệu (UC 62)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-20 | Learning Material: PDF xem ngay trên trang, XLSX xem bảng theo sheet, mọi tệp tải được; link YouTube mở trong trình phát nhúng `youtube-nocookie.com`. Tải và xem tệp qua token 5 phút của U03 sau khi U13 kiểm quyền. | UC 62 |
| BR-U13-21 | ALAP không kiểm soát quyền xem video trên YouTube; video bị xóa hoặc riêng tư thì trình phát của YouTube báo lỗi. U13 không gọi API của YouTube. | UC 62 |
| BR-U13-22 | Bỏ từ 2026-11-01: Learning Material không còn vùng cho unit khác gắn vào; Quiz nằm trong module học liệu. | FR-005 |
| BR-U13-23 | Tài liệu gắn kèm Assignment (U07) và Quiz (U10) hiện trên Assignment Detail (U08) và Quiz Practice Detail (U11) qua `MaterialLinkList` của U13; chỉ hiện tài liệu `ACTIVE` người xem có quyền, mỗi dòng mở Learning Material. | FR-004, FR-005 |

## 4. Nguồn cho AI

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-30 | Sau khi lưu phiên bản tệp, job nền trích chữ từ PDF, DOCX, PPTX, XLSX (không OCR, không chạy macro/thực thi công thức); trung bình dưới 50 ký tự mỗi trang thì ghi `NO_TEXT`; tệp vẫn dùng được. Trích chữ không gọi AI. Chữ giữ tối đa 1.000.000 ký tự. | FR-004 |
| BR-U13-31 | `IReferenceMaterial` (U06 khai báo, U13 cài): `listSelectable` trả tài liệu `ACTIVE` người gọi được xem trong phạm vi (lớp: tài liệu của môn và của lớp; môn: tài liệu của môn) kèm trạng thái chữ; `extractText` kiểm lại quyền từng tài liệu, trả chữ của phiên bản hiện tại kèm id, phiên bản, tiêu đề, cắt theo trần ký tự. Link YouTube chỉ có tiêu đề, mô tả, URL. | FR-006 |
| BR-U13-32 | Tool MCP `read_authorized_material(materialId)`: người có quyền xem đọc chữ và thông tin tài liệu; ngoài quyền thì từ chối. Nội dung tài liệu là dữ liệu không tin cậy, không phải lệnh. Staff còn quyền có thể đọc tham chiếu ARCHIVED qua tool này; khác với chọn nguồn cho tác vụ AI mới, vốn chỉ nhận ACTIVE. | FR-035 |

## 5. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U13-40 | Mọi API kiểm quyền phía server. Mọi trường hợp không có quyền đều trả `404` (không lộ học liệu hay lớp có tồn tại): ngoài phạm vi (không học, không dạy, không quản lý) và trong phạm vi nhưng sai vai trò (ví dụ người học tạo, sửa, xóa). Không ghi audit theo BR-U02-10. Sửa kèm `version`; lệch thì `409`. | SEC-002; quyết định system test WF05 2026-11-25 |
| BR-U13-41 | Không ghi audit cho học liệu và module, kể cả tạo, sửa, thay phiên bản, xóa và bị từ chối (BR-U02-10, yêu cầu 2026-11-25). Lịch sử thay đổi nằm ở phiên bản (BR-U13-12) và các cột người tạo, thời điểm. | FR-014; BR-U02-10 |
| BR-U13-42 | U01, U04, U05 không trả lời thì từ chối (fail closed); trích chữ lỗi không ảnh hưởng việc xem, tải. | BR-U01-93 |

## Module rules — AS-BUILT 2026-10-30

| Rule | Behavior |
|---|---|
| BR-U13-M01 | Manager của môn quản lý module; tên 1–200, orderNo ≥0, version CAS. Mọi material có moduleId cùng subject. |
| BR-U13-M02 | Backfill materials cũ vào Module 1; module basic không cần embedding/AI/Lesson. Từ 2026-11-26 môn mới không tự có module: mở trang học liệu không tạo "Module 1"; Chủ nhiệm môn tạo module đầu tiên, trước đó trang hiện hướng dẫn tạo module (Chủ nhiệm môn) hoặc chờ môn có module (giảng viên). |
| BR-U13-M03 | Quiz tối đa một module qua unique quiz_id; attach chuyển link cũ. Quiz cùng môn và actor có quyền manage Quiz. |
| BR-U13-M04 | Xóa chặn active materials; archived materials chuyển module khác; không xóa module cuối nếu còn archived materials. Quiz links cascade khi xóa module. |
| BR-U13-M05 | Subject view chỉ subject Quiz; teacher class view thấy subject OPEN hoặc RETIRED có publishedAt thực và quiz lớp mình; RETIRED chưa phát hành bị ẩn; Student chỉ quiz listForClass cho phép. canManageModules khác canManageQuiz. |
| BR-U13-M06 | Reorder items không rỗng, tối đa 500, mỗi item id/version; trùng id hoặc stale version bị từ chối. Source không buộc gửi đủ toàn bộ module. |

Negative tests/source: MaterialModuleServiceTest, MaterialModuleService và migration 29/10; chưa chạy lại ở baseline.


## Enabled và mô tả có định dạng — 2026-11-09

`enabled` là boolean độc lập ACTIVE/ARCHIVED, mặc định true cho bản ghi và yêu cầu tạo cũ. Disabled ẩn danh sách sinh viên, chặn xem trực tiếp/cấp token và kiểm lại khi đổi token lấy nội dung; staff vẫn quản lý/bật lại theo quyền. Tài liệu disabled không được chọn/trích làm nguồn AI mới. Không xóa version/lịch sử.

`descriptionDocument` là FormattedText tùy chọn; server lấy plain description từ document, tối đa 2.000 ký tự. Chỉ paragraph/danh sách, căn lề, indent 0–6 và run đậm/nghiêng/gạch chân; tối đa 100 block, 200 run/block, 1.000 run tổng. Không nhận HTML để render. Text cũ vẫn tương thích. MaterialRepositoryAdapter dùng JDBC; hai row JPA cũ đã bỏ, module repository giữ ngoài phạm vi (chuyển JDBC ngày 2026-11-18).

## Học liệu môn, lưu trữ, công tắc khi tạo — 2026-11-26

| Mã | Quy tắc |
|---|---|
| BR-U13-90 | Giao diện của Chủ nhiệm môn gọi học liệu của môn là "Subject material" / "Học liệu môn" (menu môn, tiêu đề trang, nút tạo, hộp thoại). Học liệu môn hiện chung trong học liệu của mọi lớp thuộc môn (BR-U13-02); giảng viên chỉ xem, không sửa, không lưu trữ (`canManage` của học liệu môn chỉ đúng với Chủ nhiệm môn). |
| BR-U13-91 | Nút gỡ học liệu của giảng viên và Chủ nhiệm môn là "Lưu trữ" (biểu tượng archive), không còn "Xóa"; Học liệu đã lưu trữ chỉ còn hiện trong module với người quản lý được nó (nhãn "Đã lưu trữ", không sửa, không lưu trữ lại); giảng viên không thấy học liệu môn đã lưu trữ. Người quản lý khôi phục về `ACTIVE` bằng `POST /materials/{id}/restore` trên danh sách hoặc trang chi tiết; người học không thấy học liệu đã lưu trữ (BR-U13-13). Trang chi tiết không rời trang sau khi lưu trữ. Quiz nháp vẫn xóa được, Quiz đã phát hành lưu trữ. |
| BR-U13-92 | Hộp thoại tạo học liệu không có công tắc bật/tắt; học liệu mới luôn `enabled = true`. Công tắc chỉ còn khi sửa và trên trang chi tiết. |
