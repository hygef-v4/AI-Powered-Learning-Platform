# U10 Template & Copy - Frontend Components

```
app/teaching/subjects/[id]/templates/      TemplateListPage (CN môn)
  CreateTemplateButton, AiDraftButton, TemplateReleaseButton, WithdrawButton, DeleteTemplateButton
app/teaching/classes/[id]/assignments/
  CopyFromTemplateDialog                    chọn template RELEASED của môn
  CopyToClassDialog                         chọn lớp đích mình dạy
app/teaching/assignments/[id]/
  VersionHistoryPanel                       danh sách version + lineage
  AssignmentDiffView                        chọn 2 version/nguồn, hiện khác biệt
```

| Component | Hành vi | API |
|---|---|---|
| `TemplateListPage` | Tạo thủ công hoặc nhờ AI (mở trình soạn U08 với panel bản nháp AI của U13), sửa, phát hành, rút, xoá template mình tạo (hộp xác nhận nói rõ bản đã copy không bị ảnh hưởng) | `POST /api/v1/templates`, `POST /api/v1/templates/{id}/release`, `.../withdraw`, `DELETE /api/v1/templates/{id}` |
| `CopyFromTemplateDialog` | | `POST /api/v1/classes/{id}/assignments:copy-from-template` |
| `CopyToClassDialog` | Chỉ hiện lớp mình dạy | `POST /api/v1/assignments/{id}:copy-to-class` |
| `AssignmentDiffView` | Hai cột, đánh dấu thêm/bớt/đổi | `GET /api/v1/assignments/diff?from=&to=` |
