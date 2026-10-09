# U04 Subject, Class, Enrollment & Learning Access - Infrastructure Design

**Bản tài liệu 2026-10-09**: UC 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller, service, `EnrollmentGuard`, `ScopeQueryService` | `backend` |
| Bảng `subjects`, `course_classes`, `enrollments` | `postgres` |
| Event `enrollment.activated` | RabbitMQ exchange `platform.events` của U03 |

U04 không chạy gì trong `worker`, không có queue riêng, không có secret riêng, không gọi dịch vụ ngoài.

## 2. Migration

`db/migration/academics/V20260925_1100__create_subjects_classes_enrollments.sql`:
- `subjects`: unique `code`, FK `manager_id` → `accounts`, cột `version`.
- `course_classes`: unique `(subject_id, code)`, index `(subject_id, status)`, FK `teacher_id` → `accounts`, `show_grade_distribution BOOLEAN NOT NULL DEFAULT FALSE`, cột `version`.
- `enrollments` (bảng nối): khóa chính `(class_id, account_id)`, index `(account_id, status)`; người thực hiện ghi danh nằm trong audit.
- Quyền: user `app` được SELECT/INSERT/UPDATE, **không** DELETE trên ba bảng (không xóa lịch sử).

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-09 | N/A | Không có secret riêng |
| RESILIENCY-04, 06 | N/A | Dùng chung deploy và healthcheck của backend |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
