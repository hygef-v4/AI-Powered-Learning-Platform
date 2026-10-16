# U11 Quiz Practice — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Lấy phần lượt Quiz của U11 cũ và chấm Quiz luyện tập của U15 cũ. Quiz chỉ có chế độ luyện tập (D01). Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `MyQuizController`, `QuizAttemptController`, `ClassQuizAttemptController` | backend | API mục 4 của domain-entities |
| `QuizPracticeQueryService` | backend | Lịch sử, chi tiết (F1, F2) |
| `QuizAttemptService` | backend | Bắt đầu, lưu, nộp, chấm, kết quả (F3–F5, F7, P1–P3) |
| `QuizAutoSubmitScanner` (cài `IScheduledScanner`), `QuizRetiredHandler` (cài `IQuizLifecycleListener`) | worker, backend | Tự nộp (F6) |
| `QuizActivityService` | backend | Ghi sự kiện qua U08 (F8) |
| `TeacherQuizAttemptService` | backend | Lượt cho giảng viên (F9) |
| Repository `quiz_attempts` | backend, worker | Chỉ dùng trong package |
