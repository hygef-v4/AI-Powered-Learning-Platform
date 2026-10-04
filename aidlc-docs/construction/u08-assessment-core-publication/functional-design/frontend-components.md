# U08 Assessment Core & Publication - Frontend Components

Màn hình theo [screen flow](../../../../docs/screen-flow.md): Assignment List (Teacher) mở từ Class Detail, Assignment Editor mở từ Assignment List.

```
app/teaching/classes/[id]/assignments/        AssignmentListPage   (màn Assignment List của Teacher)
  CreateAssignmentDialog  bước 1 chọn dạng bài và chế độ; bước 2 chọn nguồn: bài trống, copy template (CopyFromTemplateDialog U10), copy bài lớp khác (U10) — chỉ hiện nguồn cùng dạng và chế độ
app/teaching/assignments/[id]/                AssignmentEditorPage (màn Assignment Editor)
  AssignmentHeader        tiêu đề, loại, chế độ, trạng thái, tổng điểm
  InstructionsEditor
  QuestionList            thứ tự, điểm, xóa
    AddFromBankDialog     tìm trong ngân hàng U06 (chỉ ACTIVE, đúng loại); tab "Chọn ngẫu nhiên": số câu + bộ lọc, xem kết quả, bỏ/bốc lại từng câu rồi thêm
    InlineQuestionEditor  dùng lại QuestionEditor của U06
    AiDraftDialog         yêu cầu AI, chọn câu giữ lại (hiện credit sẽ dùng)
  TypeConfigSlot          chỗ U09 gắn form cấu hình riêng loại bài
  PreviewDialog           dùng QuestionView của U06
  ReviewButton            hiện lỗi kiểm tra nếu chưa đạt
  PublishDialog           mở/đóng, hạn nộp trễ, số lượt
  ScheduleSection         lịch hiện tại, sửa lịch, Ngưng giao (lý do)
  CloneButton, NewVersionButton
```

| Component | Hành vi | API |
|---|---|---|
| `AssignmentListPage` | Lọc theo trạng thái, loại; mở Assignment Editor, popup Check Progress (U16), Grading Queue (U15) | `GET /api/v1/classes/{id}/assignments` |
| `QuestionList` | Chỉ sửa khi `DRAFT`; từ `SCHEDULED` hiện nhãn "Đã khóa"; nút "Sửa (tạo version mới)" bật khi bài `CLOSED`/`RETIRED` | `POST`, `PATCH`, `DELETE /api/v1/assignments/{id}/questions` |
| `AiDraftDialog` | Quiz, Text Essay, Code Lab: gửi yêu cầu (U08 chuyển sang `AiDraftPort`), poll trạng thái đề xuất, hiện câu đề xuất và nguồn trích dẫn; Diagram Essay và bài nhóm dùng nút AI soạn khung của U09 | `POST /api/v1/assignments/{id}/ai-drafts`, `GET /api/v1/ai-suggestions/{id}` (U13) |
| `ReviewButton` | | `POST /api/v1/assignments/{id}/review` |
| `PublishDialog` | Kiểm lịch phía client; cảnh báo "phát hành xong sẽ khóa nội dung" | `POST /api/v1/assignments/{id}/publish` |
| `ScheduleSection` | | `PATCH /api/v1/assignments/{id}/schedule`, `POST /api/v1/assignments/{id}/retire` |
| `CloneButton`, `NewVersionButton` | | `POST /api/v1/assignments/{id}/clone`, `POST /api/v1/assignments/{id}/versions` |
