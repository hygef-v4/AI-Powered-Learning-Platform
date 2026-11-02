# U07 Assignment Authoring — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Lấy phần Assignment của U08 cũ (vòng đời, phát hành), U09 cũ (tài liệu, khung, DOCX) và rubric của U06 cũ. Quiz và ngân hàng thuộc U10. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-22 theo code hiện tại (các yêu cầu của người dùng 2026-10-21, 2026-10-22; chi tiết từng vòng trong code-summary).

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Danh sách bài của lớp | UC 34 | Teacher Class Detail, tab Evals |
| F2 Danh sách bài của môn | UC 34 | Assignment List |
| F3 Tạo bài | UC 35 | Tab Evals, Assignment List |
| F4 Soạn đề và khung | UC 35 | Assignment Form |
| F5 Sửa rubric | UC 36 | Thẻ rubric bên lề Assignment Form |
| F6 AI soạn bản nháp | UC 35 | Assignment Form, popup chọn nguồn AI |
| F7 Xuất, nhập DOCX | UC 35 | Assignment Form |
| F8 Xem trước (tùy chọn) | UC 35 | Assignment Form |
| F9 Xếp lịch, phát hành | UC 35 | Assignment Form, popup Xếp lịch |
| F10 Mở, đóng theo lịch | - | - |
| F11 Sửa lịch, ngưng giao | UC 34, 35 | Assignment Form |
| F12 Nhân bản, version mới | UC 35 | Assignment Form |
| F13 Copy bài giữa lớp | UC 35 | Tab Evals |
| F14 Xóa bài nháp | UC 35 | Assignment Form |
| F15 Tool MCP | UC 32 (U06) | - |
| F16 Interface cho unit khác | UC của U08, U09, U12, U14 | - |

## 2. Danh sách bài

### F1 - Danh sách bài của lớp (UC 34)
1. Giảng viên mở tab Evals của Teacher Class Detail; phần Bài tập do U07 gắn qua `TeacherClassTab`; frontend gọi `GET /api/v1/classes/{classId}/assignments`.
2. Kiểm người gọi là giảng viên chính của lớp; không phải thì "không tìm thấy" và audit.
3. Trả bài của lớp và bài của môn chứa lớp (nhãn "Của môn", chỉ đọc). Bảng có cột Bài tập (tên, version, nguồn copy hoặc version mới; bài nhân bản không ghi nhãn), Hình thức, Chế độ, Phạm vi, Trạng thái, Hạn nộp. Lọc theo trạng thái (Nháp gồm cả bài `REVIEWED` cũ); mặc định ẩn `CLOSED`, `RETIRED`.
4. Bấm bài mở Assignment Form; mỗi dòng có lối vào bài nộp do U09 gắn qua `AssignmentRowAction`; nút "Tạo bài tập" (F3).

### F2 - Danh sách bài của môn (UC 34)
1. Chủ nhiệm môn bấm mục Bài tập trên menu bên (U07 đăng ký `NavEntry`), chọn môn mình quản lý; frontend gọi `GET /api/v1/subjects/{subjectId}/assignments`.
2. Kiểm R2; trả bài của môn kèm trạng thái, version, lịch; lọc; nút "Tạo bài".

## 3. Soạn bài

### F3 - Tạo bài (UC 35)
1. Bấm "Tạo bài tập" ở tab Evals (bài của lớp) hoặc Assignment List (bài của môn); popup gồm một ô tệp DOCX tùy chọn (chọn xong chỉ hiện tên tệp), thẻ chọn Hình thức (Cá nhân, Nhóm; nhóm chỉ ở lớp và khi U12 có), thẻ chọn Chế độ (Graded, Practice; nhóm chỉ Graded). Bài của lớp có nút nhỏ "Copy từ lớp khác" (F13). Không có dòng hướng dẫn, không chọn sơ đồ.
2. Kiểm quyền (R3/R4 với lớp, R2 với môn) và lớp hoặc môn chưa lưu trữ (BR-U07-02, 03, 04).
3. Tạo bài `DRAFT`, `versionNo` 1, `diagramEnabled = true`, khung trống có một phần (cả tài liệu) kèm rubric trống; audit `ASSIGNMENT_CREATED`.
4. Có tệp DOCX: gọi `POST .../skeleton:import-docx` rồi `PUT .../skeleton` (`fromDocx`) với khung đọc được (BR-U07-61); lỗi thì "Đã tạo bài, chưa nhập tệp", người soạn Thử lại hoặc Mở bài.
5. Mở Assignment Form.

### F4 - Soạn đề và khung (UC 35)
1. Assignment Form gọi `GET /api/v1/assignments/{id}`: thông tin, cài đặt, khung, danh sách phần kèm tổng điểm và trạng thái rubric. Trang là một trang giấy kiểu Google Docs: tên bài, hướng dẫn chung, rồi khung; thanh công cụ dính trên đầu; thẻ rubric ở lề phải.
2. Đầu trang: nút Khác (Xuất DOCX, Xuất rubric, Nhân bản, Tạo version mới, Ngưng giao, Xóa bản nháp), Xem trước hoặc Quay lại, Xếp lịch (bài nháp) hoặc Đổi lịch (bài đã lên lịch, đang mở); màn hẹp thì hàng nút xuống dưới tên bài. Thanh công cụ có Soạn bằng AI (bài nháp) và Cài đặt bài.
3. Popup Cài đặt (`PATCH /api/v1/assignments/{id}` kèm `version`): thẻ chọn Hình thức (chỉ xem), Chế độ, nút gạt Ghi nhận hoạt động kèm lời báo (BR-U07-16).
4. Sửa khung trên trang: thanh công cụ có hoàn tác, làm lại, kiểu đoạn, đậm, nghiêng, gạch chân, gạch ngang, màu chữ, màu nền, căn lề, danh sách chấm, số, thụt lề, chèn bảng (lưới chọn cỡ), chèn ảnh (tải qua U03 `DOCUMENT_IMAGE`), chèn sơ đồ (một nút, mở Draw.io ngay). Các phần hiện theo cây tiêu đề: cùng tầng cùng màu, tầng sâu đậm hơn; phần lá có khung màu của tầng, ruột trắng là chỗ viết. Không có chữ gợi ý ở dòng trống. Sơ đồ, ảnh, bảng có nút ✕ ở góc để xóa; bảng có nút "+ −" nổi trên đầu cột và bên trái hàng.
5. Tự lưu 1,5 giây sau lần sửa cuối (Ctrl+S lưu ngay): `PUT /api/v1/assignments/{id}/skeleton` kèm `version`; kiểm cấu trúc, id block duy nhất, trần kích thước, XML sơ đồ an toàn, định dạng run (BR-U07-10, 12); server suy loại sơ đồ từ hình vẽ.
6. Tính lại phần (BR-U07-11): phần mới tạo rubric trống, phần không còn thì xóa rubric, phần còn heading giữ rubric đã điền (BR-U07-20).
7. Bài `REVIEWED` cũ thì về `DRAFT`; tăng `version`; `409` thì báo "Bài đã thay đổi ở nơi khác, bản đang sửa chưa lưu" kèm nút Tải lại, giữ bản đang sửa.

### F5 - Sửa rubric (UC 36)
1. Mỗi phần có thẻ rubric ở lề phải trang soạn (màn hẹp thì nằm dưới tiêu đề phần); frontend gọi `GET /api/v1/assignments/{id}/parts/{partId}/rubric` cho các phần.
2. Sửa thẳng trên thẻ: mỗi dòng một mục chấm atomic (nội dung, điểm); dấu "+" ở dòng cuối thêm mục, Enter thêm mục ngay dưới, ✕ xóa mục; tổng điểm tự tính.
3. Tự lưu cùng trang: `PUT .../parts/{partId}/rubric` kèm `version`; bỏ dòng chưa gõ gì; kiểm quyền, bài nháp, rubric chưa khóa, không vượt giới hạn, không có điểm sai (BR-U07-21); khi nháp vẫn cho lưu rubric chưa đủ.
4. Ghi đè rubric; bài `REVIEWED` cũ về `DRAFT`; audit `RUBRIC_UPDATED`; trang cập nhật tổng điểm bài.
5. Bài đã phát hành: thẻ chỉ xem, nhãn Đã khóa (BR-U07-23).

### F6 - AI soạn bản nháp (UC 35)
1. Trên Assignment Form của bài `DRAFT`, bấm "Soạn bằng AI"; nhập yêu cầu (chủ đề, mức độ, số phần, gợi ý về rubric); chọn tài liệu tham khảo từ danh sách của U13 qua `IReferenceMaterial` (chưa có U13 thì bỏ qua bước này); chọn nguồn AI trong `AiSourcePicker` của U06.
2. Frontend gọi `POST /api/v1/assignments/{id}/ai-drafts`; kiểm quyền và bài `DRAFT`.
3. Lấy chữ của tài liệu đã chọn qua `IReferenceMaterial.extractText` (U13 kiểm quyền, cắt theo trần); dựng prompt gồm yêu cầu, cài đặt bài, chữ tài liệu và JSON schema của kết quả (BR-U07-52).
4. Gọi `IAiTask.submit` (loại `ASSIGNMENT_DRAFT`, đích là bài, `inputVersion` là `version` hiện tại, `requestRef`); tạo đề xuất `PENDING` kèm provenance; trả `taskId` cho `AiTaskStatus`.
5. U06 gọi `onReady`: bài còn `DRAFT` thì lưu đề xuất `READY` và trả `APPLIED`; bài đã xóa hoặc đã phát hành thì trả `REJECTED`. `onFailed` chuyển đề xuất sang `FAILED`.
6. Người soạn xem đề xuất. Áp dụng: `POST .../ai-drafts/{draftId}:apply` kèm `version`; cảnh báo nếu khung đã có nội dung; thay khung (và hướng dẫn nếu chọn), tính lại phần như F4 bước 5, điền gợi ý rubric; đề xuất `APPLIED`; audit `ASSIGNMENT_AI_DRAFT_APPLIED`.
7. Bỏ: đề xuất `DISCARDED`, bài không đổi.

### F7 - Xuất, nhập DOCX (UC 35)
Xuất:
1. Menu Khác có Xuất DOCX (đề) và Xuất rubric; trang lưu bản đang sửa rồi gọi `GET /api/v1/assignments/{id}/docx` hoặc `GET /api/v1/assignments/{id}/rubric/docx`.
2. Kiểm quyền đọc bài (sinh viên không đọc được).
3. Tệp đề: heading theo style Word, đoạn (định dạng run, căn lề), danh sách, bảng, ảnh (đọc qua U03); mỗi sơ đồ: PNG đại diện, alt text "mô tả [drawio:assignmentId/blockId]", hyperlink tới trang xem sơ đồ, khối `mxfile` trong PNG (BR-U07-60). Tệp rubric: tên bài, mỗi phần một bảng Mục chấm, Điểm.
4. Trả file, không lưu.

Nhập (khi tạo bài, F3 bước 4):
1. Frontend gọi `POST /api/v1/assignments/{id}/skeleton:import-docx` với tệp ≤ 20 MB.
2. Đọc file trong bộ nhớ, chặn file nén bất thường; chuyển style Word thành heading, đoạn (giữ định dạng run, màu, căn lề), danh sách, bảng; ảnh lưu qua U03 (`store`, chưa `attach`).
3. Mỗi ảnh tìm sơ đồ theo thứ tự BR-U07-61; XML qua parser an toàn; sơ đồ theo mã chỉ lấy khi người nhập có quyền đọc bài nguồn.
4. Trả khung đọc được; frontend lưu luôn làm khung (`PUT .../skeleton` kèm `fromDocx`), `attach` các ảnh; audit `ASSIGNMENT_DOCX_IMPORTED`.

### F8 - Xem trước (UC 35)
1. Bấm Xem trước (tùy chọn): trang lưu bản đang sửa rồi hiện đề như người học thấy khi làm bài (không có rubric, không sửa được); nút Quay lại về trang soạn, nút Xếp lịch vẫn dùng được.
2. Không còn lệnh duyệt trên giao diện (BR-U07-32).

### F9 - Xếp lịch, phát hành (UC 35)
1. Bài nháp, bấm Xếp lịch: trang lưu bản đang sửa, kiểm sơ bộ tên bài và rubric các phần; thiếu thì tô đỏ chỗ thiếu, báo "Bài còn chỗ chưa đủ", không mở lịch.
2. Popup Xếp lịch: Hẹn giờ mở (tích mới chọn ngày, giờ), Hạn nộp, Cho nộp muộn (tích mới chọn hạn nộp muộn), Số lượt nộp; nút Phát hành ngay hoặc Xếp lịch. Frontend gọi `POST /api/v1/assignments/{id}/publish` kèm `version`, `openNow`.
3. Kiểm quyền và bài còn nháp (`DRAFT` hoặc `REVIEWED` cũ); kiểm luật duyệt (BR-U07-32), thiếu thì `ASSIGNMENT_NOT_READY` (trang đóng popup, tô đỏ chỗ thiếu); bài của lớp cần lớp `OPEN` (`IClassAccess`), bài của môn cần môn `ACTIVE` (`ISubjectQuery`); kiểm lịch (BR-U07-33).
4. Bài `GROUP`: gọi `IGroupAssignmentSupport.checkReadiness(classId)`; lỗi thì từ chối; có sinh viên chưa có nhóm thì cần `confirmUngroupedStudents`.
5. Một transaction: ghi lịch, kiểm lại rồi khóa mọi rubric, chuyển `SCHEDULED` (mở ngay thì mở luôn); audit `ASSIGNMENT_PUBLISHED`. Trang báo "Đã phát hành" hoặc "Đã lên lịch".
6. Từ đây khung, rubric, cài đặt không sửa được; muốn đổi thì nhân bản hoặc tạo version mới.

## 4. Sau phát hành

### F10 - Mở, đóng theo lịch
1. `IScheduledScanner` của U07 chạy mỗi phút trong worker.
2. Bài `SCHEDULED` có `opensAt` đã tới chuyển `OPEN` bằng câu lệnh có điều kiện; trong cùng transaction gọi `IAssignmentLifecycleListener.onOpened` (U12 tạo tài liệu nhóm); sau commit phát `assignment.opened` kèm `classId` hoặc `subjectId`.
3. Bài `OPEN` có `lateUntil` (hoặc `closesAt`) đã qua chuyển `CLOSED`.
4. Chạy lại không tạo trùng.

### F11 - Sửa lịch, ngưng giao (UC 34, 35)
Sửa lịch:
1. Bấm Đổi lịch; frontend gọi `PATCH /api/v1/assignments/{id}/schedule` kèm `version`; kiểm quyền (bài của môn chỉ Chủ nhiệm môn).
2. `SCHEDULED`: sửa theo BR-U07-33, bỏ tích Hẹn giờ mở là mở ngay; `OPEN`: không gửi ngày mở, chỉ kéo dài `closesAt`, `lateUntil` (BR-U07-35).
3. Lưu; scanner dùng lịch mới; audit `ASSIGNMENT_SCHEDULE_CHANGED`; trang báo "Đã lưu lịch".

Ngưng giao:
1. Bấm Ngưng giao, nhập lý do; frontend gọi `POST /api/v1/assignments/{id}/retire`.
2. Kiểm quyền và bài `SCHEDULED` hoặc `OPEN`.
3. Một transaction: chuyển `RETIRED`, gọi `onRetired` (U08, U12 tự nộp bài đang làm); audit `ASSIGNMENT_RETIRED` kèm lý do.

### F12 - Nhân bản, version mới (UC 35)
1. Nhân bản: `POST /api/v1/assignments/{id}/clone`; kiểm quyền với bài nguồn; tạo `DRAFT` cùng lớp hoặc môn, giữ nguyên tên bài, theo BR-U07-40, 43; nhân bản rubric; audit `ASSIGNMENT_CLONED`.
2. Version mới: bấm "Sửa" trên bài `CLOSED` hoặc `RETIRED`, frontend gọi `POST /api/v1/assignments/{id}/versions`; kiểm chưa có version đang mở (BR-U07-41); tạo `DRAFT` `versionNo + 1`; audit `ASSIGNMENT_VERSION_CREATED`.
3. Mở Assignment Form của bài mới.

### F13 - Copy bài giữa lớp (UC 35)
1. Trong popup Tạo bài tập ở tab Evals bấm "Copy từ lớp khác"; frontend gọi `GET /api/v1/classes/{classId}/assignment-copy-sources`: các lớp mình dạy (trừ lớp đích) và bài của lớp đó.
2. Chọn bài; frontend gọi `POST /api/v1/classes/{classId}/assignments:copy` kèm `sourceAssignmentId`.
3. Kiểm người gọi dạy cả lớp nguồn và lớp đích; ngoài quyền thì "không tìm thấy" (BR-U07-42).
4. Một transaction: tạo `DRAFT` ở lớp đích theo BR-U07-43, nhân bản rubric; audit `ASSIGNMENT_COPIED`; mở Assignment Form.

### F14 - Xóa bài nháp (UC 35)
1. Bấm Xóa, xác nhận; frontend gọi `DELETE /api/v1/assignments/{id}` kèm `version`.
2. Kiểm quyền và bài còn nháp (`DRAFT`, `REVIEWED` cũ) (BR-U07-31).
3. Xóa bài, rubric, đề xuất AI; audit `ASSIGNMENT_DELETED`.

## 5. MCP và unit khác

### F15 - Tool MCP
1. U06 gọi `IMcpToolProvider.call` của U07 với actor dựng từ credential MCP.
2. `read_assignment_context(assignmentId)`: kiểm quyền qua `IAuthorization`. Giảng viên có quyền với bài: trả tiêu đề, hướng dẫn, khung dạng chữ (sơ đồ ở dạng XML rút gọn), phần, rubric, `version`. Người học: chỉ bài `OPEN`, `CLOSED` của lớp đang học, chỉ thông tin bài (hình thức, chế độ, lịch, sơ đồ cần có), không có đề và rubric.
3. `save_assignment_draft(assignmentId, expectedVersion, draft)`: chỉ giảng viên có quyền, bài `DRAFT`, `expectedVersion` khớp; kiểm `draft` theo schema của đề xuất AI; lưu thành đề xuất `READY` nguồn MCP (provenance: nhãn agent). Người soạn áp dụng như F6 bước 6.
4. Ngoài quyền: trả lỗi cho U06, ghi audit.

### F16 - Interface cho unit khác
1. U08 đọc bài, lịch, `submissionWindow` (`ON_TIME`, `LATE`, `CLOSED`) và nội dung đã khóa (khung, phần, `requiredDiagrams`, rubric) qua `IAssignmentQuery`; kiểm bài làm qua `IDocumentModel`.
2. U09 đọc rubric đã khóa và tính điểm qua `IRubric.score`; dùng `toPlainText`, `compactDiagramXml` khi dựng prompt AI chấm.
3. U12 đọc khung và phần cho tài liệu nhóm; cài `IGroupAssignmentSupport` và `IAssignmentLifecycleListener`.
4. U14 nhận `assignment.opened`; đọc bài sắp hết hạn qua `listClosingBetween` để nhắc hạn.
