# Lộ trình UC của Đào Chí Cường (Người 1) — 2026-10-20

Nguồn: 25 issue Jira KAN giao cho Đào Chí Cường (đọc ngày 2026-10-20), [aidlc-state](../../aidlc-state.md), [unit-summary](../unit-summary.md), code plan [U10](code-generation-plan/u10-quiz-authoring-code-generation-plan.md), [U11](code-generation-plan/u11-quiz-practice-code-generation-plan.md), [U01 code-summary](../u01-account-and-access/code/code-summary.md).

Đây là lộ trình gom việc, không thay các code plan theo unit. Mỗi unit vẫn phải được duyệt Code Generation Part 1 trước khi viết code (Part 2).

## 1. Hiện trạng

| Jira | UC | Unit | Jira | Code thực tế |
|---|---|---|---|---|
| KAN-18 (epic) | 01–12 | U01 | In Progress | Đã code và review trên nhánh `cuong` (backend 255, frontend 77 test xanh). Chưa vào `staging`/`main` |
| KAN-68, 69, 124, 70, 73, 71, 125, 144, 74, 145, 72, 146 | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12 | U01 | To Do | Như dòng trên |
| KAN-23 (epic) | 46–48 | U10 | To Do | Chỉ có khung: contract `IQuizQuery`, `IQuizScoring`, `IQuizLifecycleListener` và stub |
| KAN-143, 78, 44 | 46, 47, 48 | U10 | To Do | Chưa có |
| KAN-25 (epic) | 49–52 | U11 | To Do | Chỉ có khung package, chưa có code |
| KAN-128, 129, 130, 57 | 49, 50, 51, 52 | U11 | To Do | Chưa có |
| KAN-152 | — | Báo cáo | In Progress | Việc tài liệu (Report 3, phụ lục Requirement), không phải code |

Unit U10 và U11 dùng contract của các unit khác. Contract đều đã có trong khung kèm stub, nên code được trước khi unit kia xong:

| Contract | Unit giữ | Tình trạng (2026-10-20) | U10/U11 dùng để |
|---|---|---|---|
| `ISubjectQuery`, `ISubjectScope` | U04 | Có code ở `staging` | Kiểm R2, môn `ACTIVE` khi phát hành |
| `IClassAccess`, `IClassScope`, slot `TeacherClassTab`, `SubjectSummarySection` | U05 | Có code ở `staging` | Quiz của lớp, ghi danh, tab Evals |
| `IAudit` | U02 | Nhánh `audit` | Audit mọi lệnh |
| `ISettings`, `IScheduledScanner` | U03 | Có code | `activity.retentionDays`; scanner tự nộp |
| `IAiTask`, `IAiTaskResultHandler`, `IMcpToolProvider`, `IReferenceMaterial` | U06 | Stub | AI soạn câu (F4, F8), tool MCP (F13) |
| `IActivityWrite`, `IActivityQuery`, `useActivityTracker` | U08 | Stub | Dữ liệu hoạt động lượt Quiz |
| Slot `LearningMaterialSection` | U13 | Chưa có | Quiz gắn tài liệu |
| Sự kiện `quiz.published` | U14 | Chưa có người nhận | Thông báo Quiz mới |

## 2. Thứ tự làm

U11 cần `IQuizQuery`, `IQuizScoring` thật của U10, nên làm U10 trước. AI/MCP phụ thuộc U06 nên để cuối và làm trên stub.

### Giai đoạn 0 — Chốt U01 (KAN-18)

- [ ] Mở PR `cuong` → `staging`, giải xung đột với U04/U05 (file dùng chung: `SecurityConfig`, `ArchitectureTest`, `client.ts`, `app/admin/layout.tsx`, `pom.xml`, compose).
- [ ] Sau khi có route `/classes`, `/manager` của U05/U04 trên `staging`: đổi `HomePaths.of` về `CLASS_DASHBOARD`, `MANAGER_DASHBOARD` (ghi chú trong code-summary).
- [ ] Chạy lại toàn bộ test backend/frontend trên `staging`; giao tester viết integration test U01.
- [ ] Cập nhật Jira: 12 UC của U01 sang Done (hoặc trạng thái review của nhóm), đóng KAN-18 khi tester xác nhận.

### Giai đoạn 1 — U10 phần ngân hàng câu hỏi: UC 46, 47 (KAN-143, KAN-78)

Theo plan U10 bước 1–3 và phần tương ứng của bước 9–11.

- [ ] Migration `quizzes` (mọi bảng của U10 trong một migration, gồm bảng Quiz, để không phải sửa migration sau).
- [ ] Domain `Question` (lineage, `DRAFT`/`ACTIVE`/`RETIRED`, kiểm số lựa chọn và đáp án theo loại), repository.
- [ ] `QuestionBankService`: xem (F1), tạo, sửa, tạo bản nháp từ bản `ACTIVE`, kích hoạt, ngưng, xóa (F2).
- [ ] `QuestionImporter`: nhập tệp ≤ 5 MB, ≤ 500 dòng, kết quả từng dòng (F3).
- [ ] API `/subjects/{id}/questions`, `/questions/{id}`, `questions:import`; `contracts/openapi/quizzes.yaml` phần ngân hàng.
- [ ] Frontend: `QuestionListPage`, `QuestionDetailPage`, `McqEditor`, `NavEntry` Ngân hàng câu hỏi.
- [ ] Unit test phần ngân hàng; Jira KAN-143, KAN-78 → In Progress rồi review.

### Giai đoạn 2 — U10 phần Quiz: UC 48 (KAN-44)

Theo plan U10 bước 4–6, 8 và phần còn lại của bước 9–11.

- [ ] Domain `Quiz`, `QuizItem`; `QuizAuthoringService` (F5–F7, F12): danh sách, tạo, cài đặt, chọn tay và bốc ngẫu nhiên câu ngân hàng, câu riêng, xóa Quiz nháp.
- [ ] `QuizPublicationService` (F9, F10): xem trước, duyệt, phát hành, ngưng; `IQuizLifecycleListener` adapter rỗng; phát `quiz.published` sau commit.
- [ ] `QuizCopyService` (F11): nhân bản, version mới, copy giữa lớp.
- [ ] `QuizQueryService` (cài `IQuizQuery`), `QuizScorer` (cài `IQuizScoring`) (F14) — thay stub.
- [ ] API Quiz; frontend `SubjectQuizListPage`, `ClassQuizzesSection` (`TeacherClassTab`), `QuizBankSummary`, `QuizDetailPage` và panel; slot `QuizDetailSection`, `QuizRowAction`.
- [ ] Unit test phần Quiz; review U10 không gồm AI/MCP.

### Giai đoạn 3 — U11: UC 51, 52, 49, 50 (KAN-130, KAN-57, KAN-128, KAN-129)

Theo plan U11. Làm UC 51 trước vì các màn khác đều dựa vào lượt làm.

- [ ] Migration `quizpractice`, domain `QuizAttempt` (trộn có seed).
- [ ] UC 51: `QuizAttemptService` bắt đầu/tiếp tục, tự lưu có `answerVersion`, nộp và chấm (F3–F5); `QuizAutoSubmitScanner`, `QuizRetiredHandler` cài `IQuizLifecycleListener` (F6); frontend `QuizTakingPage`.
- [ ] UC 52: kết quả theo cài đặt hiện điểm/đáp án (F7); `QuizResultPage`.
- [ ] UC 49: lịch sử luyện tập (F1); `QuizPracticeHistoryPage`, `NavEntry` Luyện tập Quiz.
- [ ] UC 50: chi tiết Quiz (F2); `QuizPracticeDetailPage`, slot `QuizMaterialSection`.
- [ ] Dữ liệu hoạt động (F8) và giảng viên xem lượt (F9): `QuizAttemptsSection`, `QuizAttemptCount` gắn vào slot của U10.
- [ ] Unit test theo plan U11 bước 9; review U11.

### Giai đoạn 4 — AI và MCP của U10 (phần còn lại của UC 47, 48)

- [ ] `QuizAiDraftService`, `QuizDraftResultHandler` (`QUIZ_QUESTION_DRAFT`), `QuizMcpTools` (F4, F8, F13) trên contract U06.
- [ ] Chạy thật khi U06 có code; trước đó chỉ test với stub.

### Giai đoạn 5 — Kiểm tra và đóng Jira

- [ ] Tester viết integration test theo mục "Integration test cases" của plan U10, U11.
- [ ] Chạy thử end-to-end với Docker local: Chủ nhiệm môn soạn câu → giảng viên tạo và phát hành Quiz → sinh viên làm, nộp, xem kết quả → phát hành version mới tự nộp lượt đang làm.
- [ ] Đóng KAN-23, KAN-25 và các UC con.

### Song song — KAN-152

- [ ] Rà phần V. Requirement Appendix của Report 3 theo 65 UC hiện hành; không ảnh hưởng code.

## 3. Nhánh

- U01 giữ nhánh `cuong` tới khi merge vào `staging`.
- U10, U11 tạo nhánh mới từ `staging` sau khi U01 đã vào, để dùng code thật của U04/U05 thay vì stub.
