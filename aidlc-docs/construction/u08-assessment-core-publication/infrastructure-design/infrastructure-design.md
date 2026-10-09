# U08 Assessment Core & Publication - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 41 và vòng đời (tạo, xóa, duyệt, phát hành, lịch, ngưng giao, nhân bản, version) của UC 35, 42, 43, 44, 45 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-ASM-001, US-ASM-008. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AssignmentService`, `ScheduleService`, `AssignmentQueryService` | `backend` |
| `AssignmentScheduleScanner` | `worker` |
| Bảng `assignments`, `assignment_questions` | `postgres` |
| Event | exchange `platform.events`, routing key `assignment.opened` (chỉ cho thông báo U16); mở/ngưng giao báo U11, U14 qua `AssignmentLifecyclePort` trong transaction |

Container `backend`/`worker` đặt `TZ=UTC`; múi giờ hiển thị lấy từ `APP_TIMEZONE`.

## 2. Migration

`db/migration/assessments/V20260925_1500__create_assignments.sql` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md) (chưa áp dụng nên sửa trực tiếp):
- `assignments`: FK `class_id` → `course_classes`, `subject_id` → `subjects`, `lesson_id` → `lessons`; CHECK đúng một trong `class_id`, `subject_id` khác NULL; CHECK quiz (`MULTIPLE_CHOICE_QUIZ`) có `lesson_id`, `PRACTICE`, `closes_at IS NULL`; CHECK bài tập không có `lesson_id`, `opens_at < closes_at`, `late_until IS NULL OR late_until > closes_at`; CHECK `GROUP_ASSIGNMENT` chỉ `GRADED` và chỉ khi có `class_id`; `config jsonb`; index `(class_id, status)`, `(subject_id, status)`, `(lesson_id, status)`, `(status, opens_at)`, `(status, closes_at)` cho scanner.
- `assignment_questions`: khóa chính `(assignment_id, question_id)`, FK tới `assignments`, `questions`; unique `(assignment_id, order_no)`.
- FK `rubrics.assignment_id` → `assignments` (bảng `rubrics` của U06 tạo trước).
- Cột `config` (U09), `source_assignment_id` (U08: nhân bản, version, copy giữa lớp), `reminder_sent_at` (U16) có sẵn trong bảng; unit đó ghi qua `AssignmentExtensionPort`. Không có cột `grades_released_at` (U15 suy thời điểm công bố theo bài và lớp từ `evaluations`). Không có cột Simulation Exam.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
