# U10 Quiz Authoring — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

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
| `QuizQueryService` (cài `IQuizQuery`), `QuizScorer` (cài `IQuizScoring`) | backend | Interface cho U11, U13 (F14, P2) |
| Repository `questions`, `quizzes`, `quiz_items`, `quiz_ai_drafts` | backend, worker | Chỉ dùng trong package |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
