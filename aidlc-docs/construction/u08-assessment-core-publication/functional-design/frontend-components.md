# U08 Assessment Core & Publication - Frontend Components

```
app/teaching/classes/[id]/assignments/        AssignmentListPage
app/teaching/assignments/[id]/                AssignmentEditorPage
  AssignmentHeader        tiêu đề, loại, trạng thái, tổng điểm
  InstructionsEditor
  ComponentList           thứ tự, điểm, xóa
    AddFromBankDialog     tìm trong ngân hàng U06 (chỉ ACTIVE, đúng loại)
    InlineQuestionEditor  dùng lại QuestionEditor của U06
    AiDraftDialog         yêu cầu AI, chọn câu giữ lại (hiện credit sẽ dùng)
  TypeConfigSlot          chỗ U09 gắn form cấu hình riêng loại bài
  PreviewDialog           dùng QuestionView của U06
  ReviewButton            hiện lỗi kiểm tra nếu chưa đạt
  PublishDialog           lớp (một), mở/đóng, nộp trễ + hạn cuối, số lượt
  PublicationList         trạng thái, sửa lịch, Ngưng giao (lý do)
  CloneButton, ArchiveButton
```

| Component | Hành vi | API |
|---|---|---|
| `AssignmentListPage` | Lọc theo trạng thái, loại | `GET /api/v1/classes/{id}/assignments` |
| `ComponentList` | Chỉ sửa khi `DRAFT`; bài `LOCKED` hiện nhãn "Đã khóa - nhân bản để thay đổi" | `POST`, `PATCH`, `DELETE /api/v1/assignments/{id}/components` |
| `AiDraftDialog` | Gửi yêu cầu, poll job, hiện đề xuất và nguồn trích dẫn | `POST /api/v1/assignments/{id}/ai-drafts`, `GET /api/v1/jobs/{id}` |
| `ReviewButton` | | `POST /api/v1/assignments/{id}/review` |
| `PublishDialog` | Kiểm lịch phía client; hiện cảnh báo "phát hành xong sẽ khóa nội dung" | `POST /api/v1/assignments/{id}/publications` |
| `PublicationList` | | `PATCH /api/v1/publications/{id}`, `POST .../{id}/retire` |
| `CloneButton` | | `POST /api/v1/assignments/{id}/clone` |
