# U13 Reference Materials — business-logic-model

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Truy vết

| Luồng | UC | Màn |
|---|---|---|
| F1 Tài liệu của môn | UC 58 | Material List, Material Detail |
| F2 Thêm, sửa, lưu trữ tài liệu của môn | UC 59 | Material List, Material Detail |
| F3 Tài liệu của lớp | UC 60 | Teacher Class Detail, tab Materials |
| F4 Thêm, sửa, lưu trữ tài liệu của lớp | UC 61 | Tab Materials, Material Detail |
| F5 Xem tài liệu | UC 62 | Learning Material; phần Học liệu trên Student Class Detail |
| F6 Trích chữ | - | - |
| F7 Nguồn cho AI và MCP | UC của U07, U10; UC 32 | - |

## 2. Quản lý tài liệu

### F1 - Tài liệu của môn (UC 58)
1. Chủ nhiệm môn bấm mục Học liệu trên menu bên (U13 đăng ký `NavEntry`), chọn môn mình quản lý; frontend gọi `GET /api/v1/subjects/{subjectId}/materials`.
2. Kiểm R2 (BR-U13-01); trả tài liệu theo thứ tự, loại, phiên bản hiện tại, trạng thái chữ; lọc bỏ hoặc hiện tài liệu đã lưu trữ.
3. Bấm tài liệu mở Material Detail: `GET /api/v1/materials/{id}` gồm thông tin và lịch sử phiên bản.

### F2 - Thêm, sửa, lưu trữ tài liệu của môn (UC 59)
1. Thêm tệp: chọn tệp, tải qua U03 (`IArtifact.store`, purpose `MATERIAL`) nhận `FileRef`; nhập tiêu đề, mô tả; `POST /api/v1/subjects/{subjectId}/materials` loại `FILE`.
2. Thêm link: nhập URL YouTube, tiêu đề, mô tả; `POST` loại `YOUTUBE_LINK`; kiểm dạng URL một video (BR-U13-10).
3. Kiểm R2, môn `ACTIVE`; `attach` tệp; tạo tài liệu `ACTIVE` với phiên bản 1; gửi job trích chữ với tệp (F6); audit `MATERIAL_CREATED`.
4. Sửa thông tin hoặc thứ tự: `PATCH /api/v1/materials/{id}` kèm `version`; audit `MATERIAL_UPDATED`.
5. Thay tệp hoặc link: `POST /api/v1/materials/{id}/versions` kèm `version`; tạo phiên bản mới, giữ phiên bản cũ (BR-U13-12); gửi job trích chữ; audit `MATERIAL_VERSION_CREATED`.
6. Xóa: xác nhận, `POST /api/v1/materials/{id}/archive`; tài liệu `ARCHIVED` (BR-U13-13); audit `MATERIAL_ARCHIVED`.

### F3 - Tài liệu của lớp (UC 60)
1. Giảng viên mở tab Materials (U13 gắn qua `TeacherClassTab`); frontend gọi `GET /api/v1/classes/{classId}/materials`.
2. Kiểm giảng viên chính của lớp; trả tài liệu của lớp (quản lý được) và tài liệu của môn chứa lớp (nhãn "Của môn", chỉ đọc).
3. Bấm tài liệu mở Material Detail (tài liệu của môn chỉ xem).

### F4 - Thêm, sửa, lưu trữ tài liệu của lớp (UC 61)
1. Như F2 với `POST /api/v1/classes/{classId}/materials`; kiểm giảng viên chính của lớp và lớp chưa lưu trữ.
2. Sửa, thay phiên bản, lưu trữ dùng chung API của F2 bước 4–6; quyền kiểm theo phạm vi của tài liệu.

## 3. Xem tài liệu

### F5 - Xem tài liệu (UC 62)
1. Student Class Detail có phần Học liệu do U13 gắn qua `StudentClassSection`: `GET /api/v1/me/classes/{classId}/materials` trả tài liệu `ACTIVE` của môn và của lớp.
2. Bấm tài liệu mở Learning Material: `GET /api/v1/materials/{id}/view` kèm `classId`; kiểm quyền xem (BR-U13-02).
3. Tệp: frontend gọi `POST /api/v1/materials/{id}/download-token`; U13 kiểm quyền rồi gọi `IArtifact.issueDownloadToken`; PDF hiện ngay trên trang, tệp khác tải về.
4. Link YouTube: trang nhúng `youtube-nocookie.com` theo id video đã lưu (BR-U13-20, 21).
5. Vùng `LearningMaterialSection` hiện phần do unit khác gắn (U11: Quiz gắn tài liệu này).
6. Giảng viên, Chủ nhiệm môn cũng mở được Learning Material của tài liệu mình có quyền xem.
7. Assignment Detail (U08) và Quiz Practice Detail (U11) nhúng `MaterialLinkList` qua slot `AssignmentMaterialSection`, `QuizMaterialSection`: component gọi `GET /api/v1/me/classes/{classId}/materials?ids=`; U13 chỉ trả tài liệu `ACTIVE` người học được xem (BR-U13-23).

## 4. Nguồn cho AI

### F6 - Trích chữ
1. Sau khi lưu phiên bản tệp, U13 gửi job `MATERIAL_TEXT_EXTRACT` qua `IJob` (queue `jobs.triggered`); phiên bản ở `PENDING`.
2. Worker đọc tệp qua `IArtifact.open`, trích chữ PDF, DOCX, PPTX bằng thư viện, không OCR.
3. Ghi chữ (cắt ở 1.000.000 ký tự) và `EXTRACTED`; ít chữ thì `NO_TEXT`; lỗi sau các lần thử của U03 thì `FAILED`. Phiên bản bị thay trước khi job chạy thì bỏ qua.

### F7 - Nguồn cho AI và MCP
1. U07, U10 gọi `IReferenceMaterial.listSelectable(actor, scopeRef)` để hiện danh sách tài liệu người soạn chọn được (BR-U13-31).
2. Khi gửi task AI, U07, U10 gọi `extractText(actor, materialIds, maxChars)`: U13 kiểm lại quyền từng tài liệu, trả chữ phiên bản hiện tại kèm id, phiên bản, tiêu đề; tài liệu chưa có chữ được đánh dấu để người soạn biết.
3. Agent gọi `read_authorized_material(materialId)` qua U06: U13 kiểm quyền xem của actor, trả chữ (cắt theo trần của tool) và thông tin tài liệu.
