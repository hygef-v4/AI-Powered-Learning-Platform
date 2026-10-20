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
- Chưa có `MaterialQuizList` và lọc `materialId` (U13 chưa có) (G4).
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

## Còn lại

- Chưa chạy end-to-end trên trình duyệt với backend thật; integration test (hết giờ khi đang làm, Quiz bị ngưng khi có lượt, hai tab lưu cùng lượt, phân quyền 4 role) do tester viết.
- Gắn tracker hoạt động khi U08 xong; `MaterialQuizList` khi U13 xong.
