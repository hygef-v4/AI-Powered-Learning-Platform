# U04 Subject Administration — logical-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U04 Subject Administration thuộc Iter 1, người code: Người 4; UC 16–19. Tách phần môn học từ U04 cũ (Subject, Class & Enrollment); phần lớp và ghi danh thuộc U05. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `SubjectAdminController` | backend | API mục 4 của domain-entities |
| `SubjectAdminService` | backend | F1–F6, audit |
| `SubjectScopeService` | backend | Cài `SubjectScopePort` (F7) |
| `SubjectQueryService` | backend | Cài `SubjectQueryPort` (F8) |
| `SubjectRepository` | backend | Bảng `subjects`, chỉ dùng trong package |
| `UnavailableSubjectClassesAdapter` | backend | Adapter tạm của `SubjectClassesPort` tới khi U05 xong |

U04 không chạy gì trong worker, không có queue, không có secret, không gọi dịch vụ ngoài.
