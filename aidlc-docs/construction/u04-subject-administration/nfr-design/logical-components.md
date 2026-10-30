# U04 Subject Administration — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 16, 17, 18, 19. **Primary stories:** US-CAT-001 (phần môn). **Requirements:** FR-002, FR-003.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md).

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `SubjectAdminController` | backend | API mục 4 của domain-entities |
| `SubjectAdminService` | backend | F1–F6, audit |
| `SubjectScopeService` | backend | Cài `ISubjectScope` (F7) |
| `SubjectQueryService` | backend | Cài `ISubjectQuery` (F8) |
| `ISubjectRepository` | backend | Bảng `subjects`, chỉ dùng trong package |
| `SubjectsContractStubs` (khung) | backend | Bản tạm `ISubjectClasses` tới khi U05 xong: danh sách rỗng, số lớp 0 |

U04 không chạy gì trong worker, không có queue, không có secret, không gọi dịch vụ ngoài.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
