# U11 Attempt & Submission - Frontend Components

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Student Assignments | `StudentAssignmentsPage` | 22 | Student | Class Dashboard; Student Class Detail (lọc sẵn lớp) |
| Assignment Detail | `AssignmentDetailPage` | 23 | Student | Student Assignments |
| Text Essay Workspace | `AttemptWorkspacePage` (`EssayWorkspace`) | 24 | Student | Assignment Detail |
| Codelab Workspace | `AttemptWorkspacePage` (`CodeWorkspace`) | 25 | Student | Assignment Detail |
| Diagram Essay Workspace | `AttemptWorkspacePage` (`DocumentWorkspace`) | 26 | Student | Assignment Detail |
| Submission History | `SubmissionHistoryPage` | 28, 29 | Student | Assignment Detail |
| Quiz Practice History | `QuizPracticeHistoryPage` | 18 | Student | Class Dashboard |
| Quiz Practice Detail | `QuizPracticeDetailPage` | 19 | Student | Quiz Practice History |
| Quiz Taking | `AttemptWorkspacePage` (`QuizWorkspace`) | 20 | Student | Quiz Practice Detail; Learning Material (`LessonQuizList`) |
| Quiz Result | `QuizResultPage` | 21 | Student | Quiz Taking (sau khi nộp); Quiz Practice History |

Group Essay Workspace (UC 27) thuộc U14, mở từ Assignment Detail của bài nhóm. Learning Material (UC 15) thuộc U05 và nhúng `LessonQuizList` của U11. Teacher nhờ AI đề xuất chấm (UC 38) là panel riêng của U15 trong Grading Workspace; U11 không cung cấp component chung cho phần này. Admin không có màn nào của U11; Teacher, Subject Manager không có lối vào các màn này trên Class Dashboard.

## 2. Cây component

```
app/classes/assignments/                         StudentAssignmentsPage   màn Student Assignments (?classId= lọc sẵn)
  AssignmentFilters                              lớp mình học, dạng bài, trạng thái
  StudentAssignmentTable                         lớp, tiêu đề, dạng, chế độ, nhãn "Của môn", hạn, lượt gần nhất
    AssignmentGradeCell                          của U15: điểm đã công bố và phản hồi
    GradeDistributionBadge                       của U16: phân bố điểm ẩn danh
app/classes/[id]/assignments/[assignmentId]/     AssignmentDetailPage     màn Assignment Detail
  AssignmentInfo                                 hướng dẫn, dạng, chế độ, lịch, nộp trễ, lượt còn lại
  QuestionView                                   của U06: đề từng câu Text Essay, Code Lab (ẩn đáp án)
  SkeletonView                                   khung tài liệu chỉ đọc (Diagram Essay, bài nhóm)
  RubricView                                     của U06: rubric từng câu hoặc từng phần
  LatestAttemptCard                              lượt gần nhất, trạng thái, trễ
  StartAttemptButton                             Bắt đầu làm / Tiếp tục
  GroupWorkButton                                bài nhóm: trạng thái nộp của nhóm, Mở bài nhóm (U14)
app/classes/[id]/assignments/[assignmentId]/submissions/  SubmissionHistoryPage  màn Submission History
  AttemptHistoryList                             lượt, thời điểm, trễ, cách nộp, "được chấm", điểm nếu được hiện
  SubmittedAttemptView                           lượt đang xem (mặc định gần nhất, nút ‹ ›), chỉ đọc, tải DOCX
    ReceiptView                                  biên nhận
    PracticeResultPanel                          kết quả Practice hoặc "chưa chấm AI", nút "Chấm với AI"
    PracticeAiGradingDialog                      hộp xác nhận chấm AI lượt Practice của chính mình (UC 29)
  GroupSubmissionView                            của U14: bản nộp nhóm khi bài là bài nhóm
app/classes/quizzes/                             QuizPracticeHistoryPage  màn Quiz Practice History (?classId= lọc sẵn)
  ClassFilter                                    lọc theo lớp mình học
  QuizAttemptGroupList                           nhóm theo quiz: lượt, thời điểm, điểm nếu được hiện
app/classes/[id]/quizzes/[quizId]/               QuizPracticeDetailPage   màn Quiz Practice Detail
  QuizInfo                                       học liệu, số câu, tổng điểm, giới hạn giờ, lượt còn lại, cài đặt hiện điểm/đáp án
  StartAttemptButton                             Làm quiz / Tiếp tục
app/classes/[id]/attempts/[attemptId]/           AttemptWorkspacePage     Quiz Taking hoặc workspace theo dạng bài
  AttemptHeader                                  đồng hồ (quiz có giới hạn giờ, bài tập theo hạn), "Đã lưu lúc …", nút Nộp
  QuizWorkspace                                  QuestionView (U06) theo thứ tự đã trộn
  EssayWorkspace                                 mỗi câu tự luận một EssayEditor (U09), đề câu ở trên
  DocumentWorkspace                              DocumentEditor mode STUDENT (U09)
    StudentDocxImportDialog                      của U09: nhập DOCX, xem trước, xác nhận thêm block
  CodeWorkspace                                  CodeEditor + CodeRunResult (U13), nút Chạy thử
  useAutosave                                    10 s sau lần sửa cuối, khi rời trang, xử lý 409
  SubmitConfirmDialog                            hiện lỗi kiểm theo blockId nếu có
app/classes/[id]/attempts/[attemptId]/result/    QuizResultPage           màn Quiz Result
  QuizScoreSummary                               điểm khi showScoreAfterSubmit
  QuizAnswerReview                               lựa chọn của mình; đáp án đúng, giải thích khi AFTER_SUBMIT
shared/attempts/LessonQuizList                   gắn vào LearningMaterialPage (U05): quiz của học liệu, nút "Làm quiz"
```

Route theo `RoleGuard` của U01: `/classes/*` cho Student, Teacher, Subject Manager; backend vẫn kiểm ghi danh và chủ lượt nên chỉ Student dùng được. Route cũ `app/learning/...` bỏ.

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `StudentAssignmentsPage`, `AssignmentFilters`, `StudentAssignmentTable` | Danh sách gộp các lớp `OPEN` mình học; lọc lớp, dạng, trạng thái; bấm bài → Assignment Detail (UC 22) | `GET /api/v1/me/assignments?classId=&type=&status=` |
| `AssignmentDetailPage` (`AssignmentInfo`, `QuestionView`, `SkeletonView`, `RubricView`, `LatestAttemptCard`) | Hướng dẫn, đề hoặc khung, tiêu chí chấm, hạn, lượt còn lại; nút "Lịch sử nộp" (UC 23) | `GET /api/v1/assignments/{assignmentId}/overview?classId=` |
| `StartAttemptButton` | Xác nhận "bắt đầu sẽ tính 1 lượt" (bỏ qua khi quiz không giới hạn lượt); có lượt dở thì "Tiếp tục"; mở workspace hoặc Quiz Taking | `POST /api/v1/assignments/{assignmentId}/attempts` |
| `GroupWorkButton` | Bài nhóm: trạng thái nộp của nhóm, mở Group Essay Workspace (U14); ẩn khi chưa có U14 | Trong `overview` (dữ liệu từ `GroupSubmissionQueryPort`) |
| `AttemptWorkspacePage`, `AttemptHeader` | Đồng hồ theo `deadlineAt` của server; hết giờ gửi lưu cuối rồi chuyển biên nhận (quiz: Quiz Result) | `GET /api/v1/attempts/{attemptId}` |
| `useAutosave` | Gửi `contentVersion`, gzip; `409` → hộp thoại tải lại | `PUT /api/v1/attempts/{attemptId}/content` |
| `StudentDocxImportDialog` | Preview DOCX rồi thêm block vào bản nháp bằng `contentVersion`; khung giảng viên giữ nguyên (UC 26) | `POST /api/v1/attempts/{attemptId}/docx:preview`, `PUT /api/v1/attempts/{attemptId}/content` |
| `CodeWorkspace` | Chạy thử với test công khai, 5 lần/phút (UC 25) | `POST /api/v1/code-runs`, `GET /api/v1/code-runs/{id}` (U13) |
| `SubmitConfirmDialog`, `ReceiptView` | Nộp tay; hiện biên nhận; bài tập có lối sang Submission History | `POST /api/v1/attempts/{attemptId}/submit` |
| `SubmissionHistoryPage`, `AttemptHistoryList`, `SubmittedAttemptView` | Mặc định lượt gần nhất; ‹ › chuyển lượt; tải DOCX (UC 28) | `GET /api/v1/assignments/{assignmentId}/attempts/mine`, `GET /api/v1/attempts/{attemptId}`, `GET /api/v1/attempts/{attemptId}/export.docx` |
| `PracticeResultPanel` | Kết quả Practice (Code Lab, AI) hoặc "chưa chấm AI" kèm nút "Chấm với AI" | `GET /api/v1/attempts/{attemptId}/practice-result` |
| `PracticeAiGradingDialog` | Chỉ lượt `PRACTICE` Text/Diagram Essay đã nộp của chính mình; credit ước tính và số dư, "Không đủ credit AI", "Hệ thống đang bận", poll trạng thái, quá 5 phút báo lỗi và cho bấm lại (UC 29) | `POST /api/v1/attempts/{attemptId}/ai-grading`, `GET /api/v1/attempts/{attemptId}/practice-result`, `GET /api/v1/me/credits` (U07) |
| `QuizPracticeHistoryPage`, `ClassFilter`, `QuizAttemptGroupList` | Lượt quiz gộp các lớp, lọc lớp; bấm quiz → Quiz Practice Detail; lượt đã nộp → Quiz Result; lượt dở → Quiz Taking (UC 18) | `GET /api/v1/me/quiz-attempts?classId=` |
| `QuizPracticeDetailPage`, `QuizInfo` | Thông tin quiz, lượt còn lại; ẩn nút làm khi quiz không còn `OPEN`, học liệu bị lưu trữ hoặc hết lượt (UC 19) | `GET /api/v1/assignments/{quizId}/overview?classId=` |
| `QuizResultPage` | Điểm và đáp án theo cài đặt quiz (UC 21) | `GET /api/v1/attempts/{attemptId}`, `GET /api/v1/attempts/{attemptId}/practice-result` |
| `LessonQuizList` | Quiz `OPEN` của học liệu, lượt còn lại, điểm lượt gần nhất; "Làm quiz" bắt đầu hoặc tiếp tục rồi mở Quiz Taking (UC 15, 20) | `GET /api/v1/classes/{classId}/lessons/{lessonId}/quizzes`, `POST /api/v1/assignments/{quizId}/attempts` |
