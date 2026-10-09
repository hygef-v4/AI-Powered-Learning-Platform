# U15 Grading - Frontend Components

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Submission Detail | `SubmissionDetailPage` | 37, 38 (hàng loạt), chốt/công bố hàng loạt | Teacher, Subject Manager được giao dạy lớp | Teacher Class Detail (tab Evals, nút "Bài nộp" ở từng bài) |
| Grading Workspace | `GradingWorkspacePage` | 38, 39, chốt/công bố từng bài | Teacher, Subject Manager được giao dạy lớp | Submission Detail |
| Sổ điểm (phần của tab Evals, không có màn riêng) | `GradebookPanel` (gắn vào `EvalsTab` của U08) | 40 | Teacher, Subject Manager được giao dạy lớp | Teacher Class Detail (tab Evals, nút "Sổ điểm") |
| Student Assignments, Assignment Detail, Submission History (màn của U11) | `AssignmentGradeCell` (U11 nhúng) | 22, 23, 28 | Student | Class Dashboard, Student Assignments |

AI đề xuất chấm của giảng viên (UC 38) là panel `AiProposalPanel` do U15 tự làm trong Grading Workspace, và `BulkAiGradingDialog` trên Submission Detail; không nhúng `GradeWithAiDialog` của U11. Hộp xác nhận chốt hàng loạt, hộp lý do sửa điểm là dialog, không phải màn trong screen flow. Admin không có màn nào của U15.

## 2. Cây component

```
app/classes/[id]/teaching/assignments/[assignmentId]/submissions/   SubmissionDetailPage  (màn Submission Detail)
  SubmissionHeader        bài (nhãn "Của môn" nếu là bài của môn), dạng, hạn, số đã nộp / số sinh viên của lớp
  SubmissionFilters       chờ chấm / nháp / đã chốt / đã công bố / trễ
  SubmissionProgressPanel (U16) tiến độ nộp của bài trong lớp, người chưa nộp
  GroupDocsOverviewPanel  (U14) chỉ bài nhóm: tiến độ phần của từng nhóm, nhả khóa phần, mở bản nộp nhóm
  SubmissionTable         người học (hoặc nhóm), lượt, thời điểm nộp, trễ, điểm x/tổng, trạng thái; chọn nhiều; nút "Chấm"
  SubmissionPreview       xem nhanh bài nộp chỉ đọc của dòng đang chọn (dùng SubmissionViewer)
  BulkAiGradingDialog     chấm hàng loạt bằng AI: số bài, credit ước tính, tiến độ lô; xong thì "Xác nhận từng bài" mở Grading Workspace
  FinalizeConfirmDialog   chốt hàng loạt, kết quả từng mục
  BulkPublishButton       "Công bố hàng loạt" các bài đã chốt được chọn (có "chọn tất cả đã chốt"), kết quả từng mục
app/classes/[id]/teaching/grading/[targetKind]/[evaluationId]/      GradingWorkspacePage  (màn Grading Workspace; targetKind = attempts | groups)
  SubmissionNavigator     nút ‹ › theo thứ tự của Submission Detail hoặc lô; lưu xong tự sang bài kế
  SubmissionViewer        QuestionView (U06) / DocumentEditor READONLY (U09) / code + kết quả test; nút "Tải DOCX"
  MethodChooser           Chấm tay | Nhờ AI đề xuất (credit ước tính; ẩn với Code Lab)
  RubricChecklistForm     checklist rubric theo từng câu (Text Essay) hoặc từng phần (Diagram Essay, bài nhóm), điểm từng mục và tổng
  AiProposalPanel         panel AI đề xuất (U15): nút "Nhờ AI đề xuất", trạng thái, checklist đạt/không + nhận xét đề xuất, nút "Dùng đề xuất"
  FeedbackEditor          phản hồi; khi dùng đề xuất AI thì điền sẵn phần giải thích của AI, sửa nếu muốn
  OverrideReasonDialog    bắt buộc khi sửa điểm tự chấm hoặc điểm đã chốt
  FinalizeButton          chốt bài đang mở
  PublishButton           công bố bài đang mở (khi đã chốt)
  GradeHistoryDrawer      đọc `history` của đánh giá
  GroupGradingMode        (chỉ khi targetKind = groups)
    GroupDocumentViewer     tài liệu chung như bài DOCUMENT (không tô màu theo tác giả), chỉ rõ mục còn trống
    MemberScoresPanel       điểm đóng góp từng thành viên, mặc định bằng điểm tài liệu chung; chấm tay từng người, lý do tùy chọn
shared/grades/GradebookPanel           sổ điểm gắn vào EvalsTab (U08): theo sinh viên, mỗi sinh viên đóng/mở, không có điểm tổng
  GradebookStudentRow     các bài GRADED (của lớp và của môn), điểm x/tổng, trạng thái; bấm ô điểm mở GradeHistoryDrawer
  GradebookExportAction   (U16) nút xuất CSV/XLSX
shared/grades/AssignmentGradeCell      điểm đã công bố và phản hồi của từng bài; U11 gắn vào Student Assignments, Assignment Detail, Submission History
```

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `SubmissionHeader`, `SubmissionFilters`, `SubmissionTable` | Chỉ bài nộp của lớp đang mở (bài của môn cũng vậy); không có quiz, không có bài `PRACTICE`; trạng thái chấm từng dòng (UC 37) | `GET /api/v1/classes/{classId}/assignments/{assignmentId}/evaluations` |
| `SubmissionPreview`, `SubmissionViewer` | Nội dung chỉ đọc, kết quả test Code Lab; tải DOCX tài liệu | `GET /api/v1/evaluations/{id}`, `GET /api/v1/evaluations/{id}/document.docx` |
| `BulkAiGradingDialog` | Chỉ bài có rubric; kiểm credit cả lô trước khi chạy; theo dõi tiến độ (UC 38) | `POST /api/v1/evaluations:ai-proposal-batch`, `GET /api/v1/ai-suggestions?ids=` (U13) |
| `FinalizeConfirmDialog`, `FinalizeButton` | Chỉ bài `DRAFT` hợp lệ; trả kết quả từng mục | `POST /api/v1/evaluations:finalize` |
| `BulkPublishButton` | Chỉ bài `FINALIZED`; trả kết quả từng mục | `POST /api/v1/evaluations:publish` |
| `PublishButton` | Công bố bài đang mở | `POST /api/v1/evaluations/{id}/publish` |
| `SubmissionNavigator` | Thứ tự theo bộ lọc của Submission Detail hoặc danh sách lô | `GET /api/v1/classes/{classId}/assignments/{assignmentId}/evaluations` |
| `RubricChecklistForm`, `FeedbackEditor`, `OverrideReasonDialog`, `MemberScoresPanel` | Lưu có `version`; `409` báo tải lại; lý do bắt buộc khi sửa điểm tự chấm/đã chốt (UC 39) | `PUT /api/v1/evaluations/{id}` |
| `MethodChooser` AI, `AiProposalPanel` | Mở panel trong Grading Workspace; U15 gọi `AiGradingPort`, panel poll trạng thái của U13; báo riêng "Không đủ credit AI", "Hệ thống đang bận", quá 5 phút báo lỗi và cho thử lại (UC 38) | `POST /api/v1/evaluations/{id}/ai-proposal`, `GET /api/v1/ai-suggestions/{id}` (U13) |
| `GradeHistoryDrawer` | Lịch sử trước/sau, người sửa, lý do | `GET /api/v1/evaluations/{id}` |
| `GradebookPanel` | Theo sinh viên đang ghi danh, đóng/mở, không điểm tổng (UC 40) | `GET /api/v1/classes/{classId}/gradebook` |
| `GradebookExportAction` (U16) | Xuất CSV/XLSX (UC 40) | `GET /api/v1/classes/{classId}/gradebook/export` (U16) |
| `AssignmentGradeCell` | Chỉ điểm `PUBLISHED` của chính mình, nhãn "đã cập nhật" khi điểm bị sửa (UC 22, 23, 28) | `GET /api/v1/me/evaluations?classId=` |

Kết quả quiz luyện tập và kết quả Practice hiển thị ở màn của U11 (Quiz Result, Assignment Detail), đọc qua `GradeQueryPort`; U15 không có component cho Practice.
