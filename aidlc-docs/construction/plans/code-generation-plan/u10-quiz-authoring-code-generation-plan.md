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
- [ ] **Bước 10 — Frontend:** `QuestionListPage`, `QuestionDetailPage`, `SubjectQuizListPage` (đăng ký `DashboardNavEntry`), `ClassQuizzesSection` (đăng ký `TeacherClassTab`), `QuizDetailPage` và các panel, `McqEditor`; slot `QuizDetailSection`.
- [ ] **Bước 11 — Unit test (người code viết):** số lựa chọn và đáp án theo loại; sửa bản `ACTIVE` tạo bản nháp mới, chỉ một bản nháp; xóa bản đã kích hoạt bị từ chối; nhập tệp có dòng lỗi; bốc ngẫu nhiên không trùng, không vượt chỗ trống; duyệt với câu đã ngưng; phát hành version mới ngưng version cũ và gọi listener; chấm `MCQ_MULTI` đúng hết mới có điểm; DTO người học không có đáp án; nhân bản sao câu riêng mới, giữ phiên bản câu ngân hàng; tool MCP từ chối người học; component frontend. Integration test do tester viết.
- [ ] **Bước 12 — Review:** đối chiếu UC 46–48, story, BR-U10 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Phát hành version mới khi có lượt đang làm của version cũ: U11 nhận `onRetired`.
- Hai Chủ nhiệm môn sửa cùng câu: một bên nhận `409`.
- Nhập tệp 500 dòng có dòng lỗi.
- Người học gọi mọi API của U10: không thấy đáp án, không soạn được.
- Phân quyền 4 role trên mọi endpoint, gồm giảng viên sửa Quiz của môn và Chủ nhiệm môn sửa ngân hàng môn khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
