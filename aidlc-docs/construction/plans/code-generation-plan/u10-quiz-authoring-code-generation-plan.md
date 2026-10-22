# U10 Quiz Authoring — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U10 Quiz Authoring thuộc Iter 2, người code: Người 1; UC 46–48. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Thiết kế: [business-logic-model](../../u10-quiz-authoring/functional-design/business-logic-model.md), [business-rules](../../u10-quiz-authoring/functional-design/business-rules.md), [domain-entities](../../u10-quiz-authoring/functional-design/domain-entities.md), [frontend-components](../../u10-quiz-authoring/functional-design/frontend-components.md), [NFR](../../u10-quiz-authoring/nfr-requirements/nfr-requirements.md), [NFR design](../../u10-quiz-authoring/nfr-design/nfr-design-patterns.md), [infrastructure](../../u10-quiz-authoring/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/quizzes/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../quizzes/contract/` | interface `IQuizLifecycleListener`, `IQuizQuery`, `IQuizScoring`; kiểu dữ liệu `AnswerKey`, `AnswerReview`, `QuestionScore`, `QuestionType`, `QuizAnswer`, `QuizOptionView`, `QuizQuestionView`, `QuizScope`, `QuizScore`, `QuizSettings`, `QuizStatus`, `QuizStudentView`, `QuizView` |
| `backend/.../quizzes/contract/stub/QuizzesContractStubs` | Bản tạm trả dữ liệu mẫu cho `IQuizQuery`, `IQuizScoring` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/quizzes/slots.ts` | Slot đã khai báo: `quizDetailSections` |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/quizzes/V20260925_1600__create_quiz_tables.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `Question`, `Quiz`, `QuizItem`, `QuizAiDraft` và enum; kiểm chuyển trạng thái; repository chỉ dùng trong package.
- [ ] **Bước 3 — Ngân hàng:** `QuestionBankService` (F1, F2, P1), `QuestionImporter` (F3, P5).
- [ ] **Bước 4 — Soạn Quiz:** `QuizAuthoringService` (F6, F7, F9 phần duyệt, F12), bốc ngẫu nhiên, câu riêng.
- [ ] **Bước 5 — Phát hành:** `QuizPublicationService` (F9, F10, P3), `IQuizLifecycleListener` với adapter rỗng, phát `quiz.published`.
- [ ] **Bước 6 — Nhân bản, version, copy:** `QuizCopyService` (F11).
- [ ] **Bước 7 — AI và MCP:** `QuizAiDraftService`, `QuizDraftResultHandler` (cài `IAiTaskResultHandler` cho `QUIZ_QUESTION_DRAFT`), dùng `IReferenceMaterial` của U06, `QuizMcpTools` (cài `IMcpToolProvider`) (F4, F8, F13, P4).
- [ ] **Bước 8 — Interface cho unit khác:** `QuizQueryService` (cài `IQuizQuery`), `QuizScorer` (cài `IQuizScoring`) (F14, P2).
- [ ] **Bước 9 — API:** controller theo domain-entities mục 6; `contracts/openapi/quizzes.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 10 — Frontend:** `QuestionListPage`, `QuestionDetailPage`, `SubjectQuizListPage` (đăng ký `NavEntry` cho Ngân hàng câu hỏi, Quiz), `QuizBankSummary` (`SubjectSummarySection`), `ClassQuizzesSection` (đăng ký `TeacherClassTab`), `QuizDetailPage` và các panel, `McqEditor`; slot `QuizDetailSection`, `QuizRowAction`.
- [ ] **Bước 11 — Unit test (người code viết):** số lựa chọn và đáp án theo loại; sửa bản `ACTIVE` tạo bản nháp mới, chỉ một bản nháp; xóa bản đã kích hoạt bị từ chối; nhập tệp có dòng lỗi; bốc ngẫu nhiên không trùng, không vượt chỗ trống; duyệt với câu đã ngưng; phát hành version mới ngưng version cũ và gọi listener; chấm `MCQ_MULTI` đúng hết mới có điểm; DTO người học không có đáp án; nhân bản sao câu riêng mới, giữ phiên bản câu ngân hàng; tool MCP từ chối người học; component frontend. Integration test do tester viết.
- [ ] **Bước 12 — Review:** đối chiếu UC 46–48, story, BR-U10 với code; người dùng duyệt trước khi chuyển unit.

## Giai đoạn 1 — Ngân hàng câu hỏi (UC 46, 47) — chi tiết

Lập ngày 2026-10-20 theo [lộ trình](../cuong-uc-roadmap-2026-10-20.md), nhánh `quiz-authoring` tách từ `staging` (`6341aed`). Jira: KAN-143 (UC 46), KAN-78 (UC 47). Story: US-QBK-002 (S1 nhập tệp, S2 phiên bản). Luồng F1, F2, F3; BR-U10-01…07, 50–52; NFR-U10-01, 02, 10, 11, 30, 32; P1, P5. Không gồm F4 (AI soạn câu, để giai đoạn 4) và phần Quiz (giai đoạn 2).

### Dùng của unit khác (đã có trên `staging`)

| Contract | Unit | Dùng để |
|---|---|---|
| `IAuthorization`, `Actor`, `ResourceRef.subject` | U01 | Kiểm R2 (Chủ nhiệm môn của môn) |
| `IAudit` | U02 | Audit trong transaction nghiệp vụ |
| `ISubjectQuery` | U04 | Môn tồn tại, còn `ACTIVE` khi tạo câu |
| `GET /api/v1/manager/subjects` | U05 | Danh sách môn để chọn trên Question List |

### Quyết định cần duyệt

| # | Nội dung | Đề xuất |
|---|---|---|
| D1 | Lưu trữ: tech-stack ghi JPA; U01, U05 dùng `JdbcClient` | Dùng `JdbcClient` như U01/U05 (dễ đọc/ghi `jsonb`, câu SQL "bản mới nhất mỗi lineage") |
| D2 | Nhập `.xlsx` cần Apache POI, chưa có trong `pom.xml` | Thêm `poi-ooxml` 5.4.1 (file dùng chung `pom.xml`); `.csv` dùng Commons CSV đã có |
| D3 | Design có bước xem trước rồi "Nhập n câu hợp lệ"; API thiết kế chỉ có một `questions:import` | Cùng endpoint, tham số `dryRun=true` để kiểm, không có thì ghi |
| D4 | Hai action mới trong file dùng chung của U01 | `QUESTION_BANK_VIEW`, `QUESTION_BANK_MANAGE`: role `SUBJECT_MANAGER`, phạm vi `SUBJECT` |
| D5 | Markdown: frontend chưa có thư viện render | Giai đoạn 1 hiển thị văn bản thuần (`white-space: pre-wrap`, React tự escape); thêm render markdown đã làm sạch khi cần |
| D6 | `contracts/openapi/quizzes.yaml`: chưa unit nào có thư mục `contracts/` | Ghi API trong code-summary như các unit khác, không tạo OpenAPI |
| D7 | Migration | Một file tạo cả 4 bảng của U10 ngay (đúng infrastructure-design), để giai đoạn 2 không sửa migration |

### Checklist giai đoạn 1

- [x] **1.1 Migration** — `backend/src/main/resources/db/migration/quizzes/V20260925_1600__create_quiz_tables.sql`: `questions`, `quizzes`, `quiz_items`, `quiz_ai_drafts` theo infrastructure-design (check, unique, unique riêng phần, index, FK `subjects`, `course_classes`, `accounts`); `GRANT SELECT, INSERT, UPDATE` cho `app`, thêm `DELETE` trên `questions`, `quizzes`, `quiz_items`, `quiz_ai_drafts` (điều kiện xóa kiểm trong code).
- [x] **1.2 Quyền** — `identity/contract/Actions.java`, `identity/application/AccessPolicy.java` thêm D4; `quizzes/application/QuizAccess` (kiểm quyền, ngoài phạm vi trả `404 QUESTION_NOT_FOUND` và audit `ACCESS_DENIED`; U01/U04 lỗi → `503`, BR-U10-50, 52).
- [x] **1.3 Domain** — `quizzes/domain`: `Question` (phiên bản, chuyển trạng thái), `QuestionOption`, `QuestionContent` + `QuestionRules` (BR-U10-02, 03), enum `QuestionStatus`, `QuestionScope`, `Difficulty` (dùng `QuestionType` của contract), `IQuestionRepository`.
- [x] **1.4 Repository** — `quizzes/infrastructure/JdbcQuestionRepository`: lưu `options`, `correct_option_ids`, `tags` dạng `jsonb`; danh sách theo BR-U10-06 (bản `ACTIVE` mới nhất mỗi lineage + cờ có bản nháp, lineage chỉ có nháp), lọc loại/độ khó/trạng thái/từ khóa-tag, phân trang ≤ 100; lịch sử phiên bản; đếm Quiz đang ghim (từ `quiz_items`); cập nhật có điều kiện `version`.
- [x] **1.5 Ngân hàng** — `quizzes/application/QuestionBankService` (F1, F2, P1): xem danh sách, chi tiết + lịch sử, tạo `DRAFT`, sửa `DRAFT` kèm `version` (lệch → `409`), mở bản nháp từ `ACTIVE` (đã có nháp thì trả bản đó), kích hoạt, ngưng, xóa (chỉ nháp chưa từng kích hoạt, còn lại `409` gợi ý ngưng); audit `QUESTION_CREATED`, `QUESTION_ACTIVATED`, `QUESTION_RETIRED`, `QUESTION_DELETED`; không log nội dung câu.
- [x] **1.6 Nhập tệp** — `quizzes/application/QuestionImporter` (F3, P5, BR-U10-07, NFR-U10-30): `.csv` UTF-8 và `.xlsx` (D2), ≤ 5 MB, ≤ 500 dòng, kiểm loại theo nội dung, đọc trong bộ nhớ, ô bắt đầu bằng `=`, `+`, `-`, `@` coi là văn bản; kiểm từng dòng như tạo tay; `dryRun` (D3); ghi dòng hợp lệ trong một transaction; audit `QUESTIONS_IMPORTED` một lần.
- [x] **1.7 API** — `quizzes/web/QuestionController`, `QuestionRequests` (field lạ → `400`), `QuizzesExceptionHandler` (chỉ cho controller U10): `GET`, `POST /api/v1/subjects/{subjectId}/questions`, `POST .../questions:import`, `GET`, `PATCH`, `DELETE /api/v1/questions/{id}`, `POST /api/v1/questions/{id}/versions`, `/activate`, `/retire`.
- [x] **1.8 Unit test backend** — domain rules (2–6 lựa chọn, đáp án theo loại, trùng lựa chọn, độ dài, điểm, tag), phiên bản (sửa `ACTIVE` tạo nháp mới, một nháp mỗi lineage, xóa bản đã kích hoạt bị từ chối), `409`, quyền (Teacher/Student/Admin/Chủ nhiệm môn khác → `404`/`403`, fail closed), nhập tệp (dòng lỗi, công thức, quá 500 dòng, sai loại tệp), MockMvc cho API; repository chạy trên PostgreSQL bằng Testcontainers như `AccountRepositoryIntegrationTest`.
- [x] **1.9 Frontend** — `features/quizzes`: `api.ts`, `types.ts`, `QuestionListPage` (chọn môn qua U05, tab loại, lọc, bảng, phân trang, ghi chú phiên bản), `QuestionImportDialog` (tệp mẫu `public/templates/question-import.csv`, kiểm → kết quả từng dòng → nhập), `QuestionDetailPage` (tạo/sửa/xem, `McqEditor`, `QuestionMetaPanel`, `QuestionVersionPanel`, Kích hoạt/Ngưng/Xóa, hộp "đang dùng trong n quiz"); route `app/manager/questions`, `app/manager/questions/new`, `app/manager/questions/[id]`; `data-testid` theo `{component}-{element}`.
- [x] **1.10 Unit test frontend** — `McqEditor` (kiểm số lựa chọn, đáp án), Question List (lọc, cờ nháp), Import Dialog (kiểm rồi nhập), Question Detail (409 tải lại, sửa câu đang dùng mở nháp).
- [x] **1.11 Kiểm và tài liệu** — chạy đủ bước CI (`mvn -B verify` JDK 17, `npm ci`, lint, typecheck, test, build); `aidlc-docs/construction/u10-quiz-authoring/code/code-summary.md`; tick checklist; cập nhật `aidlc-state.md`.
- [x] **1.12 Review** — đối chiếu UC 46, 47, US-QBK-002, BR-U10-01…07 với code; người dùng duyệt trước khi sang giai đoạn 2.

## Giai đoạn 2 — Quiz (UC 48) — chi tiết

Lập ngày 2026-10-20 sau khi giai đoạn 1 được duyệt; cùng nhánh `quiz-authoring`. Jira: KAN-44 (UC 48). Story: US-ASM-006, phần Quiz của US-ASM-001. Luồng F5, F6, F7, F9, F10, F11, F12, F14; BR-U10-10…16, 20…25, 30, 31, 50–52; NFR-U10-01, 03, 10, 11, 31; P2, P3. Không gồm F8 (AI soạn câu cho Quiz, giai đoạn 4).

### Dùng của unit khác

| Contract | Unit | Dùng để |
|---|---|---|
| `IAuthorization`, `ResourceRef.subject`/`courseClass` | U01 | R2 với Quiz của môn; R3/R4 (giảng viên lớp) với Quiz của lớp |
| `IClassAccess` | U05 | Lớp, môn của lớp, lớp `OPEN` khi phát hành, lớp đang mở của môn |
| `ISubjectQuery` | U04 | Môn `ACTIVE` khi phát hành Quiz của môn |
| `IEventPublisher` + `EventTypes.QUIZ_PUBLISHED` | U03 | Phát `quiz.published`, gửi sau commit |
| `IAudit` | U02 | `QUIZ_*` |
| Slot `teacherClassTabs` (tab `evals`), `subjectSummarySections` | U05 (frontend) | Phần Quiz trong tab Đánh giá; số câu hỏi, Quiz trên Manager Dashboard |

### Quyết định cần duyệt

| # | Nội dung | Đề xuất |
|---|---|---|
| E1 | Action mới trong file dùng chung của U01 | `QUIZ_SUBJECT_MANAGE` (Subject Manager, phạm vi môn); `QUIZ_CLASS_MANAGE` (Teacher, Subject Manager, phạm vi lớp: giảng viên của lớp) |
| E2 | Design cho `BankQuestionPicker` gọi API ngân hàng, nhưng API đó chỉ cho Chủ nhiệm môn (BR-U10-01) | Thêm `GET /api/v1/quizzes/{id}/bank-questions`: chỉ câu `ACTIVE` (bản mới nhất mỗi lineage) của môn chứa Quiz, quyền theo Quiz |
| E3 | Theo dõi hoạt động cần `activity.retentionDays`, chưa có khóa cài đặt nào (U08 chưa làm) | Lưu cờ nhưng chặn bật (`409 ACTIVITY_RETENTION_NOT_SET`); UI hiện công tắc tắt kèm ghi chú |
| E4 | Gắn học liệu (U13 chưa có) | Giữ cột `reference_material_ids`, API nhận tối đa 10 id; UI ẩn `MaterialRefPicker` |
| E5 | `IQuizLifecycleListener` (U11 chưa có) | Gọi mọi bean đang có trong cùng transaction; hiện chưa có bean nào |
| E6 | Route | `/manager/quizzes` (Quiz List của Chủ nhiệm môn), `/quizzes/[id]` (Quiz Detail, layout mới cho Teacher, Subject Manager) |
| E7 | Phiên bản Quiz công khai | Như câu hỏi: dùng `version` trong DB, mọi thao tác ghi gửi kèm |
| E8 | Commit giai đoạn 1 trước khi viết code giai đoạn 2 | Một commit local trên `quiz-authoring` (code + tài liệu giai đoạn 1), không push |

### Checklist giai đoạn 2

- [x] **2.0 Commit giai đoạn 1** — commit local (E8): `5d70173`.
- [x] **2.1 Quyền** — E1 trong `Actions`, `AccessPolicy` + test bảng quyền; `QuizAccess` thêm kiểm theo lớp.
- [x] **2.2 Domain** — `quizzes/domain`: `Quiz` (trạng thái BR-U10-20, nguồn, version), `QuizItem`, `QuizSettingsRules` (BR-U10-15), `QuizRules` (1–200 câu, điểm > 0, duyệt BR-U10-21), `IQuizRepository`; câu riêng dùng lại `Question` với `scope = QUIZ`.
- [x] **2.3 Repository** — `JdbcQuizRepository` (`quizzes`, `quiz_items`, `settings` dạng `jsonb`, cập nhật có điều kiện `version` và trạng thái); mở rộng `JdbcQuestionRepository` cho câu riêng và câu `ACTIVE` để chọn/bốc ngẫu nhiên (không trùng câu đã có).
- [x] **2.4 Soạn Quiz** — `QuizAuthoringService` (F5, F6, F7, F12): danh sách Quiz của môn và của lớp (Quiz của môn hiện ở lớp, chỉ đọc), tạo (cài đặt mặc định), sửa tiêu đề/hướng dẫn/cài đặt, lưu danh sách câu (chọn tay, thứ tự, điểm), xem trước bốc ngẫu nhiên, thêm/sửa/xóa câu riêng, sửa Quiz `REVIEWED` đưa về `DRAFT`, xóa Quiz chưa từng phát hành; audit `QUIZ_CREATED`, `QUIZ_DELETED`.
- [x] **2.5 Duyệt, phát hành, ngưng** — `QuizPublicationService` (F9, F10, P3): duyệt (trả danh sách lỗi theo câu), phát hành (lớp `OPEN` hoặc môn `ACTIVE`; version cũ đang `OPEN` → `RETIRED` và gọi listener trong cùng transaction; phát `quiz.published`), ngưng có lý do; audit `QUIZ_REVIEWED`, `QUIZ_PUBLISHED`, `QUIZ_RETIRED`.
- [x] **2.6 Nhân bản, version, copy** — `QuizCopyService` (F11, BR-U10-30, 31): câu ngân hàng giữ phiên bản ghim, câu riêng sao mới; version mới chỉ từ Quiz `OPEN`/`RETIRED`, tối đa một bản đang soạn; copy giữa lớp khi giảng viên dạy cả hai lớp; audit `QUIZ_CLONED`, `QUIZ_VERSION_CREATED`, `QUIZ_COPIED`.
- [x] **2.7 Interface cho unit khác** — `QuizQueryService` (cài `IQuizQuery`: Quiz `OPEN` của lớp và của môn chứa lớp; đề không có đáp án), `QuizScorer` (cài `IQuizScoring`: `MCQ_SINGLE` đúng là đủ điểm, `MCQ_MULTI` phải đúng hết; `answerReview`); thay stub nhờ `@ConditionalOnMissingBean`.
- [x] **2.8 API** — `SubjectQuizController` (`/subjects/{id}/quizzes`), `ClassQuizController` (`/classes/{id}/quizzes`, `/quiz-copy-sources`, `/quizzes:copy`), `QuizController` (`/quizzes/{id}`, `/items`, `/items:random-preview`, `/inline-questions`, `/bank-questions` (E2), `/review`, `/publish`, `/retire`, `/clone`, `/versions`); exception handler của U10 áp dụng cho cả ba.
- [x] **2.9 Unit test backend** — chuyển trạng thái; cài đặt sai; 1–200 câu; bốc ngẫu nhiên không trùng, không vượt chỗ trống; duyệt với câu ngân hàng đã ngưng; phát hành version mới ngưng version cũ và gọi listener; chấm `MCQ_MULTI`; DTO người học không có đáp án; nhân bản sao câu riêng mới, giữ phiên bản câu ngân hàng; giảng viên lớp khác `404`; giảng viên không sửa Quiz của môn; MockMvc; repository trên PostgreSQL (Testcontainers).
- [x] **2.10 Frontend** — `SubjectQuizListPage` (route `/manager/quizzes`), `QuizDetailPage` (`QuizTopBar`, `QuizItemList`, `BankQuestionPicker` chọn tay/bốc ngẫu nhiên, `InlineQuestionEditor` dùng `McqEditor`, `QuizSettingsPanel`, `QuizPreviewDialog`, popup ngưng/nhân bản/version mới/xóa; route `/quizzes/[id]` + layout), `ClassQuizzesSection` (đăng ký `teacherClassTabs` tab `evals`: Tạo Quiz, Copy Quiz từ lớp khác), `QuizBankSummary` (đăng ký `subjectSummarySections`), khai báo slot `quizRowActions`; `data-testid`.
- [x] **2.11 Unit test frontend** — Quiz List lọc trạng thái; Quiz Detail lưu câu kèm `version`, 409 tải lại, lỗi duyệt theo câu, phát hành/ngưng; picker ẩn câu đã có; Class section hiện Quiz của môn chỉ xem.
- [x] **2.12 Kiểm và tài liệu** — đủ bước CI; cập nhật code-summary; tick checklist; `aidlc-state.md`.
- [x] **2.13 Review** — đối chiếu UC 48, US-ASM-006, BR-U10-10…31 với code; người dùng duyệt trước khi sang giai đoạn 3.

## Giai đoạn 4 — AI soạn câu, tool MCP, tài liệu tham khảo (U10) và `MaterialQuizList` (U11) — chi tiết

Lập ngày 2026-10-22 theo [lộ trình](../cuong-uc-roadmap-2026-10-20.md). Nhánh `quiz-ai-mcp` tách từ `staging` (`08a6134`, đã có U09, U13, U15).
- **Jira:** phần còn lại của KAN-78 (UC 47) và KAN-44 (UC 48); phần Quiz trên học liệu của KAN-129 (UC 50) và KAN-130 (UC 51).
- **Story:** US-AIG-001, US-AIG-002, phần AI của US-ASM-006.
- **Luồng:** U10 F4, F8, F13 và phần gắn tài liệu của F7; U11 F1 bước 5.
- **Quy tắc:** BR-U10-11, 40, 41, 42, 51, 52; NFR-U10-32; P4.
- **Ngoài phạm vi:**
  - Chạy với AI thật: chờ U06 merge, mục 2 của giai đoạn 4 trong lộ trình.
  - Tracker hoạt động phía trình duyệt (G3) và bật theo dõi hoạt động (E3): chờ U08.

### Dùng của unit khác

| Contract | Unit | Tình trạng trên `staging` | Dùng để |
|---|---|---|---|
| `IAiTask`, `IAiTaskResultHandler`, `AiTaskRequest`, `AiPrompt` | U06 | Chỉ có contract và stub. Stub trả task `FAILED` ("AI chưa sẵn sàng"). Code thật ở nhánh `ai-connections` | Gửi task `QUIZ_QUESTION_DRAFT`, nhận kết quả |
| `IMcpToolProvider`, `McpToolDefinition` | U06 | Có contract; U09, U13 đã cài theo cùng cách | `read_quiz_context`, `save_quiz_draft` |
| `IReferenceMaterial` | U13 cài | Có code thật (`ReferenceMaterialService`) | Danh sách tài liệu chọn được, lấy chữ đưa vào prompt |
| Slot `learningMaterialSections` | U13 (frontend) | Có, hiện trên `LearningMaterialPage` | `MaterialQuizList` của U11 |
| `IAuthorization`, `IAudit` | U01, U02 | Có | Kiểm quyền, audit `QUIZ_AI_DRAFT_ACCEPTED` |

Không cần action mới: AI cho Quiz dùng `QUIZ_SUBJECT_MANAGE` hoặc `QUIZ_CLASS_MANAGE` như sửa Quiz; AI cho ngân hàng dùng `QUESTION_BANK_MANAGE`.

### Quyết định cần duyệt

| # | Nội dung | Đề xuất |
|---|---|---|
| H1 | Design dùng `AiSourcePicker` của U06, nhưng component này chỉ có ở nhánh `ai-connections` | Làm `AiSourceSelect` riêng trong `features/quizzes`, cùng kiểu dữ liệu `{ source, provider? }` với `SourceChoice` của U06 (U09 cũng làm vậy). Khi U06 merge thì thay bằng `AiSourcePicker`, chỉ sửa một file |
| H2 | Gắn task AI với đề xuất | `targetRef = "quiz-ai-draft:{draftId}"`. `inputVersion` là `version` của Quiz (đích Quiz) hoặc `0` (đích ngân hàng). `requestRef` do frontend tạo, ≤ 100 ký tự, để chống gửi trùng. Hai bước như U09: (1) tạo đề xuất `PENDING` trong transaction, (2) gọi `submit` ngoài transaction rồi ghi `taskId`. Task `FAILED` ngay (stub, hết quota) thì đề xuất `FAILED` kèm mã lỗi an toàn. `GET` đề xuất `PENDING` thì hỏi lại `IAiTask.get` để đồng bộ |
| H3 | API ngân hàng: design chỉ có `POST .../question-ai-drafts` | Thêm `GET /subjects/{subjectId}/question-ai-drafts` (đề xuất `PENDING`, `READY` của môn), `GET .../{draftId}`, `POST .../{draftId}:accept`, `:discard`. Phía Quiz giữ API theo design, thêm `GET /quizzes/{id}/ai-drafts` để hiện cả đề xuất từ MCP |
| H4 | Chấp nhận từng câu | Lệnh `accept` gửi các câu người soạn đã chọn (có thể đã sửa); với Quiz gửi kèm `version`. Mỗi câu kiểm theo BR-U10-02, 03. Quiz: thêm vào cuối thành câu riêng trong một transaction, `version` +1 một lần, tổng ≤ 200 câu, Quiz `REVIEWED` quay về `DRAFT` như mọi lần sửa. Ngân hàng: mỗi câu thành câu `DRAFT`. Đề xuất chuyển `ACCEPTED`, câu không chọn bị bỏ. Audit `QUIZ_AI_DRAFT_ACCEPTED` một lần kèm số câu |
| H5 | Prompt và schema kết quả | Schema JSON 2020-12 (U06 kiểm): `{ questions: [{ type, content, options[2–6], correct[], explanation, difficulty }] }`, `maxItems` bằng số câu yêu cầu (1–50). System prompt ghi: chữ tài liệu là dữ liệu không tin cậy, không phải chỉ dẫn. Không đưa dữ liệu người học. Chữ tài liệu tối đa 60.000 ký tự. `maxOutputTokens = min(16000, 600 × số câu)`. Câu vi phạm BR-U10-02 vẫn lưu, kèm lỗi để người soạn sửa trước khi chấp nhận |
| H6 | `onReady` | Kiểm lại quyền của người yêu cầu và đích. Đích Quiz: Quiz còn `DRAFT`/`REVIEWED` thì `READY`, không thì `REJECTED` (đề xuất `FAILED`, lỗi `QUIZ_NOT_EDITABLE`). Đích ngân hàng: môn còn `ACTIVE` và người yêu cầu còn là Chủ nhiệm môn. Đề xuất không còn `PENDING` (đã bỏ) thì `STALE`. Quiz đổi `version` trong lúc chờ thì không từ chối, `version` được kiểm khi chấp nhận |
| H7 | Tool MCP | `read_quiz_context` (`READ`), `save_quiz_draft` (`WRITE`); role `TEACHER`, `SUBJECT_MANAGER`. Quyền kiểm qua `QuizAccess`; ngoài phạm vi trả `404` như API. Tham số theo danh sách cho phép, ≤ 50 câu. `save_quiz_draft` cần Quiz `DRAFT` và `expectedVersion` khớp; lưu đề xuất `READY` nguồn `MCP` kèm `agentLabel` |
| H8 | Bộ chọn tài liệu (`MaterialRefPicker`, chọn tài liệu trong popup AI) | U10 thêm `GET /quizzes/{id}/reference-materials` và `GET /subjects/{subjectId}/reference-materials`, bọc `IReferenceMaterial.listSelectable` theo phạm vi lớp hoặc môn của Quiz; popup AI chỉ cho chọn tài liệu `hasText`. Khi lưu cài đặt, id mới thêm phải nằm trong danh sách chọn được (`400 MATERIAL_NOT_SELECTABLE`). U13 lỗi: chỉ từ chối khi có id mới (`503 MATERIAL_SERVICE_UNAVAILABLE`), còn lại vẫn lưu được (BR-U10-52) |
| H9 | `MaterialQuizList` (U11, G4) | `GET /api/v1/me/quizzes?classId=&materialId=` lọc qua `IQuizQuery.listByMaterial`, kèm số lượt đã dùng, còn lại và điểm cao nhất. Component đăng ký vào `learningMaterialSections`; không có Quiz thì không hiện gì |
| H10 | Commit | Bước 4.0: commit local các tài liệu đang chờ (roadmap, audit khôi phục từ stash) trên `quiz-ai-mcp`. Code giai đoạn 4 commit local sau review; push và mở PR khi người dùng yêu cầu |

### Checklist giai đoạn 4

- [x] **4.0 Commit tài liệu đang chờ** (H10).
- [ ] **4.1 Domain và repository:**
  - `QuizAiDraft`: đích, nguồn, trạng thái `PENDING → READY | FAILED`, `READY → ACCEPTED | DISCARDED`.
  - `DraftQuestion`, `IQuizAiDraftRepository`.
  - `JdbcQuizAiDraftRepository`: `proposal`, `provenance` dạng `jsonb`; đổi trạng thái có điều kiện.
- [ ] **4.2 Prompt:** `QuizDraftPrompt` dựng `AiPrompt` và schema, đọc kết quả thành `DraftQuestion` kèm lỗi từng câu (H5).
- [ ] **4.3 Dịch vụ đề xuất:** `QuizAiDraftService` (F4, F8) gồm:
  - gửi yêu cầu cho ngân hàng và cho Quiz (H2);
  - xem, liệt kê, đồng bộ `PENDING`;
  - chấp nhận (H4), bỏ.
- [ ] **4.4 Nhận kết quả:** `QuizDraftResultHandler` cài `IAiTaskResultHandler` cho `QUIZ_QUESTION_DRAFT` (H6).
- [ ] **4.5 MCP:** `QuizMcpTools` cài `IMcpToolProvider` (F13, H7).
- [ ] **4.6 Tài liệu tham khảo:** `QuizReferenceMaterials` (H8) gồm:
  - danh sách chọn được;
  - kiểm id trong `updateDetails`;
  - lấy chữ cho prompt.
- [ ] **4.7 API U10:** thêm vào `QuizController`, `QuestionController`:
  - `/ai-drafts`, `/question-ai-drafts`, `/reference-materials`;
  - mã lỗi `AI_DRAFT_NOT_FOUND` (404), `AI_DRAFT_CLOSED` (409), `AI_DRAFT_INVALID` (422, lỗi theo câu), `MATERIAL_NOT_SELECTABLE`, `MATERIAL_SERVICE_UNAVAILABLE`;
  - lỗi của U06 trả nguyên mã.
- [ ] **4.8 API U11:** `QuizPracticeQueryService`, `QuizPracticeController` nhận `materialId` (H9).
- [ ] **4.9 Unit test backend:**
  - Đề xuất: stub `FAILED` thì đề xuất `FAILED`; gửi trùng `requestRef` không tạo hai đề xuất; `GET` đồng bộ task đã lỗi.
  - `onReady`: Quiz đã phát hành thì `REJECTED`; người yêu cầu mất quyền thì `REJECTED`; đề xuất đã bỏ thì `STALE`.
  - Chấp nhận: câu sai bị từ chối; `version` cũ trả `409`; vượt 200 câu bị từ chối; ngân hàng tạo câu `DRAFT`; audit.
  - MCP: từ chối người học và lớp khác; tham số lạ; `expectedVersion` lệch.
  - Tài liệu: id không chọn được; U13 lỗi chỉ chặn id mới.
  - `listByMaterial` theo lớp; MockMvc cho API mới; repository trên PostgreSQL (Testcontainers).
- [ ] **4.10 Frontend U10:**
  - `AiSourceSelect` (H1);
  - `QuestionAiDraftDialog` (nút "Tạo nháp bằng AI" trên Question List);
  - `QuizAiDraftPanel` (Quiz Detail, Quiz `DRAFT`), cả hai dùng chung `DraftProposalList` (sửa, bỏ, chấp nhận từng câu; hỏi lại mỗi 3 giây khi `PENDING`);
  - `MaterialRefPicker` trong `QuizSettingsPanel`;
  - `data-testid`.
- [ ] **4.11 Frontend U11:** `MaterialQuizList` đăng ký `learningMaterialSections` (Đã dùng x/y lượt, điểm cao nhất, Làm quiz, hết lượt).
- [ ] **4.12 Unit test frontend:** popup AI gửi đúng nguồn và tài liệu; danh sách đề xuất chấp nhận câu đã sửa, `409` tải lại; picker giới hạn 10; `MaterialQuizList` hết lượt.
- [ ] **4.13 Kiểm và tài liệu:**
  - đủ bước CI (backend `mvn -B verify`; frontend lint, typecheck, test, build);
  - code-summary U10, U11;
  - tick checklist, lộ trình, `aidlc-state.md`.
- [ ] **4.14 Review:** đối chiếu UC 47, 48 (phần AI), BR-U10-40…42 và G4 với code; người dùng duyệt.

## Integration test cases (tester viết)

- Phát hành version mới khi có lượt đang làm của version cũ: U11 nhận `onRetired`.
- Hai Chủ nhiệm môn sửa cùng câu: một bên nhận `409`.
- Nhập tệp 500 dòng có dòng lỗi.
- Người học gọi mọi API của U10: không thấy đáp án, không soạn được.
- Phân quyền 4 role trên mọi endpoint, gồm giảng viên sửa Quiz của môn và Chủ nhiệm môn sửa ngân hàng môn khác.
- Kết quả AI về sau khi Quiz đã phát hành: đề xuất `FAILED`, Quiz không đổi.
- Tool MCP: người học và giảng viên lớp khác bị từ chối; `save_quiz_draft` với `expectedVersion` cũ.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
