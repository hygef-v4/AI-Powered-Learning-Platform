# U10 Template & Copy - Frontend Components

```
app/teaching/subjects/[id]/templates/      TemplateListPage (màn Template List của CN môn)
  CreateTemplateButton (chọn dạng và chế độ trước), DeleteTemplateButton
app/teaching/subjects/[id]/templates/[templateId]/   TemplateEditorPage (màn Template Editor)
  dùng lại các phần của AssignmentEditorPage (U08: câu, xem trước, duyệt) và TypeConfigSlot (U09: cấu hình, khung, rubric từng câu/phần, AI soạn khung) cho mọi dạng bài, kể cả bài nhóm; không có lịch
  AiDraftDialog (U13, ẩn tới khi có U13), ReleaseTemplateButton, NewTemplateVersionButton
app/teaching/classes/[id]/assignments/
  CopyFromTemplateDialog                    bước 2 của CreateAssignmentDialog (U08): chọn template RELEASED của môn cùng dạng và chế độ
  CopyFromClassDialog                       bước 2 của CreateAssignmentDialog (U08): chọn lớp nguồn mình dạy rồi chọn bài cùng dạng và chế độ
app/teaching/assignments/[id]/
  VersionHistoryPanel                       danh sách version + lineage
  AssignmentDiffView                        chọn 2 version/nguồn, hiện khác biệt
```

| Component | Hành vi | API |
|---|---|---|
| `TemplateListPage` | Danh sách template của môn (lọc dạng, chế độ, trạng thái); tạo template rồi mở Template Editor; xoá template của môn (hộp xác nhận nói rõ bản đã copy không bị ảnh hưởng) | `GET /api/v1/subjects/{id}/templates`, `POST /api/v1/templates`, `DELETE /api/v1/templates/{id}` |
| `TemplateEditorPage` | Soạn, nhờ AI đề xuất câu (hoặc khung với Diagram Essay, bài nhóm), duyệt, phát hành version template; không có lịch mở/đóng | `GET`, `PATCH /api/v1/templates/{id}`, `POST /api/v1/templates/{id}/release`, `POST /api/v1/templates/{id}/versions` |
| `CopyFromTemplateDialog` | | `POST /api/v1/classes/{id}/assignments:copy-from-template` |
| `CopyFromClassDialog` | Chỉ hiện lớp mình dạy và bài cùng dạng, chế độ | `GET /api/v1/classes/{id}/copy-sources?type=&mode=`, `POST /api/v1/classes/{id}/assignments:copy-from-class` |
| `AssignmentDiffView` | Hai cột, đánh dấu thêm/bớt/đổi | `GET /api/v1/assignments/diff?from=&to=` |
