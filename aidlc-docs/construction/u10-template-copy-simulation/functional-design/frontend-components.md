# U10 Template, Copy & Simulation - Frontend Components

```
app/teaching/subjects/[id]/templates/      TemplateListPage (CN môn)
  TemplateReleaseButton, WithdrawButton
app/teaching/classes/[id]/assignments/
  CopyFromTemplateDialog                    chọn template RELEASED của môn
  CopyToClassDialog                         chọn lớp đích mình dạy
app/teaching/assignments/[id]/
  VersionHistoryPanel                       danh sách version + lineage
  AssignmentDiffView                        chọn 2 version/nguồn, hiện khác biệt
  SimulationPolicyForm                      gắn vào PublishDialog của U08
shared/assessment/SimulationBadge           "Thi thử · N lượt / không giới hạn · lấy điểm cao nhất · không tính điểm"
```

| Component | Hành vi | API |
|---|---|---|
| `TemplateListPage` | Soạn (mở trình soạn U08), phát hành, rút | `POST /api/v1/templates/{id}/release`, `.../withdraw` |
| `CopyFromTemplateDialog` | | `POST /api/v1/classes/{id}/assignments:copy-from-template` |
| `CopyToClassDialog` | Chỉ hiện lớp mình dạy | `POST /api/v1/assignments/{id}:copy-to-class` |
| `AssignmentDiffView` | Hai cột, đánh dấu thêm/bớt/đổi | `GET /api/v1/assignments/diff?from=&to=` |
| `SimulationPolicyForm` | Lượt (trống = không giới hạn), cách lấy kết quả, thời điểm hiện đáp án, tính điểm | `PUT /api/v1/publications/{id}/simulation-policy` |
