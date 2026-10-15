# U09 Assignment Grading — nfr-design-patterns

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U09 Assignment Grading thuộc Iter 2, người code: Người 2; UC 42–45. Lấy phần chấm Assignment và sổ điểm của U15 cũ và phần tiến độ nộp, phân bố điểm, xuất bảng điểm của U16 cũ. Kết quả Quiz luyện tập thuộc U11, thông báo thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-RPT-001, US-RPT-003, US-INT-001. **Requirements:** FR-008, FR-009, FR-019, FR-020, FR-024, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## P1 - Điểm tính từ checklist
- Điểm phần lấy từ `RubricPort.score` với mục đạt; điểm bài là tổng các phần cộng điều chỉnh, kẹp trong 0 tới tổng; không nhận điểm từ client.

## P2 - Hàng loạt từng mục
- Chốt, công bố, AI hàng loạt xử lý từng mục trong transaction riêng, trả kết quả từng mục.

## P3 - Đề xuất gắn bài nộp
- Đề xuất lưu `submissionHash`; khi nhận và khi dùng đều so với đánh giá; lệch thì `STALE`.

## P4 - Lịch sử chỉ thêm
- Mọi thay đổi sau `DRAFT` thêm phần tử vào `history`; không sửa phần tử cũ; đồng thời ghi audit.

## P5 - Sổ điểm tính khi đọc
- Một truy vấn đánh giá theo lớp, ghép với danh sách bài và sinh viên trong bộ nhớ; không lưu bảng tổng hợp.
