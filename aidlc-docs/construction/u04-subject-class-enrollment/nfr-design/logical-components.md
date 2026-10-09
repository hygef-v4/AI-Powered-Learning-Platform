# U04 Subject, Class, Enrollment & Learning Access - Logical Components

**Bản tài liệu 2026-10-09**: UC 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

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
