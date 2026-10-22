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

## Sửa sau chạy thử và link thông báo Quiz (2026-10-22) — chi tiết

Lập ngày 2026-10-22 theo mục "Làm được ngay" của [lộ trình](../cuong-uc-roadmap-2026-10-20.md), trên nhánh `quiz-ai-mcp` (`a27fdd2`, chưa push). Không thêm UC; sửa lỗi phát hiện khi chạy thử end-to-end (code-summary U10, U11) và khi U14 merge vào `staging` (`2fa57f9`).

### Lỗi cần sửa

| # | Lỗi | Unit |
|---|---|---|
| L1 | Thông báo "Có Quiz mới" của U14 dẫn tới `/quiz-practice/{quizId}` không có `classId`. Trang chi tiết gọi API với `classId` rỗng nên nhận `400` và báo lỗi | U11 |
| L2 | Bốn công tắc trong `QuizSettingsPanel` (Hiện điểm, Đảo thứ tự câu, Đảo thứ tự lựa chọn, Ghi nhận hoạt động) không có tên truy cập. `Switch` dùng chung đặt nhãn ngoài nút và các chỗ gọi không truyền `ariaLabel` | U10 |
| L3 | Ô chọn đáp án ở `QuizTakingPage` có tên truy cập "on" (theo cây truy cập lúc chạy thử) | U11 |
| L4 | Kết quả không cho biết lượt được tự nộp (`AUTO_TIME_LIMIT`, `AUTO_RETIRED`); API đã trả `submitMode` nhưng UI không hiện | U11 |

Không gồm điểm "kết quả khi Quiz ẩn điểm, ẩn đáp án" (BR-U11-20 khác API và UI): việc này cần người viết requirement chốt trước.

### Quyết định cần duyệt

| # | Nội dung | Đề xuất |
|---|---|---|
| J1 | Cách xử lý L1 | Chỉ sửa frontend U11, không đổi U14 hay API. Thiếu `classId` thì `QuizPracticeDetailPage` lấy `GET /api/v1/me/quizzes` và lọc theo `quizId`. Có đúng một lớp thì `router.replace` sang link có `classId`; nhiều lớp (Quiz của môn) thì hiện danh sách lớp để chọn; không có thì báo "Quiz không có trong lớp bạn đang học hoặc đã ngưng" |
| J2 | Cách xử lý L2 | Truyền `ariaLabel` ở bốn chỗ gọi trong U10, không sửa `Switch` trong `shared/ui` (file dùng chung). Ghi chú cho người giữ UI kit: `Switch` không có tên khi thiếu `ariaLabel` (còn 2 chỗ dùng ở unit khác) |
| J3 | Cách xử lý L3 | Viết test kiểm tên truy cập của ô chọn bằng nội dung lựa chọn. Nếu test cho thấy tên sai thì gắn `id` cho ô và `htmlFor` cho nhãn; nếu tên đúng thì chỉ giữ test và ghi lại rằng "on" là do công cụ đọc cây truy cập lúc chạy thử |
| J4 | Cách hiện L4 | `QuizResultPage`: dòng phụ dưới tiêu đề "Tự nộp khi hết giờ" hoặc "Tự nộp khi quiz bị ngưng". Phần giảng viên xem lượt (`TeacherQuizSections`) thêm cùng nhãn. Danh sách lượt ở Quiz Practice Detail chưa có `submitMode` trong API nên không đổi |
| J5 | Cập nhật nhánh | `git merge origin/staging` vào `quiz-ai-mcp`, không rebase, để giữ nguyên `cf80f2e`, `a27fdd2`. Xung đột duy nhất ở `aidlc-docs/aidlc-state.md`: giữ cả hai phía. Chạy lại test sau khi merge |
| J6 | Commit | Merge commit riêng (bước 5.0). Code sửa L1–L4 thành một commit local sau khi người dùng duyệt; chưa push (vẫn chờ sửa migration `grading`) |

### Checklist

- [ ] **5.0 Cập nhật nhánh** (J5): commit roadmap và audit đang chờ, merge `origin/staging`, giải xung đột `aidlc-state.md`, chạy test quiz backend và frontend.
- [ ] **5.1 L1** (J1): `QuizPracticeDetailPage` xử lý thiếu `classId`; trang `app/quiz-practice/[quizId]` truyền `classId` rỗng thành `null`.
- [ ] **5.2 L2** (J2): `ariaLabel` cho bốn công tắc của `QuizSettingsPanel`.
- [ ] **5.3 L3** (J3): test tên truy cập ô chọn ở `QuizTakingPage`; sửa nếu sai.
- [ ] **5.4 L4** (J4): nhãn tự nộp ở `QuizResultPage` và phần giảng viên xem lượt.
- [ ] **5.5 Unit test frontend:**
  - L1: một lớp chuyển link, nhiều lớp hiện chọn lớp, không có lớp báo lỗi.
  - L2: công tắc có tên.
  - L4: nhãn tự nộp hiện đúng theo `submitMode`.
- [ ] **5.6 Kiểm và tài liệu:**
  - Đủ bước CI: frontend lint, typecheck, test, build. Backend `mvn -B verify` vẫn có 4 lỗi Flyway của `grading` cho tới khi `staging` được sửa.
  - Cập nhật code-summary U10, U11; tick checklist; cập nhật lộ trình và `aidlc-state.md`.
- [ ] **5.7 Review:** người dùng duyệt rồi commit local.

## Integration test cases (tester viết)

- Hết giờ khi đang làm: scanner nộp và chấm đúng một lần.
- U10 phát hành version mới khi có lượt đang làm: lượt được nộp `AUTO_RETIRED`.
- Người học gọi API trước khi nộp không thấy đáp án.
- Phân quyền 4 role trên mọi endpoint, gồm dùng ID lượt của người khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
