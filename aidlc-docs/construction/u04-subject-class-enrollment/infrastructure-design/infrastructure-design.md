# U04 Subject, Class, Enrollment & Learning Access - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller, service, `EnrollmentGuard`, `InviteCodeService`, `ScopeQueryService` | `backend` |
| Bảng `subjects`, `classes`, `enrollments` | `postgres` |
| Bộ đếm nhập sai mã mời | `redis`, khóa `u04:invite-fail:{accountId}`, TTL 1 giờ |
| Event `ENROLLMENT_ACTIVATED` | RabbitMQ exchange `platform.events` của U02, routing key `u04.enrollment.activated` |

U04 không chạy gì trong `worker`, không có queue riêng, không có secret riêng, không gọi dịch vụ ngoài.

## 2. Migration

`V20260925_1100__u04_subjects_classes_enrollments.sql`:
- `subjects`: unique `code`, index `manager_account_id`, cột `version`.
- `classes`: unique `(subject_id, code)`, unique `invite_code` (cho phép rỗng), index `(subject_id, status)`, `instructor_account_id`, cột `version`.
- `enrollments`: unique `(class_id, learner_account_id)`, index `(learner_account_id, status)`.
- Quyền: user `app` được SELECT/INSERT/UPDATE, **không** DELETE trên ba bảng (không xóa lịch sử).

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-09 | N/A | Không có secret riêng |
| RESILIENCY-04, 06 | N/A | Dùng chung deploy và healthcheck của backend |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
