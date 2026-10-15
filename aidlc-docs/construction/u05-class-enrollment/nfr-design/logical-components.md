# U05 Class & Enrollment — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U05 Class & Enrollment thuộc Iter 1, người code: Người 5; UC 20–30. Tách phần lớp và ghi danh từ U04 cũ; mã `BR-U05-xx` ứng với `BR-U04-xx` cũ cùng số. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `StudentClassController`, `TeachingClassController`, `ManagerController` | backend | API mục 5 của domain-entities |
| `ClassViewService` | backend | F1–F6, F8 |
| `ClassAdminService` | backend | F7, F9, F13, F14 |
| `EnrollmentService` | backend | F10–F12, P2 |
| `ClassScopeService` | backend | Cài `ClassScopePort`, `SubjectClassesPort`, `ClassAccessPort` (F15) |
| `CourseClassRepository`, `EnrollmentRepository` | backend | Chỉ dùng trong package |

U05 không chạy gì trong worker, không có queue, không có secret, không gọi dịch vụ ngoài.
