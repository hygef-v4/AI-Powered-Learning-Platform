# U09 Question Type Authoring - Frontend Components

```
shared/document/
  DocumentEditor            mode: SKELETON | STUDENT | READONLY
    BlockToolbar            kiểu đoạn giống Word (Văn bản thường, Tiêu đề, Phụ đề, Tiêu đề 1–6), Danh sách, Bảng, Ảnh, Sơ đồ
    HeadingBlock, ParagraphBlock, ListBlock, TableBlock, ImageBlock
    DiagramBlock            hiển thị SVG; bấm mở DrawioPanel; nhãn loại sơ đồ
    DrawioPanel             iframe embed.diagrams.net (proto=json), nửa màn hình
    LockedBadge             đánh dấu block của giảng viên (khung khóa hoàn toàn)
  EssayEditor               DocumentEditor giới hạn Heading/Đoạn/Danh sách
  QuestionRubricPanel       Text Essay: danh sách câu, mỗi câu mở RubricEditor (U06) tạo/sửa rubric; báo câu chưa có rubric
app/teaching/assignments/[id]/   (gắn vào TypeConfigSlot của U08; dùng trong màn Assignment Editor, Template Editor, và DocumentEditor trong Question Bank)
  QuizConfigForm
  CodeLabConfigForm         ngôn ngữ, file khởi đầu, test, lời giải mẫu; dùng CodeEditor, VerifySolutionButton của U13
  DocumentConfigForm        Diagram Essay và bài nhóm (bài nhóm: không có RequiredDiagramsForm)
    SkeletonEditor          DocumentEditor mode SKELETON; Diagram Essay, bài nhóm: khung tự chia phần theo heading nhỏ nhất (không có nút bật/tắt)
    PartRubricPanel         cây heading, phần là heading nhỏ nhất; mỗi phần mở RubricEditor (U06) tạo/sửa rubric của phần; báo phần chưa có rubric
    DocxImportDialog        upload, xem trước, báo cáo nhập
    AiSkeletonDraftDialog   nhờ AI soạn khung: yêu cầu, credit ước tính, xem trước khung + gợi ý rubric từng phần + trích dẫn, xác nhận thay khung
    RequiredDiagramsForm
app/learning/attempts/[id]/
  StudentDocxImportDialog  upload DOCX cho DOCUMENT, xem trước block sẽ thêm và phần bị bỏ, xác nhận vào bản nháp
```

| Component | Hành vi | API |
|---|---|---|
| `QuizConfigForm` | Trộn câu/đáp án, giới hạn phút, hiện điểm, hiện đáp án | `PUT /api/v1/assignments/{id}/type-config` |
| `DocumentEditor` (`STUDENT`) | Mọi block giảng viên khóa (không sửa, xóa, kéo); người học chèn và sửa block của mình ở mọi vị trí | Dùng bởi U11, U14 |
| `DrawioPanel` | Nhận `save` từ iframe: `xml` + `svg`; đóng panel | - |
| `DocxImportDialog` | Hiện "Nhận được N sơ đồ, M ảnh giữ nguyên, K phần bị bỏ" | `POST /api/v1/assignments/{id}/skeleton:import-docx` |
| `StudentDocxImportDialog` | Chỉ hiện với lượt DOCUMENT đang làm; xem trước rồi xác nhận thêm block bằng luồng lưu nháp U11 và báo xung đột bản nháp | `POST /api/v1/attempts/{id}/docx:preview`, `PUT /api/v1/attempts/{id}/content` |
| `SkeletonEditor` | Lưu khung; hệ thống tính lại phần theo heading nhỏ nhất (bài nhóm: U14 dựng mỗi phần thành một mục, UC 27) | `PUT /api/v1/assignments/{id}/skeleton` |
| `QuestionRubricPanel` | Text Essay: tạo/sửa rubric từng câu; điểm câu tự bằng tổng điểm rubric; báo câu chưa có rubric; tổng điểm bài = tổng các câu | `PUT /api/v1/assignments/{id}/questions/{questionId}/rubric` |
| `PartRubricPanel` | Tạo/sửa rubric từng phần (có thể điền sẵn từ gợi ý AI); tổng điểm bài = tổng các phần | `PUT /api/v1/assignments/{id}/parts/{partId}/rubric` |
| `AiSkeletonDraftDialog` | Ẩn khi chưa có U13; phân biệt "Không đủ credit AI" và "Hệ thống đang bận"; xác nhận thì cảnh báo nếu khung đã có nội dung | `POST /api/v1/assignments/{id}/skeleton:ai-draft`, `GET /api/v1/ai-suggestions/{id}` (U13), `POST /api/v1/assignments/{id}/skeleton:apply-ai-draft` |
| Nút "Tải DOCX" | Ở trang bài làm (U11) và trang chấm (U15) | `GET` của U11/U15 → `DocxExportPort` |
