# U08 Assessment Core & Publication - Infrastructure Design

**Bản tài liệu 2026-10-08**: UC 38; primary stories: US-ASM-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AssignmentService`, `ScheduleService`, `AssignmentQueryService` | `backend` |
| `AssignmentScheduleScanner` | `worker` |
| Bảng `assignments`, `assignment_questions` | `postgres` |
| Event | exchange `platform.events`, routing key `assignment.opened` (chỉ cho thông báo U16); mở/ngưng giao báo U11, U14 qua `AssignmentLifecyclePort` trong transaction |

Container `backend`/`worker` đặt `TZ=UTC`; múi giờ hiển thị lấy từ `APP_TIMEZONE`.

## 2. Migration

`V20260925_1500__u08_assessment.sql` theo [mô hình dữ liệu của unit](../functional-design/domain-entities.md):
- `assignments`: FK `class_id` → `course_classes`, `subject_id` → `subjects`; CHECK đúng một trong `class_id`, `subject_id` khác NULL; CHECK `opens_at < closes_at`, `late_until IS NULL OR late_until > closes_at`; CHECK dạng/chế độ (`GROUP_ASSIGNMENT` chỉ `GRADED`); `config jsonb`; index `(class_id, status)`, `(subject_id, status)`, `(status, opens_at)`, `(status, closes_at)` cho scanner.
- `assignment_questions`: khóa chính `(assignment_id, question_id)`, FK tới `assignments`, `questions`; unique `(assignment_id, order_no)`.
- Cột `config` (U09), `subject_id`, `source_assignment_id` (U10), `grades_released_at` (U15), `reminder_sent_at` (U16) có sẵn trong bảng; unit đó ghi qua `AssignmentExtensionPort`. Không có cột Simulation Exam.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
