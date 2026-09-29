# U10 Template & Copy - Frontend Components

```
app/teaching/subjects/[id]/templates/      TemplateListPage (CN môn)
  TemplateReleaseButton, WithdrawButton
app/teaching/classes/[id]/assignments/
  CopyFromTemplateDialog                    chọn template RELEASED của môn
  CopyToClassDialog                         chọn lớp đích mình dạy
app/teaching/assignments/[id]/
  VersionHistoryPanel                       danh sách version + lineage
  AssignmentDiffView                        chọn 2 version/nguồn, hiện khác biệt
```

| Component | Hành vi | API |
|---|---|---|
| `TemplateListPage` | Soạn (mở trình soạn U08), phát hành, rút | `POST /api/v1/templates/{id}/release`, `.../withdraw` |
| `CopyFromTemplateDialog` | | `POST /api/v1/classes/{id}/assignments:copy-from-template` |
| `CopyToClassDialog` | Chỉ hiện lớp mình dạy | `POST /api/v1/assignments/{id}:copy-to-class` |
| `AssignmentDiffView` | Hai cột, đánh dấu thêm/bớt/đổi | `GET /api/v1/assignments/diff?from=&to=` |
