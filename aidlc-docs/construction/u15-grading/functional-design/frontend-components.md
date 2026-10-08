# U15 Grading - Frontend Components

**Bản tài liệu 2026-10-08**: UC 34, 35, 36, 37; primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
app/teaching/assignments/[id]/grading/     GradingQueuePage (màn Student Submissions)
  GradingFilters          chờ chấm / nháp / đã chốt / đã công bố / trễ
  GroupDocsOverviewPanel  (U14) chỉ bài nhóm: tiến độ phần của từng nhóm, nhả khóa phần, mở bản nộp nhóm
  GradingTable            người học (hoặc nhóm), lượt, trễ, điểm x/tổng, trạng thái; chọn nhiều để chấm hàng loạt bằng AI, chốt hoặc công bố hàng loạt
  BulkAiGradingDialog     chấm hàng loạt bằng AI: số bài, credit ước tính, tiến độ lô; xong thì nút "Xác nhận từng bài" mở Grading Workspace
  FinalizeGradesDialog    popup Finalize Grades (UC 36): chốt hàng loạt, kết quả từng mục
  BulkPublishButton       "Công bố hàng loạt": công bố các bài đã chốt được chọn (có "chọn tất cả đã chốt"), kết quả từng mục
app/teaching/grading/[targetKind]/[id]/    GradingWorkspacePage (targetKind = attempts | groups)
  SubmissionNavigator     nút ‹ › chuyển bài theo thứ tự hàng chờ hoặc lô; lưu xong tự sang bài kế
  SubmissionViewer        QuestionView/DocumentEditor READONLY/Code + kết quả test
  MethodChooser           Chấm tay | Nhờ AI đề xuất (credit ước tính)
  RubricChecklistForm     tích mục rubric; checklist theo từng câu (Text Essay) hoặc từng phần (Diagram Essay, bài nhóm), điểm từng mục và tổng
  PublishButton           công bố bài đang mở (khi đã chốt)
  GradeWithAiDialog (U11) popup Grade with AI (UC 35) chế độ TEACHER_PROPOSAL: đề xuất, nút "Dùng đề xuất"
  FeedbackEditor          phản hồi; khi dùng đề xuất AI thì điền sẵn phần giải thích của AI, sửa nếu muốn
  OverrideReasonDialog    bắt buộc khi sửa điểm tự chấm hoặc điểm đã chốt
  GradeHistoryDrawer      đọc `evaluations.history`
  GroupGradingMode        (chỉ khi targetKind = groups)
    GroupDocumentViewer     tài liệu chung như bài DOCUMENT (không tô màu theo tác giả)
    MemberScoresPanel       điểm đóng góp từng thành viên, mặc định bằng điểm tài liệu chung; chấm tay từng người, lý do tùy chọn
app/teaching/classes/[id]/gradebook/       GradebookPage (Gradebook trong Class Detail: UC 37 — danh sách theo sinh viên, mỗi sinh viên đóng/mở xem các bài và điểm, không có điểm tổng; lịch sử sửa điểm của UC 37 mở bằng `GradeHistoryDrawer`; nút xuất bảng điểm (UC 37) do U16 gắn vào)
shared/grades/AssignmentGradeCell          điểm đã công bố và phản hồi của từng bài, gắn vào MyAssignmentsPage (các danh sách bài theo loại của Student, U11)
```

| Component | Hành vi | API |
|---|---|---|
| `GradingTable` | | `GET /api/v1/assignments/{id}/evaluations` |
| `FinalizeGradesDialog` | | `POST /api/v1/evaluations:finalize` |
| `BulkPublishButton` | Chỉ bài `FINALIZED`; trả kết quả từng mục | `POST /api/v1/evaluations:publish` (danh sách id) |
| `PublishButton` | | `POST /api/v1/evaluations/{id}/publish` |
| `RubricChecklistForm` | | `PUT /api/v1/evaluations/{id}` (có `version`) |
| `MethodChooser` AI | Mở `GradeWithAiDialog` chế độ `TEACHER_PROPOSAL`; U15 gọi `AiGradingPort`, popup poll trạng thái của U13 | `POST /api/v1/evaluations/{id}/ai-proposal`, `GET /api/v1/ai-suggestions/{id}` (U13) |
| `BulkAiGradingDialog` | Kiểm credit cả lô trước khi chạy; theo dõi tiến độ | `POST /api/v1/evaluations:ai-proposal-batch`, `GET /api/v1/ai-suggestions?ids=` |
| `SubmissionNavigator` | Thứ tự theo bộ lọc hàng chờ hoặc danh sách lô | `GET /api/v1/assignments/{id}/evaluations` |
| `GradebookPage` | | `GET /api/v1/classes/{id}/gradebook` |
| `AssignmentGradeCell` | Chỉ điểm `PUBLISHED` của chính mình, hiện cạnh từng bài trên danh sách bài theo loại (UC 17) | `GET /api/v1/me/evaluations?classId=` |

## Screen entries
GradingQueuePage hiển thị nhãn Student Submissions UC 34; GradingWorkspacePage là Grading Workspace UC 35–36 với AI Grading Proposals cho Teacher. GradebookPage/export UC 37 là panel/entry trên Class Detail, không suy scope từ quyền cấu trúc; component có thể tái dùng. Student xem điểm của mình trên danh sách bài/Assignment Detail/Submission History, không mở Teacher Gradebook.
