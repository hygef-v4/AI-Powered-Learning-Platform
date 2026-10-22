# U11 Quiz Practice — code summary (giai đoạn 3)

Code viết ngày 2026-10-20 trên nhánh `quiz-practice` (tách từ `quiz-authoring` `f3b54bc`) theo [plan giai đoạn 3](../../plans/code-generation-plan/u11-quiz-practice-code-generation-plan.md) (G1–G7). Phạm vi: UC 49 (KAN-128), UC 50 (KAN-129), UC 51 (KAN-130), UC 52 (KAN-57), phần Quiz của UC 42; luồng F1–F7, F9. Chưa gồm tracker hoạt động phía trình duyệt (G3) và `MaterialQuizList` (G4). Chưa commit.

## Business logic

| Luồng | Class chính |
|---|---|
| F1 Lịch sử | `QuizPracticeQueryService.history` (lớp `OPEN` đang học qua `IClassAccess`, Quiz đang mở qua `IQuizQuery.listForClass`, cộng Quiz đã làm kể cả đã ngưng; điểm ẩn khi Quiz không hiện điểm) |
| F2 Chi tiết | `QuizPracticeQueryService.detail` (số lượt đã dùng/còn lại, điểm cao nhất, lý do không làm được) |
| F3 Bắt đầu | `QuizAttemptService.start` (R5, lớp `OPEN`, Quiz `OPEN` của lớp hoặc môn chứa lớp, khóa theo (Quiz, người học), mở lại lượt đang làm, hết lượt `409 NO_ATTEMPTS_LEFT` + audit `QUIZ_SUBMIT_REJECTED`, chụp snapshot có seed và thứ tự trộn — G5) |
| F4 Làm, tự lưu | `QuizAttemptService.get`, `.saveAnswers` (`answerVersion`, câu/lựa chọn phải thuộc đề, hạn + 30 giây ân hạn) |
| F5 Nộp, chấm | `QuizAttemptService.submit` → `PracticeSupport.submit` (chấm bằng `IQuizScoring`, nộp có điều kiện `IN_PROGRESS`, nộp lặp trả kết quả cũ, audit `QUIZ_ATTEMPT_SUBMITTED`) |
| F6 Tự nộp | `QuizAutoSubmitScanner` (`IScheduledScanner`, mỗi phút ở worker, `AUTO_TIME_LIMIT`), `QuizRetiredHandler` (`IQuizLifecycleListener`, `AUTO_RETIRED`, trong transaction của U10) |
| F7 Kết quả | `QuizAttemptService.result` → `PracticeSupport.result` (điểm và đáp án theo cài đặt đã chụp; đáp án qua `answerReview`) |
| F8 Hoạt động | `QuizActivityService` (chủ lượt, đang làm, chính sách đã chụp bật → `IActivityWrite`; G3) |
| F9 Giảng viên | `TeacherQuizAttemptService` (giảng viên của lớp; tóm tắt theo sinh viên, chi tiết lượt chỉ đọc kèm `IActivityQuery.summary`) |
| Quyền, audit | `PracticeAccess` (ngoài phạm vi `404` + `ACCESS_DENIED`, U01 lỗi `503`), `PracticeSupport.ownAttempt` (lượt người khác `404`) |

Domain: `QuizAttempt` (hạn, ân hạn 30 giây, lưu, nộp), `AttemptSnapshot` (version, cài đặt, theo dõi, seed, thứ tự câu và lựa chọn), `QuestionResult`, enum `AttemptStatus`, `SubmitMode`; `IQuizAttemptRepository`. Hạ tầng: `JdbcQuizAttemptRepository` (`jsonb`; advisory lock khi bắt đầu; `for update skip locked` cho scanner).

## API

| Endpoint | UC | Ghi chú |
|---|---|---|
| `GET /api/v1/me/quizzes?classId=` | 49 | Chỉ Student |
| `GET /api/v1/me/quizzes/{quizId}?classId=` | 50 | |
| `POST /api/v1/quizzes/{quizId}/attempts` | 51 | Body `{classId}`; trả đề của lượt (mới hoặc đang làm) |
| `GET /api/v1/quiz-attempts/{id}`, `PUT .../answers`, `POST .../submit` | 51 | `answers` là object câu → danh sách lựa chọn, kèm `answerVersion` |
| `GET /api/v1/quiz-attempts/{id}/result` | 52 | |
| `POST /api/v1/quiz-attempts/{id}/activity` | — | G3 |
| `GET /api/v1/classes/{classId}/quizzes/{quizId}/attempts`, `GET /api/v1/classes/{classId}/quiz-attempts/{id}` | 42 | Giảng viên của lớp |

Mã lỗi: `QUIZ_NOT_FOUND`, `QUIZ_ATTEMPT_NOT_FOUND`, `QUIZ_NOT_OPEN`, `NO_ATTEMPTS_LEFT`, `ANSWER_VERSION_CONFLICT`, `ANSWER_INVALID`, `ATTEMPT_TIME_UP`, `ATTEMPT_SUBMITTED`, `ATTEMPT_IN_PROGRESS`, `ACTIVITY_NOT_ENABLED`, `QUIZ_PRACTICE_STUDENT_ONLY`, `QUIZ_PRACTICE_REQUEST_INVALID`, `SYSTEM_BUSY`.

## Migration

`db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql`: `quiz_attempts` với unique `(quiz_id, account_id, attempt_no)`, unique riêng phần một lượt `IN_PROGRESS` mỗi (Quiz, người học), check trạng thái/nộp, index `(status, deadline_at)`, `(quiz_id, class_id)`, `(account_id, submitted_at)`, FK `quizzes`, `course_classes`, `accounts`; `app` không có `DELETE`.

## Frontend

- Route (layout chỉ Student): `app/quiz-practice` (#42), `app/quiz-practice/[quizId]?classId=` (#43), `app/quiz-attempts/[attemptId]` (#44), `app/quiz-attempts/[attemptId]/result` (#45).
- `features/quizpractice`: `QuizPracticeHistoryPage`, `QuizPracticeDetailPage`, `QuizTakingPage` (đồng hồ bù lệch giờ server, điều hướng câu, tự lưu 2 giây và khi rời trang, popup nộp, tự nộp khi hết giờ), `QuizResultPage`, `ResultQuestions`, `TeacherQuizSections` (`QuizAttemptsSection` gắn `quizDetailSections`, `QuizAttemptCount` gắn `quizRowActions` của U10).

## Unit test

- Backend: `QuizAttemptTest` 3, `QuizPracticeServicesTest` 11 (repository giả, đồng hồ chỉnh được), `QuizPracticeControllerTest` 3, `JdbcQuizAttemptRepositoryIntegrationTest` 4 (PostgreSQL 16 qua Testcontainers), thêm 1 test bảng quyền.
- Frontend: `QuizPracticeScreens.test.tsx` 10.
- Chạy đủ bước CI ngày 2026-10-20: backend `mvn -B verify` trên JDK 17 773 test, 0 lỗi, 31 bỏ qua (U04); frontend `npm ci`, lint, typecheck, 253 test, build đều đạt.

## Thay đổi file dùng chung

- `identity/contract/Actions.java`, `identity/application/AccessPolicy.java`: `QUIZ_PRACTICE` (Student, phạm vi lớp) (G1).

## Khác với thiết kế

- Chưa gắn `useActivityTracker` phía trình duyệt (U08 chưa có); API ghi hoạt động đã có (G3). Quiz hiện chưa bật được theo dõi (E3 của U10).
- `MaterialQuizList` và lọc `materialId` (G4) làm ở giai đoạn 4, xem mục cuối.
- Số lượt tính theo từng version Quiz (G6).
- Phần Lượt làm trên Quiz Detail chỉ hiện khi mở từ lớp (G7).
- Lượt bị từ chối khi bắt đầu (hết lượt, Quiz đã ngưng) ghi `QUIZ_SUBMIT_REJECTED` với lý do.

## Đối chiếu UC 49–52 (bước 3.14)

| Rule | Code | Test |
|---|---|---|
| BR-U11-01 chỉ Student đang học lớp | `QUIZ_PRACTICE`, `PracticeAccess`, layout Student | `onlyEnrolledStudentsOfTheQuizClassCanStart`, `quizPracticeIsStudentOnlyWithClassScope` |
| BR-U11-02 lịch sử gộp, Quiz đã ngưng có lượt | `history` | `historyShowsOpenQuizzesAndRetiredOnesWithOwnAttempts` |
| BR-U11-03 chi tiết, lý do không làm được | `detail` | `attemptsAreLimitedAndUnlimitedWhenNoMax`, `QuizPracticeDetailPage` test |
| BR-U11-10 tính lượt, một lượt đang làm | `start`, unique riêng phần, advisory lock | `startingCreatesOneAttempt…`, `onlyOneAttemptInProgressPerQuizAndStudent` |
| BR-U11-11 chụp, trộn theo seed | `AttemptSnapshot.of` | `sameSeedGivesSameShuffledOrder…` |
| BR-U11-12 tự lưu, `answerVersion`, ân hạn 30 giây | `saveAnswers`, `QuizTakingPage` | `savingChecksVersionQuestionsAndDeadlineWithGrace`, `tự lưu 2 giây…` |
| BR-U11-13 nộp tay, nộp lặp | `submit` | `submitScoresWithPinnedAnswers…` |
| BR-U11-14 tự nộp hết giờ, Quiz ngưng | `QuizAutoSubmitScanner`, `QuizRetiredHandler` | `scannerSubmitsOverdueAttemptsOnce`, `retiredQuizSubmitsAttemptsInProgress`, `hết giờ thì tự nộp` |
| BR-U11-15 chấm khi nộp, bất biến | `PracticeSupport.submit`, `QuizAttempt.submitted` | `savingBumpsVersionAndSubmittedAttemptIsFinal` |
| BR-U11-20 kết quả theo cài đặt | `PracticeSupport.result`, `QuizResultPage` | `resultFollowsSnapshotSettingsAndIsOwnOnly`, `QuizResultPage` tests |
| BR-U11-22 chỉ lượt của mình | `ownAttempt` | `resultFollowsSnapshotSettingsAndIsOwnOnly` |
| BR-U11-30 hoạt động theo chính sách đã chụp | `QuizActivityService` | `activityNeedsMonitoringEnabledInSnapshot` |
| BR-U11-31 giảng viên lớp xem, chỉ đọc | `TeacherQuizAttemptService`, `QuizAttemptsSection` | `teacherSeesClassStudentsAttemptsReadOnly`, `QuizAttemptsSection` test |
| BR-U11-40 audit | `PracticeAccess` | `submitScores…`, `attemptsAreLimited…` |
| BR-U11-41 `404` + `ACCESS_DENIED`, fail closed | `PracticeAccess` | `onlyEnrolledStudents…` |
| NFR-U11-30 không lộ đáp án trước khi nộp | `TakingView` không có đáp án | `startingCreatesOneAttempt…` |

## Chạy thử end-to-end (2026-10-20, stack Docker local)

Cùng lần chạy với U10 (xem code-summary U10: code `staging` `c7a4791`, compose local qua Nginx `:8088`, override tạm cổng PostgreSQL và healthcheck RabbitMQ). Quiz của lớp có 6 câu, tổng 7 điểm; v1: 10 phút, 3 lượt, hiện điểm, đáp án sau khi nộp, đảo câu và lựa chọn; v2: 1 phút, ẩn điểm, không hiện đáp án. Sinh viên 1 làm trên trình duyệt; sinh viên 2, 3 làm qua API (để frontend không tự nộp khi hết giờ); trạng thái lượt kiểm thêm trong PostgreSQL.

| Bước | Kết quả |
|---|---|
| s1 vào `/quiz-practice`: thấy Quiz, 0/3 lượt, bấm Bắt đầu | Đạt; đề đã đảo câu và lựa chọn, đồng hồ 10 phút |
| Tự lưu ~2 giây sau khi chọn (`answerVersion` 0 → 1 → 2); tải lại trang giữ đủ 6 câu | Đạt |
| Popup nộp, nộp tay; kết quả 5/7 đúng như dự tính (câu `MCQ_MULTI` chọn thiếu tính sai), đáp án và giải thích hiện theo `AFTER_SUBMIT` | Đạt |
| Nộp lặp → `200` trả kết quả cũ; lưu sau khi nộp → `409 ATTEMPT_SUBMITTED`; lịch sử 1/3 lượt, điểm cao nhất 5/7 | Đạt |
| s2 bắt đầu, lưu 5 câu, bắt đầu lại trả đúng lượt đang làm; giảng viên phát hành version mới | Đạt; lượt s2 `SUBMITTED`, `AUTO_RETIRED`, 4/7 đúng dự tính; kết quả của s2 vẫn theo snapshot v1 (hiện điểm, đáp án) |
| s1 sau khi v1 ngưng: lịch sử có v2 (Đang mở, 0/3) và v1 (Đã ngưng, 1/3, điểm 5); bắt đầu v1 → `409` | Đạt (số lượt tính theo version, G6) |
| s3 bắt đầu v2 (1 phút), lưu 5 câu; lưu ở hạn + 10 giây → `200`; ở hạn + 32 giây → `409` "Đã hết thời gian làm bài" | Đạt (ân hạn 30 giây) |
| Scanner ở worker tự nộp lượt quá hạn | Đạt; hạn 14:06:08Z, nộp 14:07:21Z, `AUTO_TIME_LIMIT`, điểm 4; gọi nộp sau đó trả kết quả cũ; sau vài chu kỳ quét vẫn một dòng `SUBMITTED` và đúng một audit `QUIZ_ATTEMPT_SUBMITTED`, log worker không có lỗi scanner |

Lỗi và điểm cần quyết định (chưa sửa):

1. **Kết quả khi Quiz ẩn điểm và đặt `NEVER` không khớp giữa BR, API và UI.** BR-U11-20 yêu cầu luôn hiện câu trả lời của mình từng câu và số câu đúng. API `GET /quiz-attempts/{id}/result` trả `correctCount`, `wrongCount`, `blankCount` và cờ `correct` từng câu (với câu một đáp án, cờ này lộ đáp án khi sinh viên chọn đúng). Còn `QuizResultPage` chỉ hiện "Đã nộp bài" và hai dòng "không hiện điểm/đáp án", không có câu trả lời của mình hay số câu đúng. Tái hiện: v2 ở trên, s3 mở `/quiz-attempts/{id}/result`. Cần chốt có hiện cờ đúng/sai từng câu khi `NEVER` hay không, rồi sửa một trong hai phía.
2. Nhỏ: ô chọn đáp án ở `QuizTakingPage` có tên truy cập "on" (nhãn không gắn với input); kết quả không cho biết lượt được tự nộp (`AUTO_TIME_LIMIT`, `AUTO_RETIRED`), chỉ hiện "Đã nộp bài".
3. Quan sát: "Lần gần nhất" ở lịch sử sinh viên lấy `startedAt` mới nhất, phía giảng viên lấy `submittedAt` (17:11 và 17:12 cho cùng một lượt); hai dòng của hai version trong lịch sử cùng tên, chỉ phân biệt bằng trạng thái.

## Còn lại

- Integration test (hết giờ khi đang làm, Quiz bị ngưng khi có lượt, hai tab lưu cùng lượt, phân quyền 4 role) do tester viết; chốt điểm 1 ở mục trên.
- Gắn tracker hoạt động khi U08 xong.

## G4 — Quiz gắn học liệu (giai đoạn 4, 2026-10-22)

Làm trên nhánh `quiz-ai-mcp` cùng giai đoạn 4 của U10, sau khi U13 có slot `LearningMaterialSection` trên `staging`.

- API: `GET /api/v1/me/quizzes?classId=&materialId=` (`materialId` cần kèm `classId`, không có thì `400`) → `QuizPracticeQueryService.byMaterial`: sinh viên của lớp đang mở; Quiz `OPEN` gắn học liệu qua `IQuizQuery.listByMaterial`; số lượt đã dùng, điểm cao nhất của mình.
- Frontend: `MaterialQuizList` đăng ký `learningMaterialSections`. Hiện "Practice · n phút · Đã dùng x / y lượt · Điểm cao nhất", nút Làm quiz/Làm tiếp; hết lượt thì báo; không phải sinh viên hoặc không có Quiz thì không hiện. Chưa có số câu trên dòng vì `QuizView` không có số câu.
- Test: `QuizPracticeServicesTest` (1 test), `QuizPracticeControllerTest` (thêm kiểm `materialId`), `MaterialQuizList.test.tsx` (3 test).
