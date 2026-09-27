# U15 Grading - Frontend Components

```
app/teaching/publications/[id]/grading/     GradingQueuePage
  GradingFilters          chờ chấm / nháp / đã chốt / đã công bố / trễ
  GradingTable            người học, lượt, trễ, phương thức, điểm x/tổng, trạng thái; chọn nhiều để chốt
  BulkFinalizeDialog      kết quả từng mục
  PublishGradesButton
app/teaching/grading/attempts/[id]/        GradingWorkspacePage
  SubmissionViewer        QuestionView/DocumentEditor READONLY/Code + kết quả test
  MethodChooser           Chấm tay | Nhờ AI đề xuất (credit ước tính)
  RubricChecklistForm     tích mục, tổng tự tính; câu không rubric: ô điểm
  AiGradingPanel (U13)    đề xuất, nút "Dùng đề xuất"
  FeedbackEditor
  OverrideReasonDialog    bắt buộc khi sửa điểm tự chấm/đã chốt/khác đề xuất
  GradeHistoryDrawer
app/teaching/grading/groups/[id]/          GroupGradingPage
  GroupDocumentViewer     tài liệu nhóm, tô màu mục theo tác giả
  MemberContributionPanel mục của từng thành viên, chấm tay hoặc AI
  MemberFinalForm         điểm cuối từng người, hiện hai nguồn tham khảo
app/teaching/classes/[id]/gradebook/       GradebookPage (ma trận, không có cột tổng)
app/learn/grades/                          MyGradesPage
```

| Component | Hành vi | API |
|---|---|---|
| `GradingTable` | | `GET /api/v1/publications/{id}/grades` |
| `BulkFinalizeDialog` | | `POST /api/v1/grades:finalize` |
| `PublishGradesButton` | | `POST /api/v1/publications/{id}/grades:publish` |
| `RubricChecklistForm` | | `PUT /api/v1/grades/{id}` (có `version`) |
| `MethodChooser` AI | | `POST /api/v1/grades/{id}/ai-proposal` |
| `GradebookPage` | | `GET /api/v1/classes/{id}/gradebook` |
| `MyGradesPage` | | `GET /api/v1/me/grades` |
