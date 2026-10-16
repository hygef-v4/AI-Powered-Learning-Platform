# U13 Reference Materials — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Phiên bản chỉ thêm
- Thay tệp hoặc link chèn dòng `material_versions` mới và tăng `currentVersionNo`; không sửa, không xóa phiên bản cũ.

## P2 - Trích chữ nền
- Job mang (`materialId`, `versionNo`); bỏ qua khi phiên bản đã có kết quả; giới hạn thời gian mỗi tệp.

## P3 - Kiểm quyền trước token
- U13 kiểm quyền xem rồi mới gọi `issueDownloadToken`; không lộ `fileId` ra frontend.
