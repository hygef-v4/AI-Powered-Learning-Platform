# U08 Assessment Core & Publication - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AssignmentService`, `PublicationService`, `AssignmentQueryService` | `backend` |
| `PublicationScheduleHandler` | `worker` |
| Bảng `assignments`, `assignment_components`, `publications` | `postgres` |
| Queue | `jobs.u08.publication-open`, `jobs.u08.publication-close` |
| Event | exchange `platform.events`, routing key `u08.assignment.opened`, `u08.assignment.closed`, `u08.assignment.retired` |

Container `backend`/`worker` đặt `TZ=UTC`; múi giờ hiển thị lấy từ `APP_TIMEZONE`.

## 2. Migration

`V20260925_1500__u08_assessment.sql`:
- `assignments` (`version`, `total_points numeric(6,2)`, `inline` qua bảng thành phần), index `(scope_type, scope_id, status)`.
- `assignment_components` với `CHECK ((bank_item_id IS NULL) <> (inline_definition IS NULL))`, unique `(assignment_id, sequence_no)`.
- `publications` với `CHECK (opens_at < closes_at)`, `CHECK (NOT allow_late OR late_until > closes_at)`, partial unique `(assignment_id, class_id) WHERE status <> 'RETIRED'`, index `(class_id, status)`.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
