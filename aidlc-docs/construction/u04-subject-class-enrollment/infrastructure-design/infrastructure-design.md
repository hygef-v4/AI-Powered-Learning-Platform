# U04 Subject, Class & Enrollment — infrastructure-design

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 16, 17, 18, 19. **Primary stories:** US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. **Requirements:** FR-002/003/005/027.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u04-subject-class-enrollment` không quyết định chức năng hiện hành.

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

## Quy tắc tích hợp revision hiện hành

- Admin CRUD/archive subjects và gán Subject Manager ACTIVE, chỉ xem class list metadata; không teacher hoặc manager assignment cho Admin.
- Subject Manager của môn R2 tạo/lifecycle lớp, phân Teacher/Subject Manager ACTIVE và thêm/gỡ Student; Teacher chỉ xem lớp được giao, không enrollment/lifecycle.
- Enrollment ACTIVE cấp R5, khi gỡ không xóa lịch sử bài/điểm nhưng deny thao tác tiếp theo. Mỗi sinh viên tối đa một lớp hiệu lực của một môn theo baseline.
- Scope reader cho API/MCP/activity cùng current authority; group membership và teacher assignment kiểm mỗi call, không role-only.
- Archived resource chỉ đọc lịch sử theo quyền; không tiếp tục edit/publish/enroll.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Admin không dạy/chấm, manager không assigned teacher không được chấm, Teacher không enrollment edit.
- Concurrent enrollment/assignment update có version; revoke enrollment deny next API/MCP/activity call; giữ lịch sử.
