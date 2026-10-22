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

- [x] Mở PR `cuong` → `staging` ([#5](https://github.com/hygef-v4/AI-Powered-Learning-Platform/pull/5), merge vào `staging` ngày 2026-10-20 tại `6341aed`), giải xung đột với U04/U05 (file dùng chung: `SecurityConfig`, `ArchitectureTest`, `client.ts`, `app/admin/layout.tsx`, `pom.xml`, compose).
- [x] Sau khi có route `/classes`, `/manager` của U05/U04 trên `staging`: đổi `HomePaths.of` về `CLASS_DASHBOARD`, `MANAGER_DASHBOARD` (ghi chú trong code-summary).
- [x] Chạy lại toàn bộ test backend/frontend sau merge (backend 676, frontend 214 đạt). Còn: giao tester viết integration test U01.
- [x] Cập nhật Jira: 12 UC của U01 sang Done (2026-10-20). Còn: đóng KAN-18 khi tester xác nhận.

### Giai đoạn 1 — U10 phần ngân hàng câu hỏi: UC 46, 47 (KAN-143, KAN-78)

Theo plan U10 bước 1–3 và phần tương ứng của bước 9–11.

- [x] Migration `quizzes` (mọi bảng của U10 trong một migration, gồm bảng Quiz, để không phải sửa migration sau).
- [x] Domain `Question` (lineage, `DRAFT`/`ACTIVE`/`RETIRED`, kiểm số lựa chọn và đáp án theo loại), repository.
- [x] `QuestionBankService`: xem (F1), tạo, sửa, tạo bản nháp từ bản `ACTIVE`, kích hoạt, ngưng, xóa (F2).
- [x] `QuestionImporter`: nhập tệp ≤ 5 MB, ≤ 500 dòng, kết quả từng dòng (F3).
- [x] API `/subjects/{id}/questions`, `/questions/{id}`, `questions:import`; `contracts/openapi/quizzes.yaml` phần ngân hàng. (Theo D6: không tạo OpenAPI, API ghi trong code-summary.) Commit `5d70173`.
- [x] Frontend: `QuestionListPage`, `QuestionDetailPage`, `McqEditor`, `NavEntry` Ngân hàng câu hỏi.
- [x] Unit test phần ngân hàng; Jira KAN-143, KAN-78 → In Progress rồi review. Jira KAN-143, KAN-78 Done (2026-10-20).

### Giai đoạn 2 — U10 phần Quiz: UC 48 (KAN-44)

Theo plan U10 bước 4–6, 8 và phần còn lại của bước 9–11.

- [x] Domain `Quiz`, `QuizItem`; `QuizAuthoringService` (F5–F7, F12): danh sách, tạo, cài đặt, chọn tay và bốc ngẫu nhiên câu ngân hàng, câu riêng, xóa Quiz nháp.
- [x] `QuizPublicationService` (F9, F10): xem trước, duyệt, phát hành, ngưng; `IQuizLifecycleListener` adapter rỗng; phát `quiz.published` sau commit.
- [x] `QuizCopyService` (F11): nhân bản, version mới, copy giữa lớp.
- [x] `QuizQueryService` (cài `IQuizQuery`), `QuizScorer` (cài `IQuizScoring`) (F14) — thay stub.
- [x] API Quiz; frontend `SubjectQuizListPage`, `ClassQuizzesSection` (`TeacherClassTab`), `QuizBankSummary`, `QuizDetailPage` và panel; slot `QuizDetailSection`, `QuizRowAction`.
- [x] Unit test phần Quiz; review U10 không gồm AI/MCP. Commit `f3b54bc`; Jira KAN-44 Done (2026-10-20).

### Giai đoạn 3 — U11: UC 51, 52, 49, 50 (KAN-130, KAN-57, KAN-128, KAN-129)

Theo plan U11. Làm UC 51 trước vì các màn khác đều dựa vào lượt làm.

- [x] Migration `quizpractice`, domain `QuizAttempt` (trộn có seed).
- [x] UC 51: `QuizAttemptService` bắt đầu/tiếp tục, tự lưu có `answerVersion`, nộp và chấm (F3–F5); `QuizAutoSubmitScanner`, `QuizRetiredHandler` cài `IQuizLifecycleListener` (F6); frontend `QuizTakingPage`.
- [x] UC 52: kết quả theo cài đặt hiện điểm/đáp án (F7); `QuizResultPage`.
- [x] UC 49: lịch sử luyện tập (F1); `QuizPracticeHistoryPage`, `NavEntry` Luyện tập Quiz.
- [x] UC 50: chi tiết Quiz (F2); `QuizPracticeDetailPage`, slot `QuizMaterialSection`.
- [x] Dữ liệu hoạt động (F8) và giảng viên xem lượt (F9): `QuizAttemptsSection`, `QuizAttemptCount` gắn vào slot của U10. F8: chỉ có API, chưa có tracker phía trình duyệt (G3).
- [x] Unit test theo plan U11 bước 9; review U11. PR #6 merge vào `staging` (`c7a4791`); Jira KAN-128, KAN-129, KAN-130, KAN-57 Done (2026-10-20).

### Giai đoạn 4 — AI và MCP của U10 (phần còn lại của UC 47, 48)

- [x] `QuizAiDraftService`, `QuizDraftResultHandler` (`QUIZ_QUESTION_DRAFT`), `QuizMcpTools` (F4, F8, F13) trên contract U06. Plan chi tiết: [U10 giai đoạn 4](code-generation-plan/u10-quiz-authoring-code-generation-plan.md) (H1–H10), nhánh `quiz-ai-mcp`.
- [x] Bộ chọn tài liệu tham khảo (U10) và `MaterialQuizList` (U11, G4) trên U13 đã có ở `staging`. Code xong và được duyệt ngày 2026-10-22, commit local trên `quiz-ai-mcp`.
- [ ] Chạy thật khi U06 có code; trước đó chỉ test với stub.

Tình trạng 2026-10-22: contract U06 trên `staging` đã đủ cho bước này (U09 đã cài `IAiTaskResultHandler`, `IMcpToolProvider` theo cùng cách); U13 đã cài `IReferenceMaterial` trên `staging`. Code U06 (`AiTaskService`, `AiSourcePicker`) mới ở nhánh `ai-connections`, chưa merge.

### Giai đoạn 5 — Kiểm tra và đóng Jira

- [ ] Tester viết integration test theo mục "Integration test cases" của plan U10, U11.
- [x] Chạy thử end-to-end với Docker local: Chủ nhiệm môn soạn câu → giảng viên tạo và phát hành Quiz → sinh viên làm, nộp, xem kết quả → phát hành version mới tự nộp lượt đang làm. 5 kịch bản đạt (gồm tự nộp khi hết giờ); sửa `frontend/Dockerfile` qua PR #7 (`392b7d0`).
- [ ] Đóng KAN-23, KAN-25 và các UC con.

### Song song — KAN-152

- [ ] Rà phần V. Requirement Appendix của Report 3 theo 65 UC hiện hành; không ảnh hưởng code.

### Tình trạng 2026-10-22 (chiều)

`staging` mới nhất là `2fa57f9`, đã thêm U14 (thông báo, bảng tin lớp). Các nhánh chưa merge: `ai-connections` (U06, 19 commit), `assignment` (U07/U08, 5 commit), `group-assignment` (U12, 17 commit). Jira không đổi: KAN-18 In Progress, KAN-23 và KAN-25 To Do, KAN-152 In Progress.

**Đang chờ người khác:**

| Việc | Chờ ai | Ghi chú |
|---|---|---|
| Push `quiz-ai-mcp` và mở PR giai đoạn 4 | Nhóm sửa migration `grading` trên `staging` | `grading/V20260925_1500` của U09 tham chiếu bảng `assignments` của U07 chưa merge, nên `staging` không dựng được DB mới và CI backend đỏ |
| Chạy AI thật; thay `AiSourceSelect` bằng `AiSourcePicker` | U06 merge `ai-connections` | |
| Tracker hoạt động (G3), bật ghi nhận hoạt động (E3) | U08 merge `assignment` (có `useActivityTracker`, khóa `activity.retentionDays`) | |
| Integration test U01, U10, U11; đóng KAN-18, KAN-23, KAN-25 | Tester | |
| Chốt kết quả khi Quiz ẩn điểm, ẩn đáp án (BR-U11-20 khác API và UI) | Người viết requirement | Ghi ở code-summary U11, mục chạy thử end-to-end |

**Làm được ngay, không cần chờ:**

Plan chi tiết: [U11, mục "Sửa sau chạy thử và link thông báo Quiz"](code-generation-plan/u11-quiz-practice-code-generation-plan.md) (J1–J6).

- [x] Thông báo "Có Quiz mới" của U14 dẫn tới `/quiz-practice/{quizId}` không có `classId`, nên trang chi tiết U11 gọi API với `classId` rỗng và báo lỗi. Sửa phía U11: thiếu `classId` thì tự tìm lớp đang học có Quiz đó (một lớp thì mở luôn, nhiều lớp thì cho chọn).
- [x] Lỗi nhỏ từ lần chạy thử end-to-end:
  - Công tắc trong `QuizSettingsPanel` (U10) chưa có tên truy cập.
  - Ô chọn đáp án ở `QuizTakingPage` (U11) có tên truy cập "on".
  - Kết quả không cho biết lượt được tự nộp (`AUTO_TIME_LIMIT`, `AUTO_RETIRED`).
- [x] Cập nhật `quiz-ai-mcp` theo `staging` mới (merge `972d594`). Chỉ xung đột ở `aidlc-state.md`, để PR giai đoạn 4 sẵn sàng khi `staging` được sửa.
- [ ] KAN-152: chỉ là việc tài liệu nhưng Report 3 không có trong repo, và Jira không có mô tả hay tệp đính kèm. Cần người dùng gửi bản Report 3 (link Google Docs hoặc tệp).

## 3. Nhánh

- U01 giữ nhánh `cuong` tới khi merge vào `staging`.
- U10, U11 tạo nhánh mới từ `staging` sau khi U01 đã vào, để dùng code thật của U04/U05 thay vì stub.
