# U08 Assessment Core & Publication - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `AssignmentService`, `PublicationService`, `AssignmentQueryService` | `backend` |
| `PublicationScheduleHandler` | `worker` |
| Bảng `assignments`, `assignment_components`, `publications` | `postgres` |
| Queue | `jobs.scheduled` (`PUBLICATION_OPEN`, `PUBLICATION_CLOSE`) |
| Event | exchange `platform.events`, routing key `assignment.opened` (chỉ cho thông báo U16); mở/ngưng giao báo U11, U14 qua `PublicationLifecyclePort` trong transaction |

Container `backend`/`worker` đặt `TZ=UTC`; múi giờ hiển thị lấy từ `APP_TIMEZONE`.

## 2. Migration

`V20260925_1500__u08_assessment.sql`:
- `assignments` (`version`, `total_points numeric(6,2)`, `inline` qua bảng thành phần), index `(scope_type, scope_id, status)`.
- `assignment_components` với `CHECK ((bank_item_id IS NULL) <> (inline_definition IS NULL))`, unique `(assignment_id, sequence_no)`.
- Cột do unit khác ghi được chính unit đó thêm bằng `ALTER TABLE` trong migration của mình: `assignments.type_config`, `assignments.skeleton` (U09); `assignments.lineage_kind`, `source_class_id`, `lineage_actor_id`, `lineage_at` (U10, dùng cùng `source_assignment_id` của U08); `publications.simulation_policy`, `policy_locked_at` (U10); `publications.grades_released_by`, `grades_released_at` (U15).
- `publications` với `CHECK (opens_at < closes_at)`, `CHECK (NOT allow_late OR late_until > closes_at)`, partial unique `(assignment_id, class_id) WHERE status <> 'RETIRED'`, index `(class_id, status)`.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
