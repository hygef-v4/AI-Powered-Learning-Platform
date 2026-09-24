# U09 Question Type Authoring - Frontend Components

```
shared/document/
  DocumentEditor            mode: SKELETON | LEARNER | READONLY
    BlockToolbar            Heading, Đoạn, Danh sách, Bảng, Ảnh, Sơ đồ
    HeadingBlock, ParagraphBlock, ListBlock, TableBlock, ImageBlock
    DiagramBlock            hiển thị SVG; bấm mở DrawioPanel; nhãn loại sơ đồ
    DrawioPanel             iframe embed.diagrams.net (proto=json), nửa màn hình
    LockedBadge             đánh dấu block của giảng viên không sửa được
  EssayEditor               DocumentEditor giới hạn Heading/Đoạn/Danh sách
app/teaching/assignments/[id]/   (gắn vào TypeConfigSlot của U08)
  QuizConfigForm
  DocumentConfigForm
    SkeletonEditor          DocumentEditor mode SKELETON
    DocxImportDialog        upload, xem trước, báo cáo nhập
    RequiredDiagramsForm
```

| Component | Hành vi | API |
|---|---|---|
| `QuizConfigForm` | Trộn câu/đáp án, giới hạn phút, hiện điểm, hiện đáp án | `PUT /api/v1/assignments/{id}/type-config` |
| `DocumentEditor` (`LEARNER`) | Block giảng viên: chữ/ảnh khóa, bảng/sơ đồ sửa được, không xóa/kéo; chèn block mới ở mọi vị trí | Dùng bởi U11 |
| `DrawioPanel` | Nhận `save` từ iframe: `xml` + `svg`; đóng panel | - |
| `DocxImportDialog` | Hiện "Nhận được N sơ đồ, M ảnh giữ nguyên, K phần bị bỏ" | `POST /api/v1/assignments/{id}/skeleton:import-docx` |
| `SkeletonEditor` | Lưu khung | `PUT /api/v1/assignments/{id}/skeleton` |
| Nút "Tải DOCX" | Ở trang bài làm (U11) và trang chấm (U15) | `GET` của U11/U15 → `DocxExportPort` |
