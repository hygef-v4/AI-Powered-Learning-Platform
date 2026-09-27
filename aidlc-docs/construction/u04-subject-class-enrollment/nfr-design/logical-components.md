# U04 Subject, Class, Enrollment & Learning Access - Logical Components

## 1. Sơ đồ

```
 Trình duyệt (admin, giảng viên, người học)
   |
   v
 +-------------------------------- backend --------------------------------+
 | SubjectController   ClassController   EnrollmentController   MeController|
 |        |                  |                  |                   |        |
 |        v                  v                  v                   v        |
 | SubjectService      ClassService      EnrollmentService   LearnerClassService
 |        |                  |             |       |                 |      |
 |        |                  |     EnrollmentGuard  InviteCodeService |      |
 |        |                  |      (advisory lock)  (Bucket4j/Redis)  |      |
 |        +--------+---------+-------------+---------------------------+      |
 |                 v                                                          |
 |       Repository (PostgreSQL: subjects, classes, enrollments)              |
 |                                                                            |
 | ScopeQueryService --> SubjectScopePort, ClassScopePort (cho U01)           |
 |                   --> ClassAccessPort (cho U05, U06, U08-U12, U14-U16)     |
 | Dùng: AuthorizationPort, AccountLookupPort (U01); PublishedContentPort     |
 |       (U05); AuditPort, EventPublisherPort (U02)                           |
 +----------------------------------------------------------------------------+
```

**Text alternative**: Bốn controller nhận yêu cầu của admin, giảng viên và người học, gọi service tương ứng. `EnrollmentService` dùng `EnrollmentGuard` để khóa theo người học và môn, `InviteCodeService` kiểm rate limit trên Redis. Mọi service ghi PostgreSQL qua repository. `ScopeQueryService` cung cấp port kiểm phạm vi cho U01 và các unit khác. U04 dùng U01 để kiểm quyền và tra tài khoản, U05 để lấy nội dung đã phát hành, U02 để ghi audit và phát event.

## 2. Thành phần

| Thành phần | Trách nhiệm |
|---|---|
| `SubjectService` | F1 |
| `ClassService` | F2, F3; P4 |
| `EnrollmentService` | F4-F6; P3, P5 |
| `EnrollmentGuard` | P2 |
| `InviteCodeService` | F7; P6 |
| `LearnerClassService` | F8; P7, P8 |
| `ScopeQueryService` | F9; P1 |

## 3. Cấu hình

| Khóa | Mặc định |
|---|---|
| `U04_BULK_MAX_ROWS` | 200 |
| `U04_BULK_MAX_BYTES` | 100KB |
| `U04_INVITE_DEFAULT_DAYS` | 7 |
| `U04_INVITE_MAX_DAYS` | 30 |
| `U04_INVITE_FAIL_PER_HOUR` | 10 |

## 4. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log email |
| SECURITY-05 | Compliant | P3 kiểm đầu vào |
| SECURITY-08 | Compliant | P7 |
| SECURITY-15 | Compliant | P6, P8 fail closed |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
