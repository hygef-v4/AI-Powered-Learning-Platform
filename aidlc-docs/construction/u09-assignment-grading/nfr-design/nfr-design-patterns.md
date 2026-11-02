# U09 Assignment Grading — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Điểm tính từ checklist
- Điểm phần lấy từ `IRubric.score` với mục đạt; điểm bài là tổng các phần cộng điều chỉnh, kẹp trong 0 tới tổng; không nhận điểm từ client.

## P2 - Hàng loạt từng mục
- Chốt, công bố, AI hàng loạt xử lý từng mục trong transaction riêng, trả kết quả từng mục.

## P3 - Đề xuất gắn bài nộp
- Đề xuất lưu `submissionHash`; khi nhận và khi dùng đều so với đánh giá; lệch thì `STALE`.

## P4 - Lịch sử chỉ thêm
- Mọi thay đổi sau `DRAFT` thêm phần tử vào `history`; không sửa phần tử cũ; đồng thời ghi audit.

## P5 - Sổ điểm tính khi đọc
- Một truy vấn đánh giá theo lớp, ghép với danh sách bài và sinh viên trong bộ nhớ; không lưu bảng tổng hợp.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
