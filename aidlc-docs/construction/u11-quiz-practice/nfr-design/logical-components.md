# U11 Quiz Practice — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

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

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
