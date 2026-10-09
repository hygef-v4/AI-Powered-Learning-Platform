# U08 Assessment Core & Publication - Frontend Components

**Bản tài liệu 2026-10-09**: UC 41 và vòng đời (tạo, xóa, duyệt, phát hành, lịch, ngưng giao, nhân bản, version) của UC 35, 42, 43, 44, 45 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Tab Evals | `EvalsTab` (gắn vào `TeacherClassPage` của U04) | 41 | Teacher, Subject Manager được giao dạy | Teacher Class Detail |
| Assignment List | `SubjectAssignmentListPage` | 42–45 (cấp môn) | Subject Manager | Manager Dashboard |
| Quiz List | `SubjectQuizListPage` | 35 (cấp môn) | Subject Manager | Manager Dashboard |
| Assignment Form | `AssignmentFormPage` (U08 khung và vòng đời; U09 nội dung dạng bài) | 42–45 | Teacher, Subject Manager | Tab Evals, Assignment List |
| Quiz Detail | `QuizDetailPage` (U08 câu và vòng đời; U09 cài đặt quiz) | 35 | Teacher, Subject Manager | Tab Evals, Quiz List |

## 2. Cây component

```
app/classes/[id]/teaching/  EvalsTab                   tab Evals
  AssignmentSection                                     bài của lớp + bài của môn (nhãn "Của môn", chỉ đọc), lọc; nút "Bài nộp" mở Submission Detail (U15)
  QuizSection                                           quiz của lớp + quiz của môn (chỉ đọc), học liệu gắn
  GradebookPanel                                        (U15) mục "Sổ điểm", nút xuất CSV/XLSX của U16
  CreateAssignmentDialog                                chọn dạng và chế độ, rồi nguồn: bài trống hoặc copy bài lớp khác
    CopyFromClassDialog                                 chọn lớp nguồn mình dạy rồi bài cùng dạng và chế độ
  CreateQuizDialog                                      chọn học liệu của lớp
app/classes/[id]/teaching/assignments/[assignmentId]/  AssignmentFormPage  (bài của lớp)
app/classes/[id]/teaching/quizzes/[quizId]/             QuizDetailPage      (quiz của lớp)
app/manager/assignments/                                SubjectAssignmentListPage  chọn môn, bài của môn, Tạo bài
app/manager/assignments/[assignmentId]/                 AssignmentFormPage  (bài của môn)
app/manager/quizzes/                                    SubjectQuizListPage chọn môn, quiz của môn, Tạo quiz (chọn học liệu của môn)
app/manager/quizzes/[quizId]/                           QuizDetailPage      (quiz của môn)
AssignmentFormPage
  AssignmentHeader        tiêu đề, dạng, chế độ, phạm vi (lớp/môn), trạng thái, tổng điểm
  InstructionsEditor
  AssignmentQuestionList  Text Essay, Code Lab: thứ tự, điểm, bỏ câu
    QuestionPicker        của U06: chọn tay; tab "Chọn ngẫu nhiên"
    QuestionEditor        của U06: câu riêng của bài
    AiDraftDialog         nhờ AI soạn câu
  TypeConfigSlot          chỗ U09 gắn phần riêng: rubric từng câu Text Essay, trạng thái kiểm lời giải Code Lab, khung và rubric phần (popup Rubric Detail)
  PreviewDialog
  ReviewButton            hiện danh sách lỗi nếu chưa đạt
  PublishDialog           mở/đóng, hạn nộp trễ, số lượt; cảnh báo khóa nội dung và rubric; bài nhóm nhúng GroupReadinessPanel (U12) và ô xác nhận cảnh báo
  ScheduleSection         lịch hiện tại, sửa lịch, Ngưng giao (lý do)
  DeleteButton, CloneButton, NewVersionButton
QuizDetailPage
  QuizHeader              tiêu đề, học liệu gắn, trạng thái, tổng điểm
  QuizQuestionList        thứ tự, điểm, bỏ câu
    QuestionPicker        của U06: chọn tay; tab "Chọn ngẫu nhiên" (số câu + bộ lọc, bốc lại từng câu)
    QuestionEditor        của U06: câu trắc nghiệm riêng của quiz
    AiDraftDialog         nhờ AI soạn câu, chọn câu giữ lại (hiện credit sẽ dùng)
  QuizSettingsSlot        chỗ U09 gắn cài đặt quiz (thời gian, số lượt, xáo câu, hiện đáp án)
  PreviewDialog, ReviewButton, PublishQuizButton, RetireButton, DeleteButton, NewVersionButton
```

Giảng viên mở bài/quiz của môn thì `AssignmentFormPage`/`QuizDetailPage` ở chế độ chỉ đọc, không có nút soạn hay vòng đời.

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `EvalsTab` (`AssignmentSection`, `QuizSection`) | Lọc theo dạng, chế độ, trạng thái; mặc định ẩn `CLOSED`/`RETIRED` (UC 41) | `GET /api/v1/classes/{classId}/assignments`, `GET /api/v1/classes/{classId}/quizzes` |
| `CreateAssignmentDialog`, `CopyFromClassDialog` | Bước 1 dạng + chế độ; bước 2 nguồn (bài trống, copy bài lớp khác); copy xong mở Assignment Form của bài mới | `POST /api/v1/classes/{classId}/assignments`, `GET /api/v1/classes/{classId}/copy-sources`, `POST /api/v1/classes/{classId}/assignments:copy-from-class` |
| `CreateQuizDialog` | Chọn học liệu của lớp | `POST /api/v1/classes/{classId}/quizzes` |
| `SubjectAssignmentListPage` | Bài của môn; tạo bài (không có dạng bài nhóm) | `GET`, `POST /api/v1/subjects/{subjectId}/assignments` |
| `SubjectQuizListPage` | Quiz của môn; tạo quiz chọn học liệu của môn | `GET`, `POST /api/v1/subjects/{subjectId}/quizzes` |
| `AssignmentHeader`, `InstructionsEditor`, `DeleteButton` | Sửa khi `DRAFT`; từ `SCHEDULED` hiện nhãn "Đã khóa"; Xóa khi chưa từng phát hành | `GET`, `PATCH`, `DELETE /api/v1/assignments/{id}` |
| `QuizQuestionList`, `AssignmentQuestionList` | Chỉ sửa khi `DRAFT`; chỉ cho chọn câu khớp dạng | `POST`, `PATCH`, `DELETE /api/v1/assignments/{id}/questions` |
| `AiDraftDialog` | Gửi yêu cầu (U08 chuyển sang `AiDraftPort`), poll đề xuất, hiện câu và nguồn trích dẫn | `POST /api/v1/assignments/{id}/ai-drafts`, `GET /api/v1/ai-suggestions/{id}` (U13) |
| `ReviewButton` | Duyệt; lỗi hiện theo mục (câu, rubric trống, cấu hình, lời giải mẫu) | `POST /api/v1/assignments/{id}/review` |
| `PublishDialog`, `PublishQuizButton` | Bài tập: kiểm lịch phía client, cảnh báo khóa; quiz: phát hành ngay | `POST /api/v1/assignments/{id}/publish` |
| `ScheduleSection`, `RetireButton` | Sửa lịch; ngưng giao kèm lý do | `PATCH /api/v1/assignments/{id}/schedule`, `POST /api/v1/assignments/{id}/retire` |
| `CloneButton`, `NewVersionButton` | Nhân bản; "Sửa (tạo version mới)" khi bài `CLOSED`/`RETIRED` hoặc quiz đã phát hành | `POST /api/v1/assignments/{id}/clone`, `POST /api/v1/assignments/{id}/versions` |
