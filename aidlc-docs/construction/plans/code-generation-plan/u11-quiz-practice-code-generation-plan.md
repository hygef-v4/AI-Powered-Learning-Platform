# U11 Quiz Practice — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Thiết kế: [business-logic-model](../../u11-quiz-practice/functional-design/business-logic-model.md), [business-rules](../../u11-quiz-practice/functional-design/business-rules.md), [domain-entities](../../u11-quiz-practice/functional-design/domain-entities.md), [frontend-components](../../u11-quiz-practice/functional-design/frontend-components.md), [NFR](../../u11-quiz-practice/nfr-requirements/nfr-requirements.md), [NFR design](../../u11-quiz-practice/nfr-design/nfr-design-patterns.md), [infrastructure](../../u11-quiz-practice/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/quizpractice/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../quizpractice/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/quizpractice/slots.ts` | Slot đã khai báo: `quizMaterialSections` |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `QuizAttempt` và enum; trộn có seed; repository chỉ dùng trong package.
- [ ] **Bước 3 — Xem Quiz:** `QuizPracticeQueryService` (F1, F2).
- [ ] **Bước 4 — Làm, nộp, chấm:** `QuizAttemptService` (F3–F5, F7, P1–P3).
- [ ] **Bước 5 — Tự nộp:** `QuizAutoSubmitScanner`, `QuizRetiredHandler` (F6).
- [ ] **Bước 6 — Hoạt động và giảng viên:** `QuizActivityService` (F8), `TeacherQuizAttemptService` (F9).
- [ ] **Bước 7 — API:** controller theo domain-entities mục 4; `contracts/openapi/quiz-practice.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 8 — Frontend:** `QuizPracticeHistoryPage` (đăng ký `NavEntry`), `QuizPracticeDetailPage`, `QuizTakingPage`, `QuizResultPage`, `MaterialQuizList` (slot của U13), `QuizAttemptsSection`, `QuizAttemptCount` (slot của U10); khai báo slot `QuizMaterialSection` cho U13.
- [ ] **Bước 9 — Unit test (người code viết):** điều kiện bắt đầu (ghi danh, Quiz mở, hết lượt, không giới hạn lượt); một lượt đang làm; trộn theo seed ổn định; lưu sau giờ và trong 30 giây ân hạn; nộp lặp; chấm `MCQ_MULTI`; tự nộp khi hết giờ và khi Quiz bị ngưng; kết quả theo cài đặt hiện điểm và đáp án; giảng viên lớp khác bị từ chối; component frontend. Integration test do tester viết.
- [ ] **Bước 10 — Review:** đối chiếu UC 49–52, story, BR-U11 với code; người dùng duyệt trước khi chuyển unit.

## Giai đoạn 3 — Quiz Practice (UC 49–52) — chi tiết

Lập ngày 2026-10-20 sau khi U10 giai đoạn 2 được duyệt (`f3b54bc`) theo [lộ trình](../cuong-uc-roadmap-2026-10-20.md). Jira: KAN-128 (UC 49), KAN-129 (UC 50), KAN-130 (UC 51), KAN-57 (UC 52). Story: US-ASM-003 (phần Quiz). Luồng F1–F7, F9; BR-U11-01…22, 31, 40, 41; NFR-U11-01…03, 10, 11, 30, 31. Không gồm F8 ghi sự kiện hoạt động phía trình duyệt (xem G3) và `MaterialQuizList` (xem G4).

### Dùng của unit khác

| Contract | Unit | Tình trạng | Dùng để |
|---|---|---|---|
| `IQuizQuery`, `IQuizScoring`, `IQuizLifecycleListener` | U10 | Đã cài (`f3b54bc`) | Quiz người học thấy, đề không có đáp án, chấm, đáp án sau khi nộp; tự nộp khi Quiz bị ngưng |
| `IAuthorization` | U01 | Có | R5 (sinh viên đang học lớp), R3/R4 (giảng viên lớp) |
| `IClassAccess` | U05 | Có | Lớp `OPEN` đang học, ghi danh, môn của lớp |
| `IScheduledScanner` | U03 | Có (chạy ở profile `worker`) | Tự nộp khi hết giờ |
| `IAudit` | U02 | Có | `QUIZ_ATTEMPT_SUBMITTED`, `QUIZ_SUBMIT_REJECTED`, `ACCESS_DENIED` |
| `IActivityWrite`, `IActivityQuery` | U08 | Stub | Dữ liệu rời trang (G3) |
| Slot `quizDetailSections`, `quizRowActions` | U10 (frontend) | Có | Phần Lượt làm trên Quiz Detail, số sinh viên đã làm trên dòng Quiz |
| Slot `LearningMaterialSection` | U13 | Chưa có | Quiz gắn học liệu (G4) |

### Quyết định cần duyệt

| # | Nội dung | Đề xuất |
|---|---|---|
| G1 | Action mới trong file dùng chung của U01 | `QUIZ_PRACTICE`: role `STUDENT`, phạm vi lớp (sinh viên đang học, R5). Giảng viên xem lượt dùng lại `QUIZ_CLASS_MANAGE` |
| G2 | Nhánh | Tạo nhánh `quiz-practice` từ `quiz-authoring` (`f3b54bc`), vì U11 dùng code U10 chưa merge |
| G3 | Ghi nhận hoạt động: U08 chưa có `useActivityTracker`, Quiz chưa bật được theo dõi (E3 của U10) | Làm API `POST /quiz-attempts/{id}/activity` (kiểm chủ lượt, `IN_PROGRESS`, chính sách đã chụp) và gọi `IActivityQuery` cho phần giảng viên; chưa gắn tracker phía trình duyệt, làm khi U08 có |
| G4 | U13 chưa có slot `LearningMaterialSection` | Chưa làm `MaterialQuizList` và lọc `materialId`; giữ slot `quizMaterialSections` đã khai báo |
| G5 | Trộn câu, trộn lựa chọn | Lúc bắt đầu tính thứ tự câu và thứ tự lựa chọn từng câu từ seed rồi lưu vào `snapshot`; đề trả về theo thứ tự đó |
| G6 | Số lượt | Đếm theo từng version Quiz (mỗi version là một Quiz riêng); version mới thì lượt tính lại từ 0 |
| G7 | Giảng viên xem lượt | Theo lớp (`classId`): Quiz của lớp, hoặc Quiz của môn chứa lớp; Chủ nhiệm môn mở Quiz của môn không kèm lớp thì không có phần Lượt làm |

### Checklist giai đoạn 3

- [x] **3.0 Nhánh** — tạo `quiz-practice` từ `quiz-authoring` (G2).
- [x] **3.1 Migration** — `backend/src/main/resources/db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql` theo infrastructure-design (unique `(quiz_id, account_id, attempt_no)`, unique riêng phần một lượt `IN_PROGRESS`, index `(status, deadline_at)`, `(quiz_id, class_id)`, `(account_id, submitted_at)`, FK; `app` không có `DELETE`).
- [x] **3.2 Quyền** — G1 trong `Actions`, `AccessPolicy` + test bảng quyền; `PracticeAccess` (ngoài phạm vi `404` + `ACCESS_DENIED`, U01/U05/U10 lỗi → `503`).
- [x] **3.3 Domain** — `quizpractice/domain`: `QuizAttempt` (trạng thái, lưu câu trả lời có `answerVersion`, hạn nộp + 30 giây ân hạn, nộp), `AttemptSnapshot` (version, cài đặt, chính sách theo dõi, seed, thứ tự câu/lựa chọn — G5), enum `AttemptStatus`, `SubmitMode`; `IQuizAttemptRepository`.
- [x] **3.4 Repository** — `JdbcQuizAttemptRepository` (`snapshot`, `answers`, `result` dạng `jsonb`; khóa hàng theo (Quiz, người học) khi bắt đầu; nộp là câu lệnh có điều kiện `IN_PROGRESS`; tìm lượt quá hạn cho scanner).
- [x] **3.5 Xem Quiz** — `QuizPracticeQueryService` (F1, F2): lịch sử gộp các lớp `OPEN` đang học (Quiz đang mở + Quiz đã làm, kể cả đã ngưng), chi tiết Quiz (số lượt đã dùng/còn lại, lý do không làm được).
- [x] **3.6 Làm, lưu, nộp, chấm** — `QuizAttemptService` (F3–F5, F7): bắt đầu hoặc mở lại lượt đang làm (BR-U11-10, 11), đề theo snapshot không có đáp án, tự lưu (BR-U11-12), nộp tay (BR-U11-13), chấm bằng `IQuizScoring` (BR-U11-15), kết quả theo cài đặt (BR-U11-20); audit.
- [x] **3.7 Tự nộp** — `QuizAutoSubmitScanner` (cài `IScheduledScanner`, `AUTO_TIME_LIMIT`), `QuizRetiredHandler` (cài `IQuizLifecycleListener`, `AUTO_RETIRED`, trong transaction của U10); chạy lại không nộp hai lần.
- [x] **3.8 Giảng viên và hoạt động** — `TeacherQuizAttemptService` (F9: tóm tắt theo sinh viên của lớp, chi tiết một lượt chỉ đọc kèm `IActivityQuery.summary`, số sinh viên đã làm), `QuizActivityService` (G3).
- [x] **3.9 API** — `QuizPracticeController` (`/me/quizzes`, `/me/quizzes/{quizId}`, `/quizzes/{quizId}/attempts`, `/quiz-attempts/{id}`, `/answers`, `/submit`, `/result`, `/activity`), `TeacherQuizAttemptController` (`/classes/{classId}/quizzes/{quizId}/attempts`, `/classes/{classId}/quiz-attempts/{attemptId}`); exception handler riêng của U11.
- [x] **3.10 Unit test backend** — điều kiện bắt đầu (ghi danh, Quiz mở, hết lượt, không giới hạn lượt); một lượt đang làm; trộn theo seed ổn định; lưu sau giờ và trong 30 giây ân hạn; `answerVersion` lệch `409`; câu/lựa chọn không thuộc đề; nộp lặp; chấm `MCQ_MULTI`; tự nộp khi hết giờ và khi Quiz bị ngưng (một lần); kết quả ẩn điểm/đáp án theo cài đặt; người khác xem lượt `404`; giảng viên lớp khác `404`; MockMvc; repository trên PostgreSQL (Testcontainers).
- [x] **3.11 Frontend** — `features/quizpractice`: `QuizPracticeHistoryPage` (`/quiz-practice`), `QuizPracticeDetailPage` (`/quiz-practice/[quizId]?classId=`), `QuizTakingPage` (`/quiz-attempts/[attemptId]`: đồng hồ, điều hướng câu, tự lưu 2 giây, `SubmitQuizDialog`, `TimeUpOverlay`), `QuizResultPage` (`/quiz-attempts/[attemptId]/result`); `QuizAttemptsSection` gắn `quizDetailSections`, `QuizAttemptCount` gắn `quizRowActions`; layout chỉ Student; `data-testid`.
- [x] **3.12 Unit test frontend** — lịch sử và nút Bắt đầu/Tiếp tục; hết lượt nêu lý do; tự lưu sau 2 giây kèm `answerVersion`, `409` tải lại; popup nộp đếm câu chưa trả lời; hết giờ tự nộp; kết quả không hiện điểm/đáp án khi Quiz tắt; phần Lượt làm của giảng viên.
- [x] **3.13 Kiểm và tài liệu** — đủ bước CI; `aidlc-docs/construction/u11-quiz-practice/code/code-summary.md`; tick checklist; `aidlc-state.md`.
- [x] **3.14 Review** — đối chiếu UC 49–52, US-ASM-003, BR-U11 với code; người dùng duyệt.

## Integration test cases (tester viết)

- Hết giờ khi đang làm: scanner nộp và chấm đúng một lần.
- U10 phát hành version mới khi có lượt đang làm: lượt được nộp `AUTO_RETIRED`.
- Người học gọi API trước khi nộp không thấy đáp án.
- Phân quyền 4 role trên mọi endpoint, gồm dùng ID lượt của người khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
