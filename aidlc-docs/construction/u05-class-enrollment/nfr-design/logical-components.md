# U05 Class & Enrollment — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30. **Primary stories:** US-CAT-002, US-CAT-003, US-LRN-001; phần lớp của US-CAT-001. **Requirements:** FR-002, FR-003, FR-005.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `StudentClassController`, `TeachingClassController`, `ManagerController` | backend | API mục 5 của domain-entities |
| `ClassViewService` | backend | F1–F6, F8 |
| `ClassAdminService` | backend | F7, F9, F13, F14 |
| `EnrollmentService` | backend | F10–F12, P2 |
| `ClassScopeService` | backend | Cài `IClassScope`, `ISubjectClasses`, `IClassAccess` (F15) |
| `ICourseClassRepository`, `IEnrollmentRepository` | backend | Chỉ dùng trong package |

U05 không chạy gì trong worker, không có queue, không có secret, không gọi dịch vụ ngoài.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
