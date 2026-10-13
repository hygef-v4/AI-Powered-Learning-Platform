# U04 Subject, Class & Enrollment — logical-components

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67. **Primary stories:** US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. **Requirements:** FR-002/003/005/027.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u04-subject-class-enrollment` không quyết định chức năng hiện hành.

## 1. Sơ đồ

```
 Trình duyệt (admin, giảng viên, người học)
   |
   v
 +-------------------------------- backend --------------------------------+
 | SubjectController   ClassController   EnrollmentController   MeController|
 |        |                  |                  |                   |        |
 |        v                  v                  v                   v        |
 | SubjectService      ClassService      EnrollmentService   StudentClassService
 |        |                  |             |       |                 |      |
 |        |                  |     EnrollmentGuard                    |      |
 |        |                  |      (advisory lock)                   |      |
 |        +--------+---------+-------------+---------------------------+      |
 |                 v                                                          |
 |       Repository (PostgreSQL: subjects, course_classes, enrollments)       |
 |                                                                            |
 | ScopeQueryService --> SubjectScopePort, ClassScopePort (cho U01)           |
 |                   --> ClassAccessPort (cho U05, U06, U08-U16)              |
 | Dùng: AuthorizationPort, AccountLookupPort (U01); PublishedContentPort     |
 |       (U05); AuditPort (U02), EventPublisherPort (U03)                           |
 +----------------------------------------------------------------------------+
```

**Text alternative**: Bốn controller nhận yêu cầu của admin, giảng viên và người học, gọi service tương ứng. `EnrollmentService` dùng `EnrollmentGuard` để khóa theo người học và môn. Mọi service ghi PostgreSQL qua repository. `ScopeQueryService` cung cấp port kiểm phạm vi cho U01 và các unit khác. U04 dùng U01 để kiểm quyền và tra tài khoản, U05 để lấy học liệu đang hiển thị, U02 để ghi audit và U03 để phát event.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `SubjectService` | F1 |
| `ClassService` | F2, F3; P4 |
| `EnrollmentService` | F4-F6; P3, P5 |
| `EnrollmentGuard` | P2 |
| `StudentClassService` | F8, F10 (Class Dashboard, Student/Teacher Class Detail, Student Detail); P7, P8 |
| `ManagerViewService` | F11, F12 (Manager Dashboard, Subject Detail, Class List của Subject Manager) |
| `ScopeQueryService` | F9; P1 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U04_BULK_MAX_ROWS` | 200 |
| `U04_BULK_MAX_BYTES` | 100KB |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log email |
| SECURITY-05 | Compliant | P3 kiểm đầu vào |
| SECURITY-08 | Compliant | P7 |
| SECURITY-15 | Compliant | P8 fail closed |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |

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
