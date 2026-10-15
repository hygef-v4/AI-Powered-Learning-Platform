# U08 Assignment Work — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Thiết kế: [business-logic-model](../../u08-assignment-work/functional-design/business-logic-model.md), [business-rules](../../u08-assignment-work/functional-design/business-rules.md), [domain-entities](../../u08-assignment-work/functional-design/domain-entities.md), [frontend-components](../../u08-assignment-work/functional-design/frontend-components.md), [NFR](../../u08-assignment-work/nfr-requirements/nfr-requirements.md), [NFR design](../../u08-assignment-work/nfr-design/nfr-design-patterns.md), [infrastructure](../../u08-assignment-work/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan mới, chưa được duyệt và chưa code. Thay phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của plan U11 cũ.

## Code hiện có

| Path | Hiện trạng | Việc trong plan này |
|---|---|---|
| `backend/.../attempts/` (`AttemptQueryService`, `DraftSaver`, `AssignmentLifecycleAdapter`, port `SubmissionQueryPort`, `SubmissionSubmittedPort`, `SubmissionView`, `AttemptStatus`, `PracticeResultPort`, `PracticeResult`, `AttemptRunResultPort`) | Khung cũ, chưa có test | Tạo package `edu.aiplatform.assignmentwork`; chuyển `SubmissionQueryPort`, `SubmissionView`; đổi `SubmissionSubmittedPort` thành `SubmissionListener`; xóa `AttemptRunResultPort` (Code Lab đã bỏ); `PracticeResultPort` của Quiz để plan U11 |
| `backend/.../grading/adapter/SubmissionSubmittedAdapter`, `PracticeResultAdapter` | Cài port cũ | Xóa; U09 cài `SubmissionListener` khi làm |
| `contracts/openapi/attempts.yaml` | Contract cũ | Thay bằng `contracts/openapi/assignment-work.yaml` |

## Checklist

- [ ] **Bước 1 — Dọn code cũ:** theo bảng trên; code biên dịch được.
- [ ] **Bước 2 — Migration:** `db/migration/assignmentwork/V20260925_1400__create_assignment_work_tables.sql` theo infrastructure-design.
- [ ] **Bước 3 — Domain:** `AssignmentAttempt`, `PracticeFeedback`, `ActivityEvent`, `ActivitySummary` và enum; repository chỉ dùng trong package.
- [ ] **Bước 4 — Xem bài:** `StudentAssignmentQueryService` (F1, F2); contract `GroupWorkStatusPort` với adapter rỗng.
- [ ] **Bước 5 — Làm và nộp:** `AttemptService` (F3, F4, F6, P1, P2), `SubmissionListener` với adapter rỗng, `AutoSubmitScanner`, `AssignmentRetiredHandler` (F7).
- [ ] **Bước 6 — DOCX:** nhập, xuất qua `DocxPort` của U07 (F5).
- [ ] **Bước 7 — Lịch sử nộp:** `SubmissionHistoryService` (F8).
- [ ] **Bước 8 — Phản hồi Practice:** `PracticeFeedbackService`, `PracticeFeedbackResultHandler` (cài `AiTaskResultHandler` cho `PRACTICE_FEEDBACK`), `AssignmentWorkMcpTools` (cài `McpToolProvider`), giới hạn tần suất (F9, F10, P3).
- [ ] **Bước 9 — Dữ liệu hoạt động:** `ActivityService` (cài `ActivityWritePort`, `ActivityQueryPort`), `ActivitySummaryCalculator`, `ActivityRetentionScanner`, `ActivitySettingDefinitions` (F11, F12, P4, P5).
- [ ] **Bước 10 — Port cho unit khác:** `SubmissionQueryService` (cài `SubmissionQueryPort`) gồm `listStudentsWithoutSubmission` (F13).
- [ ] **Bước 11 — API:** controller theo domain-entities mục 5; `contracts/openapi/assignment-work.yaml`; ngoài phạm vi trả `404`.
- [ ] **Bước 12 — Frontend:** `StudentAssignmentsPage`, `ClassAssignmentsCard` (đăng ký `StudentClassSection`), mục Student Assignments (đăng ký `DashboardNavEntry`), `StudentAssignmentDetailPage`, `AssignmentWorkspacePage`, `SubmissionHistoryPage`, `PracticeFeedbackPanel`; slot `StudentAssignmentResult`, `SubmissionResultSection`, `GroupWorkEntry`, `AssignmentMaterialSection`; `useActivityTracker`, `ActivityTimeline`.
- [ ] **Bước 13 — Unit test (người code viết):** điều kiện bắt đầu (ghi danh, lớp, hạn, hết lượt, bài nhóm); một lượt đang làm; lưu lệch `contentVersion`; lưu sau hạn và trong 30 giây ân hạn; nộp thiếu sơ đồ bắt buộc; nộp hai lần trả cùng biên nhận; nộp trễ; tự nộp khi hết hạn và khi ngưng giao; lượt được chấm của bài `GRADED`; phản hồi bị từ chối với bài `GRADED`, lượt đang làm, quá giới hạn; phản hồi hash lệch không lưu; sự kiện trùng, lệch thứ tự, thiếu heartbeat thành khoảng "không rõ"; chính sách tắt không thu; dọn theo thời hạn; component frontend. Integration test do tester viết.
- [ ] **Bước 14 — Review:** đối chiếu UC 37–41, story, BR-U08 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Hai tab lưu cùng một lượt: tab sau nhận `409`, không mất bản đã lưu.
- Hết hạn khi đang làm: scanner tự nộp đúng một lần, U09 nhận `onSubmitted`.
- Ngưng giao bài khi có lượt đang làm: lượt được nộp `AUTO_RETIRED` trong cùng transaction.
- Xin phản hồi bằng API cá nhân giả và qua MCP; xin trên bài `GRADED` bị từ chối.
- Phân quyền 4 role trên mọi endpoint, gồm dùng ID lượt của người khác và người học đã bị gỡ khỏi lớp.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
