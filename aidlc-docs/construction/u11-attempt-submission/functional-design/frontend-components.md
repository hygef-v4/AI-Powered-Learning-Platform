# U11 Attempt & Submission - Frontend Components

**Bản tài liệu 2026-10-08**: UC 17, 18, 19, 20, 21, 22, 24, 25; primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
app/learning/classes/[id]/assignments/        MyAssignmentsPage (màn các danh sách bài theo loại của Student: danh sách + lượt gần nhất (trạng thái, trễ), điểm đã công bố và phản hồi qua `AssignmentGradeCell` của U15 (UC 17, 18, 24); GradeDistributionBadge của U16 cạnh bài có phân bố)
app/learning/assignments/[assignmentId]/     AssignmentOverviewPage (màn Assignment Detail)
  AssignmentInfo            hướng dẫn, hạn, nộp trễ, lượt còn lại, nhãn GRADED/PRACTICE
  StartAttemptButton
  AttemptHistoryList        lượt, thời điểm, trễ, "được chấm", điểm (nếu được hiện)
app/learning/attempts/[id]/                   AttemptWorkspacePage (workspace theo loại bài cá nhân)
  AttemptHeader             đồng hồ đếm ngược, "Đã lưu lúc …", nút Nộp
  QuizWorkspace             QuestionView (U06) theo thứ tự đã trộn
  EssayWorkspace            mỗi câu tự luận một EssayEditor (U09), đề câu ở trên
  DocumentWorkspace         DocumentEditor mode STUDENT (U09)
    StudentDocxImportDialog  nhập DOCX, xem trước, xác nhận thêm block vào bản nháp
  CodeWorkspace             trình soạn code + Chạy thử (U13)
  useAutosave               10 s sau lần sửa cuối, khi rời trang, xử lý 409
  SubmitConfirmDialog       hiện lỗi kiểm theo blockId nếu có
  ReceiptView
app/learning/attempts/[id]/view               SubmittedAttemptView (màn Submission History: mở lượt gần nhất, nút ‹ › chuyển giữa các lượt; chỉ đọc, tải DOCX, kết quả Practice)
  GradeWithAiDialog         popup Grade with AI (UC 25, 35), dùng chung hai chế độ: STUDENT_PRACTICE (mở từ Assignment Detail: credit ước tính, nút "Chấm với AI", trạng thái, điểm và phản hồi Practice; quá 5 phút hiện lỗi và cho bấm lại) và TEACHER_PROPOSAL (U15 mở từ Grading Workspace: nút "Nhờ AI đề xuất", checklist đạt/không + nhận xét + bằng chứng, nút "Dùng đề xuất")
```

| Component | Hành vi | API |
|---|---|---|
| `StartAttemptButton` | Xác nhận "bắt đầu sẽ tính 1 lượt" | `POST /api/v1/assignments/{id}/attempts` |
| `useAutosave` | Gửi `contentVersion`; `409` → hộp thoại tải lại | `PUT /api/v1/attempts/{id}/content` |
| `StudentDocxImportDialog` | Preview DOCX rồi thêm block vào bản nháp bằng `contentVersion`; khung giảng viên giữ nguyên | `POST /api/v1/attempts/{id}/docx:preview`, `PUT /api/v1/attempts/{id}/content` |
| `AttemptHeader` | Đồng hồ theo `deadlineAt` của server; hết giờ gửi lưu cuối rồi chuyển sang biên nhận | - |
| `SubmitConfirmDialog` | | `POST /api/v1/attempts/{id}/submit` |
| `AttemptHistoryList` | | `GET /api/v1/assignments/{id}/attempts/mine` |
| `SubmittedAttemptView` | Mặc định lượt gần nhất; ‹ › chuyển lượt trước/sau | `GET /api/v1/attempts/{id}`, `GET .../export.docx` |
| `GradeWithAiDialog` | STUDENT_PRACTICE: chỉ lượt `PRACTICE` Text/Diagram Essay của chính mình; TEACHER_PROPOSAL: giảng viên lớp, bài `GRADED` hoặc tài liệu chung của bài nhóm. Hiện riêng "Không đủ credit AI" và "Hệ thống đang bận"; poll trạng thái (`usePollStatus`) | STUDENT_PRACTICE: `POST /api/v1/attempts/{id}/ai-grading`, `GET /api/v1/attempts/{id}/practice-result`; TEACHER_PROPOSAL: `POST /api/v1/evaluations/{id}/ai-proposal` (U15), `GET /api/v1/ai-suggestions/{id}` (U13) |

## Screen mapping theo Drawio
MyAssignmentsPage dùng lại cho Quiz Assignments, Codelab Assignments, Text Essay Assignments, Diagram Essay Assignments và Group Essay Assignments (UC 17), với filter type cố định theo entry. AssignmentOverviewPage hiển thị nhãn Assignment Detail UC 18. QuizWorkspace, EssayWorkspace, CodeWorkspace, DocumentWorkspace có nhãn Quiz Workspace, Text Essay Workspace, Codelab Workspace, Diagram Essay Workspace (UC 19–22).
Submission History UC 24 là entry lịch sử/lượt chỉ đọc; component AttemptHistoryList/SubmittedAttemptView có thể dùng lại. GradeWithAiDialog STUDENT_PRACTICE mở từ Assignment Detail sau khi chọn lượt Practice đã nộp của chính mình (UC 25); chế độ TEACHER_PROPOSAL chỉ U15 mở trong Grading Workspace (UC 35). Không gộp hai quyền dù reuse component.
