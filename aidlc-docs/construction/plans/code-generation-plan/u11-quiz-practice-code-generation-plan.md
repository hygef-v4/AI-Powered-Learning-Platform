# U11 Quiz Practice — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U11 Quiz Practice thuộc Iter 3, người code: Người 1; UC 49–52. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 49, 50, 51, 52. **Primary stories:** US-ASM-003 (phần Quiz); dữ liệu hoạt động lượt Quiz của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-039.

Thiết kế: [business-logic-model](../../u11-quiz-practice/functional-design/business-logic-model.md), [business-rules](../../u11-quiz-practice/functional-design/business-rules.md), [domain-entities](../../u11-quiz-practice/functional-design/domain-entities.md), [frontend-components](../../u11-quiz-practice/functional-design/frontend-components.md), [NFR](../../u11-quiz-practice/nfr-requirements/nfr-requirements.md), [NFR design](../../u11-quiz-practice/nfr-design/nfr-design-patterns.md), [infrastructure](../../u11-quiz-practice/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan mới, chưa được duyệt và chưa code. Thay phần lượt Quiz của plan U11 cũ và chấm Quiz luyện tập của plan U15 cũ.

## Code hiện có

| Path | Hiện trạng | Việc trong plan này |
|---|---|---|
| `backend/.../attempts/port/PracticeResultPort`, `PracticeResult` | Port cũ để U15 cũ chấm Quiz | Xóa; U11 chấm qua `QuizScoringPort` của U10 và lưu kết quả trên lượt |
| `contracts/openapi/attempts.yaml` | Đã thay ở plan U08 | Thêm `contracts/openapi/quiz-practice.yaml` |

## Checklist

- [ ] **Bước 1 — Dọn code cũ, package:** xóa port ở bảng trên; tạo package `edu.aiplatform.quizpractice`.
- [ ] **Bước 2 — Migration:** `db/migration/quizpractice/V20260925_1700__create_quiz_attempts.sql` theo infrastructure-design.
- [ ] **Bước 3 — Domain:** `QuizAttempt` và enum; trộn có seed; repository chỉ dùng trong package.
- [ ] **Bước 4 — Xem Quiz:** `QuizPracticeQueryService` (F1, F2).
- [ ] **Bước 5 — Làm, nộp, chấm:** `QuizAttemptService` (F3–F5, F7, P1–P3).
- [ ] **Bước 6 — Tự nộp:** `QuizAutoSubmitScanner`, `QuizRetiredHandler` (F6).
- [ ] **Bước 7 — Hoạt động và giảng viên:** `QuizActivityService` (F8), `TeacherQuizAttemptService` (F9).
- [ ] **Bước 8 — API:** controller theo domain-entities mục 4; `contracts/openapi/quiz-practice.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 9 — Frontend:** `QuizPracticeHistoryPage` (đăng ký `DashboardNavEntry`), `QuizPracticeDetailPage`, `QuizTakingPage`, `QuizResultPage`, `MaterialQuizList` (slot của U13), `QuizAttemptsSection` (slot của U10); khai báo slot `QuizMaterialSection` cho U13.
- [ ] **Bước 10 — Unit test (người code viết):** điều kiện bắt đầu (ghi danh, Quiz mở, hết lượt, không giới hạn lượt); một lượt đang làm; trộn theo seed ổn định; lưu sau giờ và trong 30 giây ân hạn; nộp lặp; chấm `MCQ_MULTI`; tự nộp khi hết giờ và khi Quiz bị ngưng; kết quả theo cài đặt hiện điểm và đáp án; giảng viên lớp khác bị từ chối; component frontend. Integration test do tester viết.
- [ ] **Bước 11 — Review:** đối chiếu UC 49–52, story, BR-U11 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Hết giờ khi đang làm: scanner nộp và chấm đúng một lần.
- U10 phát hành version mới khi có lượt đang làm: lượt được nộp `AUTO_RETIRED`.
- Người học gọi API trước khi nộp không thấy đáp án.
- Phân quyền 4 role trên mọi endpoint, gồm dùng ID lượt của người khác.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
