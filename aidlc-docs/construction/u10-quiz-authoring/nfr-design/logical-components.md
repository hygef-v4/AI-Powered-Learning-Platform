# U10 Quiz Authoring — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U10 Quiz Authoring thuộc Iter 2, người code: Người 1; UC 46–48. Lấy ngân hàng câu hỏi của U06 cũ, phần Quiz của U08 cũ và cài đặt Quiz của U09 cũ. Ngân hàng chỉ còn câu trắc nghiệm theo UC 46, 47. Làm Quiz thuộc U11. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 46, 47, 48. **Primary stories:** US-ASM-006, US-QBK-002; phần Quiz của US-ASM-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `QuestionController`, `QuizController`, `ClassQuizController`, `SubjectQuizController`, `QuizAiDraftController` | backend | API mục 6 của domain-entities |
| `QuestionBankService`, `QuestionImporter` | backend | Ngân hàng, phiên bản, nhập tệp (F1–F3, P1, P5) |
| `QuizAuthoringService` | backend | Tạo, soạn, duyệt, xóa (F6, F7, F9, F12) |
| `QuizPublicationService` | backend | Phát hành, ngưng (F9, F10, P3) |
| `QuizCopyService` | backend | Nhân bản, version mới, copy (F11) |
| `QuizAiDraftService`, `QuizDraftResultHandler`, `QuizMcpTools` | backend, worker | AI soạn câu, tool MCP (F4, F8, F13, P4) |
| `QuizQueryService` (cài `QuizQueryPort`), `QuizScorer` (cài `QuizScoringPort`) | backend | Port cho U11, U13 (F14, P2) |
| Repository `questions`, `quizzes`, `quiz_items`, `quiz_ai_drafts` | backend, worker | Chỉ dùng trong package |
