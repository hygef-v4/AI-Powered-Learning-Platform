# U11 Attempt & Submission - Frontend Components

```
app/learn/classes/[id]/assignments/        MyAssignmentsPage (danh sách + trạng thái)
app/learn/assignments/[publicationId]/     AssignmentOverviewPage
  AssignmentInfo            hướng dẫn, hạn, nộp trễ, lượt còn lại, SimulationBadge (U10)
  StartAttemptButton
  AttemptHistoryList        lượt, thời điểm, trễ, "được chấm", điểm (nếu được hiện)
app/learn/attempts/[id]/                   AttemptWorkspacePage
  AttemptHeader             đồng hồ đếm ngược, "Đã lưu lúc …", nút Nộp
  QuizWorkspace             QuestionView (U06) theo thứ tự đã trộn
  EssayWorkspace            EssayEditor (U09)
  DocumentWorkspace         DocumentEditor mode LEARNER (U09)
    LearnerDocxImportDialog  nhập DOCX, xem trước, xác nhận thêm block vào bản nháp
  CodeWorkspace             trình soạn code + Chạy thử (U13)
  useAutosave               10 s sau lần sửa cuối, khi rời trang, xử lý 409
  SubmitConfirmDialog       hiện lỗi kiểm theo blockId nếu có
  ReceiptView
app/learn/attempts/[id]/view               SubmittedAttemptView (chỉ đọc, tải DOCX)
```

| Component | Hành vi | API |
|---|---|---|
| `StartAttemptButton` | Xác nhận "bắt đầu sẽ tính 1 lượt" | `POST /api/v1/publications/{id}/attempts` |
| `useAutosave` | Gửi `contentVersion`; `409` → hộp thoại tải lại | `PUT /api/v1/attempts/{id}/content` |
| `LearnerDocxImportDialog` | Preview DOCX rồi thêm block vào bản nháp bằng `contentVersion`; khung giảng viên giữ nguyên | `POST /api/v1/attempts/{id}/docx:preview`, `PUT /api/v1/attempts/{id}/content` |
| `AttemptHeader` | Đồng hồ theo `deadlineAt` của server; hết giờ gửi lưu cuối rồi chuyển sang biên nhận | - |
| `SubmitConfirmDialog` | | `POST /api/v1/attempts/{id}/submit` |
| `AttemptHistoryList` | | `GET /api/v1/publications/{id}/attempts/mine` |
| `SubmittedAttemptView` | | `GET /api/v1/attempts/{id}`, `GET .../export.docx` |
