# U09 Question Type Authoring - Frontend Components

**Bản tài liệu 2026-10-09**: nội dung theo dạng bài của UC 35 (cài đặt quiz), 42, 43, 44, 45 và popup Rubric Detail của UC 46 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Gắn vào màn của U08

| Màn | Chỗ gắn (U08) | Component U09 | UC |
|---|---|---|---|
| Assignment Form (Text Essay) | `TypeConfigSlot` | `QuestionRubricList` | 42 |
| Assignment Form (Code Lab) | `TypeConfigSlot` | `CodeLabCheckPanel` | 43 |
| Assignment Form (Diagram Essay) | `TypeConfigSlot` | `DocumentConfigForm` (có `RequiredDiagramsForm`) | 44 |
| Assignment Form (bài nhóm) | `TypeConfigSlot` | `DocumentConfigForm` (không có `RequiredDiagramsForm`) | 45 |
| Rubric Detail (popup) | Mở từ `QuestionRubricList`, `PartRubricList` | `RubricDetailDialog` (gắn `RubricEditor` của U06) | 46 |
| Quiz Detail | `QuizSettingsSlot` | `QuizSettingsForm` | 35 |

Assignment Form của bài của môn (Subject Manager) dùng cùng các form; giảng viên lớp mở bài của môn thì form chỉ đọc.

## 2. Cây component

```
shared/document/
  DocumentEditor            mode: SKELETON | STUDENT | READONLY
    BlockToolbar            kiểu đoạn giống Word (Văn bản thường, Tiêu đề, Phụ đề, Tiêu đề 1–6), Danh sách, Bảng, Ảnh, Sơ đồ
    HeadingBlock, ParagraphBlock, ListBlock, TableBlock, ImageBlock
    DiagramBlock            hiển thị SVG; bấm mở DrawioPanel; nhãn loại sơ đồ
    DrawioPanel             iframe embed.diagrams.net (proto=json), nửa màn hình
    LockedBadge             đánh dấu block của khung (khóa hoàn toàn)
  EssayEditor               DocumentEditor giới hạn Heading/Đoạn/Danh sách
shared/authoring/
  QuestionRubricList        câu Text Essay của bài (U08 quản lý câu): mỗi câu một nút Rubric, cảnh báo khi rubric trống
  CodeLabCheckPanel         câu Code Lab của bài: trạng thái kiểm lời giải mẫu (`statusOf`), VerifySolutionButton của U13 (gửi kèm `assignmentId`)
  DocumentConfigForm        Diagram Essay và bài nhóm
    SkeletonEditor          DocumentEditor mode SKELETON; khung tự chia phần theo heading nhỏ nhất
    SkeletonFromBankButton  QuestionPicker của U06 (câu DOCUMENT), sao khung vào bài
    PartRubricList          cây heading, mỗi phần một nút Rubric (cảnh báo khi rubric trống)
    DocxImportDialog        upload, xem trước, báo cáo nhập
    AiSkeletonDraftDialog   nhờ AI soạn khung: yêu cầu, credit ước tính, xem trước khung + gợi ý rubric + trích dẫn, xác nhận thay khung
    RequiredDiagramsForm    chỉ Diagram Essay
  RubricDetailDialog        popup Rubric Detail: RubricEditor của U06; chỉ đọc khi bài đã phát hành
  QuizSettingsForm          trộn câu/đáp án, thời gian, hiện điểm, hiện đáp án
StudentDocxImportDialog     dùng trong Diagram Essay Workspace (U11): upload DOCX, xem trước block sẽ thêm và phần bị bỏ, xác nhận vào bản nháp
```

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `QuizSettingsForm` | Trộn câu/đáp án, giới hạn phút, hiện điểm, hiện đáp án (`NEVER`, `AFTER_SUBMIT`) | `GET`, `PUT /api/v1/assignments/{id}/type-config` |
| `QuestionRubricList` | Câu thêm qua U08 thì có ngay rubric trống; bỏ câu thì rubric bị xóa theo; tổng điểm bài = tổng điểm rubric các câu (UC 42) | Câu: API U08; rubric: API rubric bên dưới |
| `CodeLabCheckPanel` | Hiện trạng thái kiểm lời giải từng câu; câu riêng sửa nội dung thì báo cần kiểm lại (UC 43) | Kiểm lời giải qua API U13 |
| `SkeletonEditor` | Lưu khung; hệ thống tính lại phần, tự tạo rubric cho phần mới và xóa rubric của phần không còn (bài nhóm: U14 dựng mỗi phần thành một mục) (UC 44, 45) | `GET`, `PUT /api/v1/assignments/{id}/skeleton` |
| `DocxImportDialog` | Hiện "Nhận được N sơ đồ, M ảnh giữ nguyên, K phần bị bỏ" | `POST /api/v1/assignments/{id}/skeleton:import-docx` |
| `AiSkeletonDraftDialog` | Ẩn khi chưa có U13; phân biệt "Không đủ credit AI" và "Hệ thống đang bận"; xác nhận thì cảnh báo nếu khung đã có nội dung | `POST .../skeleton:ai-draft`, `GET /api/v1/ai-suggestions/{id}` (U13), `POST .../skeleton:apply-ai-draft` |
| `RubricDetailDialog` | Rubric đã tự tạo; thêm/xóa/sắp xếp tiêu chí và mục; Lưu ghi đè; không có nút xóa rubric (UC 46) | `GET`, `PUT /api/v1/assignments/{id}/questions/{questionId}/rubric`, `/parts/{partId}/rubric` |
| `DocumentEditor` (`STUDENT`) | Mọi block khung khóa (không sửa, xóa, kéo); người học chèn và sửa block của mình ở mọi vị trí | Dùng bởi U11, U14 |
| `DrawioPanel` | Nhận `save` từ iframe: `xml` + `svg`; đóng panel | - |
| `StudentDocxImportDialog` | Chỉ hiện với lượt DOCUMENT đang làm; xem trước rồi xác nhận thêm block bằng luồng lưu nháp U11 và báo xung đột bản nháp | `POST /api/v1/attempts/{id}/docx:preview`, `PUT /api/v1/attempts/{id}/content` (U11) |
| Nút "Tải DOCX" | Ở trang bài làm (U11) và trang chấm (U15) | `GET` của U11/U15 → `DocxExportPort` |
