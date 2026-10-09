# U11 Attempt & Submission - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U11. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-ASM-003, US-ASM-012; phần làm bài của US-ASM-004, US-ASM-006 (quiz), US-ASM-007.
- **Primary UC hiện hành**: UC 18, 19, 20, 21 (quiz luyện tập), 22, 23, 24, 25, 26, 28, 29 theo bản 73 UC; `LessonQuizList` trên Learning Material (UC 15 của U05). UC 27 (bài nhóm) thuộc U14, UC 38 (Teacher nhờ AI đề xuất chấm) thuộc U15, U15 tự làm panel trong Grading Workspace. Supporting flows theo current-srs-contract.md.
- **Quyết định 2026-10-09**: chỉ Student (R5) dùng U11, Admin không có quyền; quiz là quiz luyện tập gắn học liệu (luôn `PRACTICE`, không lịch, `maxAttempts` 1-10 hoặc không giới hạn, không vào sổ điểm), làm từ Learning Material hoặc Quiz Practice; bỏ quiz `GRADED`, lịch quiz và `AFTER_CLOSE` (cài đặt quiz `showCorrectAnswers` chỉ `NEVER`/`AFTER_SUBMIT`; `timeLimitMinutes` 1-300, hết giờ tự nộp); Student Assignments và Quiz Practice History mở từ Class Dashboard là danh sách gộp các lớp mình học, lọc theo lớp; Student Assignments gồm bài của lớp và bài của môn; mỗi lượt ghi `class_id`; nút "Chấm với AI" (UC 29) trên Submission History; học liệu bị lưu trữ thì quiz ẩn với sinh viên nhưng lượt cũ vẫn xem được.
- **Thiết kế nguồn**: `construction/u11-attempt-submission/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ScheduledScanner` | U03 | Dùng thật (tự nộp theo hạn, `AttemptDeadlineScanner`) |
| `ArtifactPort` | U03 | Dùng thật (ảnh `DOCUMENT_IMAGE`) |
| `ClassAccessPort` | U04 | Dùng thật (ghi danh, `listOpenClassesOf`) |
| `ContentRefPort` | U05 | Dùng thật (học liệu của quiz còn hiện trong lớp) |
| `BankQueryPort`, `RubricPort`, `QuestionView`, `RubricView` | U06 | Dùng thật (đề góc nhìn người học, đáp án cho Quiz Result, rubric) |
| `AssignmentQueryPort` (`listForClass`, `listQuizzesOfLesson`, `isSubmissionOpen`) | U08 | Dùng thật |
| `TypeConfigPort`, `DocumentModelPort`, `DocxExportPort`, `DocxStudentImportPort`, `DocumentEditor`, `EssayEditor`, `StudentDocxImportDialog` | U09 | Dùng thật |
| `CodeRunPort`, `CodeEditor`, `CodeRunResult` | U13 (`C`) | Adapter tạm báo "chạy thử chưa sẵn sàng"; Practice Code Lab chưa có kết quả; U13 thay |
| `PracticeGradingPort` | U13 (`C`) | Adapter tạm báo "AI chưa sẵn sàng" (nút "Chấm với AI" hiện thông báo); U13 thay |
| `GroupSubmissionQueryPort` | U14 (`C`) | Adapter rỗng: bài nhóm không hiện trạng thái, ẩn nút "Mở bài nhóm"; U14 thay |
| `SubmissionSubmittedPort` | U15 (`C`, U11 khai báo) | Adapter rỗng; U15 (ngay sau U14 trong wave 3) cài |
| `PracticeResultPort` | U15 (`C`, U11 khai báo `scoreQuiz`) | Adapter rỗng: Quiz Result hiện "chưa có kết quả" tới khi U15 cài |
| `GradeQueryPort`, `AssignmentGradeCell` | U15 (`C`) | Ẩn phần điểm tới khi U15 có |
| `GradeDistributionBadge` | U16 (`C`) | Không hiện tới khi U16 có |
| U11 cài `AssignmentLifecyclePort` (U08 khai báo) | cho U08 | Thay adapter rỗng của U08: `onRetired` đặt hạn ngay cho lượt dở |
| U11 cung cấp `SubmissionQueryPort`, `AttemptRunResultPort`, `LessonQuizList` | cho U05, U13, U15, U16 | U05 nhúng danh sách quiz; U13 ghi `run_result`; U15, U16 đọc bài nộp theo bài và lớp |

### Dữ liệu U11 sở hữu

PostgreSQL `attempts` (gồm nội dung bài làm, `class_id`); Redis `ratelimit:attempt-save:*`; scanner tự nộp trong worker; không phát và không nghe event. Kết quả quiz và Practice ghi vào `evaluations` qua U15.

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  attempts/
    api/                AttemptController, StudentViewController, DTO, GzipRequestFilter
    application/        StudentViewService, AttemptStarter, DraftSaver, AttemptSubmitter,
                        AttemptQueryService, AttemptAccessGuard
    domain/             Attempt, AttemptStatus, SubmitMode, AttemptContent, AttemptSnapshot,
                        DeadlineCalculator
    infrastructure/     JPA repository, UnavailableCodeRunAdapter, UnavailablePracticeGradingAdapter,
                        NoopSubmissionSubmittedAdapter, NoopPracticeResultAdapter,
                        NoopGroupSubmissionQueryAdapter
    worker/             AttemptDeadlineScanner
    adapter/            AssignmentLifecycleAdapter (cài port của U08)
    port/               SubmissionQueryPort, AttemptRunResultPort, SubmissionSubmittedPort,
                        CodeRunPort, PracticeGradingPort, PracticeResultPort, GradeQueryPort
/backend/src/main/resources/db/migration/attempts/V20260925_1800__create_attempts.sql
/frontend/src/app/classes/assignments/                                  StudentAssignmentsPage
/frontend/src/app/classes/[id]/assignments/[assignmentId]/              AssignmentDetailPage
/frontend/src/app/classes/[id]/assignments/[assignmentId]/submissions/  SubmissionHistoryPage
/frontend/src/app/classes/quizzes/                                      QuizPracticeHistoryPage
/frontend/src/app/classes/[id]/quizzes/[quizId]/                        QuizPracticeDetailPage
/frontend/src/app/classes/[id]/attempts/[attemptId]/                    AttemptWorkspacePage
/frontend/src/app/classes/[id]/attempts/[attemptId]/result/             QuizResultPage
/frontend/src/shared/attempts/                  LessonQuizList
/frontend/src/features/attempt/useAutosave.ts
/contracts/openapi/attempts.yaml
/tests/load/attempt-autosave.js               (k6)
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - Biến cấu hình U11 theo `logical-components.md` §3; Nginx route lưu 12 MB, preview DOCX 20 MB.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain `Attempt` (có `classId`, `deadlineAt` rỗng được), `AttemptContent` (quiz và 3 dạng bài tập cá nhân), `AttemptSnapshot` (cài đặt quiz, seed), trạng thái chấm AI đọc từ U13, kết quả Practice qua `PracticeResultPort` (U15), `DeadlineCalculator` (BR-U11-06); port `SubmissionQueryPort` (`listByAssignment(assignmentId, classId)`), `AttemptRunResultPort`; port khai báo `SubmissionSubmittedPort`, `CodeRunPort`, `PracticeGradingPort`, `PracticeResultPort`, `GradeQueryPort`, `GroupSubmissionQueryPort` + adapter tạm.
- [ ] **Bước 3** - `AttemptAccessGuard` (R5: ghi danh `ACTIVE` lớp `OPEN`, bài của lớp/bài của môn chứa lớp/quiz của học liệu còn hiện; Admin, Teacher, Subject Manager bị từ chối; ngoài phạm vi `404`) và `AttemptStarter`: advisory lock, đếm lượt theo `maxAttempts` U08 (quiz có thể không giới hạn), snapshot dạng/chế độ bài, cài đặt quiz và seed, ghi `class_id`, tính `deadline_at` (F4, P1, BR-U11-01…06).
- [ ] **Bước 4** - Trả đề cho người học: góc nhìn đã lọc đáp án (`getStudentView`), quiz trộn theo seed khi cài đặt bật (F4 bước 4, BR-U11-53).
- [ ] **Bước 5** - `DraftSaver`: UPDATE có điều kiện, `409`/`410`, ân hạn 30 s, lượt không hạn, kiểm tài liệu, `GzipRequestFilter` giới hạn 10 MB, rate limit; preview DOCX chỉ cho lượt Diagram Essay đang làm, xác nhận thêm block qua cùng luồng lưu có `contentVersion` (F5, F5a, P2, P6, BR-U11-10…14, BR-U09-45…48).
- [ ] **Bước 6** - `AttemptSubmitter` một đường, idempotent, biên nhận; chỉ bài tập `GRADED` gọi `SubmissionSubmittedPort` trong transaction; quiz gọi `PracticeResultPort.scoreQuiz`, Practice Code Lab gọi `CodeRunPort.grade`; Practice Text/Diagram Essay không gọi AI khi nộp. Endpoint "Chấm với AI" (`POST /attempts/{id}/ai-grading`) kiểm chủ lượt, `SUBMITTED`, `PRACTICE` Text/Diagram Essay rồi gọi `PracticeGradingPort`; `GET /attempts/{id}/practice-result` đọc kết quả, quiz lọc điểm/đáp án theo `showScoreAfterSubmit`, `showCorrectAnswers` (F6, F8, F10, P3, BR-U11-20…24, 35, 54).
- [ ] **Bước 7** - `AttemptDeadlineScanner` (scanner U03: hết giờ quiz, hết hạn bài tập, ngưng giao; bỏ qua lượt `deadline_at` rỗng) và `AssignmentLifecycleAdapter.onRetired` (đặt hạn ngay, `AUTO_RETIRED`, gồm quiz bị ngưng hoặc có version mới) (F7, P3, P5).
- [ ] **Bước 8** - `StudentViewService`: Student Assignments gộp các lớp `OPEN` (bài của lớp và bài của môn, không gồm quiz, lọc lớp/dạng/trạng thái, lượt gần nhất, trạng thái nhóm qua U14), Assignment Detail (đề, khung, rubric, lịch, lượt còn lại), quiz của học liệu, Quiz Practice History gộp và Quiz Practice Detail (F1, F2, F3, P8, BR-U11-36, 37, 50…52).
- [ ] **Bước 9** - `AttemptQueryService`: Submission History, lượt được chấm (lượt nộp cuối của bài `GRADED`), Quiz Result, xuất DOCX, `SubmissionQueryPort` theo bài và lớp (F8, F9, F11, BR-U11-30…34, 38, 54).
- [ ] **Bước 10** - Audit theo BR-U11-40.
- [ ] **Bước 11** - Unit test mọi `BR-U11-xx`, gồm các mốc giờ quanh `deadlineAt`, quiz không giới hạn lượt/giờ, quiz ẩn khi học liệu lưu trữ, Admin/Teacher bị từ chối.
- [ ] **Bước 12** - Tóm tắt: `aidlc-docs/construction/u11-attempt-submission/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 13** - Flyway `db/migration/attempts/V20260925_1800__create_attempts.sql` theo `infrastructure-design.md` §3, bảng `attempts` theo [mô hình dữ liệu của unit](../../u11-attempt-submission/functional-design/domain-entities.md), cột `class_id`, `submit_mode`, `deadline_at` cho phép rỗng và trigger bất biến nội dung sau nộp.
- [ ] **Bước 14** - JPA repository.
- [ ] **Bước 15** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test: hai lần bắt đầu đồng thời; nộp tay và tự nộp đồng thời; ngừng giao và version mới của quiz tự nộp hàng loạt; quiz hết giờ tự nộp; nộp Practice không tự chấm; bấm "Chấm với AI" gọi `PracticeGradingPort` (adapter giả trả đủ/thiếu credit, quá 5 phút) và hiện đúng thông báo, lượt không phải Practice Text/Diagram Essay bị từ chối (luồng credit thật kiểm ở U13); bài `RETIRED` có điểm vẫn hiện; bài của môn hiện ở mọi lớp `OPEN` của môn và lượt ghi đúng `class_id`; lượt gần nhất và chuyển lượt ‹ ›; trigger chặn sửa sau nộp.
- [ ] **Bước 16** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 17** - `/contracts/openapi/attempts.yaml` (U11 không phát event); cần sửa theo mục 7.
- [ ] **Bước 18** - Controller + DTO + validation theo bảng API của `domain-entities.md` §7, gồm `POST /api/v1/attempts/{id}/docx:preview` chỉ cho chủ lượt Diagram Essay đang làm; xác nhận dùng `PUT /api/v1/attempts/{id}/content`.
- [ ] **Bước 19** - Test MockMvc: lượt người khác `404`; lọc lớp ngoài phạm vi `404`; Admin/Teacher bị từ chối; hết lượt/quá hạn bị từ chối; đề không chứa đáp án; Quiz Result chỉ trả đáp án khi `AFTER_SUBMIT`; "Chấm với AI" trên lượt của người khác hoặc bài `GRADED` bị từ chối.
- [ ] **Bước 20** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 21** - `StudentAssignmentsPage` (bộ lọc lớp/dạng/trạng thái, `?classId=` lọc sẵn, gắn `AssignmentGradeCell` U15 và `GradeDistributionBadge` U16), `AssignmentDetailPage` (`AssignmentInfo`, `QuestionView`, `SkeletonView`, `RubricView`, `LatestAttemptCard`, `StartAttemptButton`, `GroupWorkButton`).
- [ ] **Bước 22** - `QuizPracticeHistoryPage`, `QuizPracticeDetailPage`, `QuizResultPage`, `LessonQuizList` (để U05 gắn vào Learning Material).
- [ ] **Bước 23** - `AttemptWorkspacePage` với `QuizWorkspace` (Quiz Taking), `EssayWorkspace`, `DocumentWorkspace` (gồm `StudentDocxImportDialog`), `CodeWorkspace`, `AttemptHeader` (đồng hồ), `useAutosave` (P7), `SubmitConfirmDialog`, `ReceiptView`.
- [ ] **Bước 24** - `SubmissionHistoryPage` (`AttemptHistoryList`, `SubmittedAttemptView` với lượt gần nhất và nút ‹ ›, `PracticeResultPanel`, BR-U11-38), `PracticeAiGradingDialog` (UC 29, chỉ Student, mở từ Submission History; lỗi sau 5 phút).
- [ ] **Bước 25** - Test frontend: tự lưu không gửi chồng, `409` dừng tự lưu, hết giờ tự chuyển biên nhận/Quiz Result, lọc lớp giữ theo `?classId=`.
- [ ] **Bước 26** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 27** - Tải thử k6 100 người tự lưu 10 phút; ghi kết quả.
- [ ] **Bước 28** - Cập nhật `README.md`: luồng làm bài và quiz, tự nộp, cách U15 cài `SubmissionSubmittedPort`, `PracticeResultPort`, cách U05 gắn `LessonQuizList`.
- [ ] **Bước 29** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-003 S1 (UC 20, 24, 25, 26) | 3, 4, 6, 23 |
| US-ASM-003 S2 | 3, 6, 19 |
| US-ASM-003 S3 | 3, 9, 19 |
| US-ASM-003 S4 | 5, 23 |
| US-ASM-003 S5 (UC 28) | 9, 24 |
| US-ASM-003 S6 (UC 22, 23) | 8, 21 |
| UC 15, 18, 19, 21 (quiz luyện tập) | 8, 9, 22 |
| US-ASM-012 (Practice và AI theo credit, UC 29) | 2, 6, 9, 15, 18, 24; phối hợp U07/U13 |

## 5. Ngoài phạm vi

- Chạy code (U13), bài nhóm và Group Essay Workspace (U14), chấm, điểm và Teacher nhờ AI chấm (U15), thông báo và phân bố điểm (U16), màn Learning Material (U05).

## 6. Revision implementation scope - 2026-10-08

Mục này được thay bởi mục 7 (bản 73 UC).

- [ ] Năm danh sách Student và Assignment Detail/Submission History; component chung filter type cố định từ entry, không sửa UC IDs.
- [ ] Student AI UC 25 mở từ Assignment Detail cho own submitted Practice Essay/Diagram, Teacher AI UC 35 từ U15; component chung không gộp quyền.

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] Đánh số UC mới: 18–21 (quiz luyện tập), 22 (Student Assignments), 23 (Assignment Detail), 24–26 (Text Essay, Code Lab, Diagram), 28 (Submission History), 29 (chấm AI Practice); cập nhật `summary`/`description` trong `attempts.yaml`.
- [ ] Bỏ quiz `GRADED` và lịch quiz: quiz luôn `PRACTICE`, `deadline_at` = giờ bắt đầu + `timeLimitMinutes` hoặc rỗng; bỏ mọi xử lý `AFTER_CLOSE`; quiz nộp gọi `PracticeResultPort.scoreQuiz`, không gọi `SubmissionSubmittedPort`; `maxAttempts` rỗng = không giới hạn.
- [ ] Contract `attempts.yaml`: thay `GET /api/v1/classes/{classId}/my-assignments` bằng `GET /api/v1/me/assignments?classId=&type=&status=` (danh sách gộp, mỗi dòng có `classId`, `classCode`, `scope` lớp/môn, không còn `MULTIPLE_CHOICE_QUIZ` trong enum `type` của danh sách); `GET /api/v1/assignments/{assignmentId}/overview` thêm query `classId`, trả đề/khung/rubric, lượt còn lại, trạng thái nhóm, dùng chung cho quiz; `POST /api/v1/assignments/{assignmentId}/attempts` thêm body `classId`.
- [ ] Contract thêm: `GET /api/v1/me/quiz-attempts?classId=` (Quiz Practice History), `GET /api/v1/classes/{classId}/lessons/{lessonId}/quizzes` (`LessonQuizList`); chuyển `POST /api/v1/attempts/{attemptId}/docx:preview` từ `authoring.yaml` sang `attempts.yaml` (U11 kiểm quyền rồi gọi U09).
- [ ] Contract sửa: `practice-result` trả thêm phần quiz (điểm khi `showScoreAfterSubmit`, đáp án đúng/giải thích khi `AFTER_SUBMIT`) và trạng thái chấm AI; `export.docx` bỏ `ADMIN` (giữ `STUDENT` chủ lượt, `TEACHER`/`SUBJECT_MANAGER` giảng viên lớp của lượt); mọi API người học khác chỉ `x-roles: STUDENT`.
- [ ] Domain/migration: cột `attempts.class_id` (FK, index `(assignment_id, class_id, status)`), `deadline_at` cho phép NULL; đổi tên migration thành `db/migration/attempts/V20260925_1800__create_attempts.sql` (chưa áp dụng nên sửa trực tiếp).
- [ ] Code skeleton: `SubmissionQueryPort.listByAssignment(assignmentId, classId)` (thêm `classId`), `SubmissionView` thêm `classId`; `AssignmentLifecycleAdapter.onRetired` cài theo P5; khai báo `GroupSubmissionQueryPort` dùng của U14 (adapter rỗng).
- [ ] Màn theo screen flow: Student Assignments, Assignment Detail, Text Essay/Codelab/Diagram Essay Workspace, Submission History, Quiz Practice History, Quiz Practice Detail, Quiz Taking, Quiz Result; route `app/learning/...` đổi sang `app/classes/...`; đổi `MyAssignmentsPage` → `StudentAssignmentsPage`, `AssignmentOverviewPage` → `AssignmentDetailPage`, `SubmittedAttemptView` nằm trong `SubmissionHistoryPage`; bỏ năm danh sách theo loại.
- [ ] Nút "Chấm với AI" (UC 29) chuyển từ Assignment Detail sang Submission History; thay `GradeWithAiDialog` hai chế độ bằng `PracticeAiGradingDialog` chỉ cho Student; bỏ chế độ `TEACHER_PROPOSAL` (người dùng chốt 2026-10-09: U15 tự làm panel AI đề xuất chấm trong Grading Workspace cho UC 38).
- [ ] Ảnh hưởng unit khác (sửa ở lượt của unit đó): U04 `ClassDashboardPage` thêm lối vào Student Assignments; U05 `LearningMaterialPage` dùng API quiz của U11; U06 thêm U11 dùng `getVersion` cho Quiz Result; U13 bỏ ghi chú "popup Grade with AI của U11 theo screen flow"; U15 bỏ `GradeWithAiDialog (U11)`, tự làm panel AI đề xuất chấm trong Grading Workspace (UC 38); U14 thêm U11 vào người dùng `GroupSubmissionQueryPort`; U15 `AssignmentGradeCell` nhận `classId` theo dòng và `GET /api/v1/me/evaluations` cho `classId` tùy chọn, `QuizScorer` chỉ còn cho quiz luyện tập, dùng `listByAssignment(assignmentId, classId)`; U16 badge và nhắc hạn theo lớp của lượt.
