# U04 Subject, Class, Enrollment & Learning Access - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller, service, `EnrollmentGuard`, `InviteCodeService`, `ScopeQueryService` | `backend` |
| Bảng `subjects`, `course_classes`, `enrollments` | `postgres` |
| Bộ đếm nhập sai mã mời | `redis`, khóa `ratelimit:invite-code:{accountId}`, TTL 1 giờ |
| Event `enrollment.activated` | RabbitMQ exchange `platform.events` của U03 |

U04 không chạy gì trong `worker`, không có queue riêng, không có secret riêng, không gọi dịch vụ ngoài.

## 2. Migration

`V20260925_1100__u04_subjects_classes_enrollments.sql`:
- `subjects`: unique `code`, FK `manager_id` → `accounts`, cột `version`.
- `course_classes`: unique `(subject_id, code)`, unique `invite_code` (cho phép rỗng), index `(subject_id, status)`, FK `teacher_id` → `accounts`, `show_grade_distribution BOOLEAN NOT NULL DEFAULT FALSE`, cột `version`.
- `enrollments` (bảng nối): khóa chính `(class_id, account_id)`, index `(account_id, status)`; người thực hiện ghi danh nằm trong audit.
- Quyền: user `app` được SELECT/INSERT/UPDATE, **không** DELETE trên ba bảng (không xóa lịch sử).

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-09 | N/A | Không có secret riêng |
| RESILIENCY-04, 06 | N/A | Dùng chung deploy và healthcheck của backend |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
