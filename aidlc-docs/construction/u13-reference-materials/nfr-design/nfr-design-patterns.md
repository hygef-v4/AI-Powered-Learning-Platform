# U13 Reference Materials — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Phiên bản chỉ thêm
- Thay tệp hoặc link chèn dòng `material_versions` mới và tăng `currentVersionNo`; không sửa, không xóa phiên bản cũ.

## P2 - Trích chữ nền
- Job mang (`materialId`, `versionNo`); bỏ qua khi phiên bản đã có kết quả; giới hạn thời gian mỗi tệp.

## P3 - Kiểm quyền trước token
- U13 kiểm quyền xem rồi mới gọi `issueDownloadToken`; không lộ `fileId` ra frontend.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
