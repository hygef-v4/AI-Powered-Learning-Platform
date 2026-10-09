# U08 Assessment Core & Publication - Code Generation Plan

**Bản tài liệu 2026-10-09**: UC 41 và vòng đời (tạo, xóa, duyệt, phát hành, lịch, ngưng giao, nhân bản, version) của UC 35, 42, 43, 44, 45 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-ASM-001, US-ASM-008. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

**Phê duyệt 2026-10-05 là baseline trước revision**; checklist triển khai mới chưa hoàn thành, DTO/contracts/code cần rà theo thiết kế hiện hành.

> Plan này là nguồn duy nhất cho Code Generation của U08. Mỗi bước xong thì đánh `[x]` ngay.
>
> **Đã duyệt 2026-10-05** (người dùng duyệt cả 16 plan): bắt đầu Part 2 (sinh code) theo thứ tự wave.
>
> Quyết định 2026-10-05: code xong unit **không viết integration test** (Testcontainers, kiểm đầu-cuối nhiều thành phần); tester riêng đảm nhận. Unit chỉ viết unit test (và test MockMvc/frontend nếu có trong plan). Bước integration test bên dưới giữ kịch bản để chuyển cho tester.

## 1. Bối cảnh

- **Story**: US-ASM-001, US-ASM-008, US-ASM-010 (copy bài giữa lớp, gộp từ U10 đã xóa); khóa nội dung cho US-QBK-002 S2, S3.
- **Primary UC hiện hành**: UC 41 và phần vòng đời của UC 35, 42–45 theo bản 73 UC. Supporting flows theo current-srs-contract.md.
- **Quyết định 2026-10-09**: có bài của lớp (Teacher), bài của môn (Chủ nhiệm môn, giao cho mọi lớp của môn với một lịch chung, không có bài nhóm, giảng viên từng lớp chấm) và quiz luyện tập gắn học liệu (không lịch, không vào sổ điểm); giữ bước Duyệt; phát hành khóa rubric và chặn khi rubric trống; quiz, Text Essay, Code Lab dùng câu từ ngân hàng của môn hoặc câu riêng.
- **Thiết kế nguồn**: `construction/u08-assessment-core-publication/` (functional-design, nfr-requirements, nfr-design, infrastructure-design).
- **Stack**: như U01 — Maven + Java 17 + Spring Boot 3.x; Next.js + TypeScript + npm + Tailwind, component tự viết.
- **Code nằm ở workspace root**, không trong `aidlc-docs/`.

### Khung dự án dùng chung

Khung dự án là **Bước K1-K6 của plan U03** (U03 code đầu tiên). Bước 0 kiểm điều kiện này.

### Phụ thuộc

| Port | Unit thật | Xử lý lượt này |
|---|---|---|
| `AuthorizationPort` | U01 | Dùng thật |
| `AuditPort` | U02 | Dùng thật |
| `ScheduledScanner`, `EventPublisherPort` | U03 | Dùng thật |
| `ClassAccessPort`, `SubjectScopePort`, `ClassScopePort` | U04 | Dùng thật |
| `ContentRefPort` | U05 | Dùng thật (`getLessonRef` cho học liệu của quiz; bộ lọc chọn ngẫu nhiên) |
| `BankQueryPort`, `InlineQuestionPort`, `RubricPort`, `QuestionEditor`, `QuestionPicker`, `QuestionView` | U06 | Dùng thật |
| `TypeConfigPort` | U09 (`C`) | Adapter tạm luôn đạt; U09 thay |
| `GroupReadinessPort` | U12 (`C`) | Dùng thật: U12 code trước U08 trong wave 2 |
| `CodeLabCheckPort` | U13 (`C`) | Adapter tạm luôn đạt; U13 thay |
| `AssignmentLifecyclePort` | U11, U14 (`C`) | Adapter rỗng; U11, U14 (wave 3) thay |
| `AiDraftPort` | U13 (`C`) | Adapter tạm báo "AI chưa sẵn sàng"; ẩn nút AI khi chưa có; U13 thay |

### Dữ liệu U08 sở hữu

PostgreSQL `assignments` (bài của lớp, bài của môn, quiz; gồm lịch, `lesson_id`), `assignment_questions` (quiz, Text Essay, Code Lab); FK `rubrics.assignment_id`; scanner mở/đóng theo lịch trong worker; event `assignment.opened` (chỉ cho thông báo U16).

## 2. Cấu trúc

```
/backend/src/main/java/edu/aiplatform/
  assessments/
    api/                AssignmentController, QuizController, ScheduleController,
                        StudentAssignmentController, DTO
    application/        AssignmentService, QuizService, ReviewValidator, ScheduleService,
                        AssignmentCopier, AssignmentQueryService, StudentViewMapper,
                        AssignmentScopeGuard
    domain/             Assignment, AssignmentQuestion, trạng thái, SubmissionWindow
    infrastructure/     JPA repository, PassTypeConfigCheckAdapter, PassCodeLabCheckAdapter,
                        UnavailableAiDraftAdapter, NoopLifecycleAdapter, GroupReadinessAdapter (gọi U12)
    worker/             AssignmentScheduleScanner
    port/               AssignmentQueryPort, AssignmentExtensionPort,
                        TypeConfigPort, AiDraftPort, AssignmentLifecyclePort,
                        GroupReadinessPort, CodeLabCheckPort
/backend/src/main/resources/db/migration/assessments/
/frontend/src/app/classes/[id]/teaching/assignments/
/frontend/src/app/classes/[id]/teaching/quizzes/
/frontend/src/app/manager/assignments/
/frontend/src/app/manager/quizzes/
/frontend/src/shared/assessments/     EvalsTab, AssignmentFormPage, QuizDetailPage (dùng chung cho lớp và môn)
/contracts/openapi/assessments.yaml
/contracts/messages/assignment-events.json
```

## 3. Các bước

### Nhóm A - Khung

- [ ] **Bước 0** - Kiểm khung dự án (plan U03 Bước K1-K6) đã có.
- [ ] **Bước 1** - Biến cấu hình U08 theo `logical-components.md` §3; `TZ=UTC` cho `backend`/`worker`.

### Nhóm B - Domain và logic

- [ ] **Bước 2** - Domain: `Assignment` (aggregate; bài của lớp hoặc của môn, quiz có `lesson_id`; khóa từ `SCHEDULED`/`OPEN`; gồm lịch), `AssignmentQuestion` (quiz, Text Essay, Code Lab), `SubmissionWindow` (P1, P3, BR-U08-10…15, 33, 60…62).
- [ ] **Bước 3** - Port và adapter: `AssignmentQueryPort` (`listForClass` gồm bài của môn, `listQuizzesOfLesson`, `isSubmissionOpen`), `AssignmentExtensionPort`; port khai báo `TypeConfigPort` (luôn đạt), `CodeLabCheckPort` (luôn đạt), `AiDraftPort` (báo chưa sẵn sàng), `AssignmentLifecyclePort` (rỗng), `GroupReadinessPort` (cắm U12).
- [ ] **Bước 4** - `AssignmentScopeGuard` (R3/R4 bài của lớp, R2 bài của môn, giảng viên lớp chỉ đọc bài của môn, Admin bị từ chối) và `AssignmentService`: tạo bài của lớp hoặc của môn (chọn dạng và chế độ trước, không có bài nhóm ở cấp môn), thêm câu Text Essay/Code Lab (chọn tay, ngẫu nhiên, câu riêng, AI; Text Essay gọi `TypeConfigPort.syncQuestionRubrics`), sửa, xóa bài chưa từng phát hành, nhân bản, tạo version mới (sao cấu hình và rubric qua `TypeConfigPort.copy`), audit (F1–F3, F7, BR-U08-01, 02, 10…17, 41…44).
- [ ] **Bước 5** - `QuizService`: tạo quiz gắn học liệu (kiểm `ContentRefPort.getLessonRef`), thêm câu chọn tay, chọn ngẫu nhiên (`BankQueryPort.pickRandom`), câu riêng (`InlineQuestionPort`), AI soạn câu (`AiDraftPort`), phát hành ngay và tự `RETIRED` version cũ, ngưng (F9, F10, BR-U08-18, 21, 60…64).
- [ ] **Bước 6** - `ReviewValidator` (câu quiz, điểm, học liệu còn `ACTIVE`; nội dung dạng bài và rubric đã điền qua `TypeConfigPort`; `CodeLabCheckPort`) và duyệt (F4, P5, BR-U08-20, 22).
- [ ] **Bước 7** - `ScheduleService`: phát hành bài tập (ghi lịch, kiểm lịch/nộp trễ/số lượt, bài của lớp cần lớp `OPEN`, bài của môn cần môn `ACTIVE`, bài nhóm hỏi `GroupReadinessPort`, gọi `RubricPort.lockForAssignment`, khóa bài), sửa lịch, ngưng giao gọi `onRetired` trong transaction, audit (F5, F7, P7, BR-U08-02, 30…34, 40).
- [ ] **Bước 8** - `AssignmentScheduleScanner` đăng ký với U03 (UPDATE có điều kiện mỗi phút; mở bài gọi `AssignmentLifecyclePort.onOpened` trong transaction rồi phát `assignment.opened` kèm `classId` hoặc `subjectId` sau commit; đóng bài tập theo `closes_at`/`late_until`, quiz không đóng) (F6, P2, P6, BR-U08-35, 36).
- [ ] **Bước 9** - `AssignmentQueryService`, `StudentViewMapper` và `AssignmentExtensionService` (U09 ghi `config` khi bài `DRAFT`, U16 ghi `reminder_sent_at`) (F8, P3, P4, BR-U08-03, 63).
- [ ] **Bước 10** - Unit test mọi `BR-U08-xx`: ranh giới `closes_at`/`late_until`; bài đã phát hành không sửa được; phát hành bị chặn khi rubric trống; bài của môn hiện ở mọi lớp `OPEN` của môn và giảng viên lớp không sửa được; quiz không lịch, không vào danh sách bài, ẩn khi học liệu bị lưu trữ; phát hành version mới của quiz retire version cũ; chọn ngẫu nhiên không trùng câu đã có.
- [ ] **Bước 11** - Tóm tắt: `aidlc-docs/construction/u08-assessment-core-publication/code/business-logic-summary.md`.

### Nhóm C - Dữ liệu

- [ ] **Bước 12** - Flyway `db/migration/assessments/V20260925_1500__create_assignments.sql` theo `infrastructure-design.md` §2 (gồm FK `rubrics.assignment_id`).
- [ ] **Bước 13** - JPA repository.
- [ ] **Bước 14** - **Không làm (tester riêng); kịch bản chuyển cho tester:** Integration test Testcontainers: scanner mở/đóng chạy lặp, đổi lịch rồi quét, phát hành bài đã phát hành bị chặn, hai người sửa cùng bài chạy lần lượt (khóa dòng), event gửi sau commit, phát hành rollback khi rubric trống.
- [ ] **Bước 15** - Tóm tắt: `code/repository-summary.md`.

### Nhóm D - API

- [x] **Bước 16** - `/contracts/openapi/assessments.yaml` và schema event; cần sửa theo mục 7.
- [ ] **Bước 17** - Controller + DTO + validation (giờ nhận ISO 8601 có offset).
- [ ] **Bước 18** - Test MockMvc: phát hành sai phạm vi bị từ chối và audit; giảng viên không sửa/phát hành bài của môn; Admin bị từ chối; người học không thấy đáp án, không thấy bài `SCHEDULED`/`RETIRED` và không thấy quiz trong danh sách bài; sửa bài đã phát hành trả `409`.
- [ ] **Bước 19** - Tóm tắt: `code/api-summary.md`.

### Nhóm E - Frontend

- [ ] **Bước 20** - `EvalsTab` (`AssignmentSection`, `QuizSection`, `CreateAssignmentDialog` có `CopyFromClassDialog`, `CreateQuizDialog`) gắn vào `TeacherClassPage` của U04; `SubjectAssignmentListPage`, `SubjectQuizListPage` dưới `/manager`.
- [ ] **Bước 21** - `AssignmentFormPage` (`AssignmentHeader`, `InstructionsEditor`, `AssignmentQuestionList` (Text Essay, Code Lab), `TypeConfigSlot` cho U09, `PreviewDialog`, `ReviewButton`, `PublishDialog`, `ScheduleSection`, `DeleteButton`, `CloneButton`, `NewVersionButton`; chế độ chỉ đọc cho giảng viên mở bài của môn).
- [ ] **Bước 22** - `QuizDetailPage` (`QuizHeader`, `QuizQuestionList` với `QuestionPicker`/`QuestionEditor` của U06, `AiDraftDialog`, `QuizSettingsSlot` cho U09, `PublishQuizButton`, `RetireButton`).
- [ ] **Bước 23** - Test frontend: cảnh báo khóa khi phát hành, danh sách rubric trống khi phát hành bị từ chối, kiểm lịch phía client, giờ hiển thị Việt Nam.
- [ ] **Bước 24** - Tóm tắt: `code/frontend-summary.md`.

### Nhóm F - Hoàn tất

- [ ] **Bước 25** - Cập nhật `README.md`: vòng đời bài và quiz, bài của môn, lịch mở/đóng, cách U09 cài `TypeConfigPort`, cách U11 dùng `isSubmissionOpen` và `listQuizzesOfLesson`.
- [ ] **Bước 26** - Chạy toàn bộ test, ghi `code/test-results.md`.

## 4. Truy vết

| Nguồn | Bước |
|---|---|
| US-ASM-001 S1, S2 (UC 42–45) | 4, 6, 7, 8, 21 |
| US-ASM-001 S3 (UC 41) | 9, 20 |
| US-ASM-008 (nhân bản, version, ngưng giao) | 4, 7, 21, 22 |
| US-ASM-010 (copy bài giữa lớp) | 8a, 20 |
| UC 35 (quiz) | 5, 6, 22 |
| US-QBK-002 S2, S3 (khóa nội dung) | 2, 4, 10, 18 |
| FR-006 (AI soạn câu quiz) | 5, 22 |

## 5. Ngoài phạm vi

- Nội dung riêng từng dạng bài và cài đặt quiz (U09); lượt làm (U11); nhóm (U12); AI thật (U13); chấm và công bố điểm (U15). Simulation Exam đã rút.

## 6. Revision implementation scope - 2026-10-08
- [ ] R3/R4 cho danh sách bài và CRUD/lifecycle bài của lớp; Admin không đọc/sửa bài.
- [ ] Giữ supporting review/publish/version/copy.

## 7. Revision theo bản 73 UC - 2026-10-09

- [ ] Bài của môn: dòng `assignments` có `subject_id` là bài giao cho mọi lớp của môn (không còn là template của U10); API `GET`, `POST /api/v1/subjects/{subjectId}/assignments`; không có `GROUP_ASSIGNMENT`; `listForClass` gồm bài của môn; event `assignment.opened` có `subjectId`.
- [ ] Quiz luyện tập: cột `assignments.lesson_id`; quiz chỉ `PRACTICE`, không lịch, phát hành → `OPEN`; API `GET`, `POST /api/v1/classes/{classId}/quizzes`, `/api/v1/subjects/{subjectId}/quizzes`; `listQuizzesOfLesson` cho U11; danh sách bài tập không gồm quiz.
- [ ] Câu ở `assignment_questions` cho quiz, Text Essay, Code Lab (ngân hàng dùng cho mọi dạng bài, người dùng chốt lại 2026-10-09); thêm/bỏ câu Text Essay gọi `TypeConfigPort.syncQuestionRubrics`.
- [ ] Rubric: bỏ cài `RubricOwnerPort` và `TypeConfigPort.repointRubric`; phát hành gọi `RubricPort.lockForAssignment`, rubric trống → `422` kèm danh sách; FK `rubrics.assignment_id`.
- [ ] `contracts/messages/assignment-events.json`: thêm `subjectId` (khi là bài/quiz của môn) và `type`; `classId` không còn bắt buộc.
- [ ] Phát hành bài nhóm: body thêm `confirmUngroupedStudents`; `PublishDialog` nhúng `GroupReadinessPanel` của U12. `EvalsTab` có nút "Bài nộp" (Submission Detail của U15) và mục "Sổ điểm" (`GradebookPanel` của U15). `CodeLabCheckPort.check` theo `contentHash` của câu `CODE` (U13 lưu).
- [ ] Thêm `DELETE /api/v1/assignments/{id}` (xóa bài chưa từng phát hành); `x-roles` bỏ `ADMIN`.
- [ ] Màn theo screen flow: tab Evals của Teacher Class Detail, Assignment List và Quiz List dưới Manager Dashboard, Assignment Form, Quiz Detail; route `app/teaching/...` đổi sang `app/classes/[id]/teaching/...` và `app/manager/...`.
- [ ] Ảnh hưởng unit khác (sửa ở lượt của unit đó): U10 bị xóa: template môn thay bằng bài của môn, copy giữa lớp gộp vào U08 (`AssignmentCopier`, API `copy-sources`, `assignments:copy-from-class` chuyển sang `assessments.yaml`, bỏ `contracts/openapi/templates.yaml`), bỏ màn so sánh version; U11 hiện quiz theo học liệu và Quiz Practice, Student Assignments gồm bài của môn; U15 công bố điểm theo (bài, lớp), suy từ `evaluations` (bỏ cột `assignments.grades_released_at`), sổ điểm không gồm quiz; U16 gửi thông báo bài của môn tới mọi lớp `OPEN` của môn.
