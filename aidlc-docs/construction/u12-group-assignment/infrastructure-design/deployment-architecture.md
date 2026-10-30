# U12 Group Assignment — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --REST--> nginx --> backend (package groups, groupdocs) --> postgres (nhóm, tài liệu nhóm)
browser <--SSE--- nginx <-- backend <-- RabbitMQ fanout platform.realtime <-- backend (instance khác)
backend --> U07 (khung, kiểm tài liệu) ; backend --> U09 (đánh giá khi nộp) ; backend --> U08 (dữ liệu hoạt động)
worker (tạo tài liệu nhóm, tự nộp) --> postgres
```

**Text alternative**: Giảng viên và sinh viên dùng trình duyệt gọi REST qua Nginx tới backend để quản lý nhóm, làm và nộp tài liệu nhóm; trình duyệt mở kênh SSE để nhận cập nhật. Instance backend nào xử lý thay đổi cũng gửi sự kiện lên exchange fanout của RabbitMQ để mọi instance đẩy tới người đang mở tài liệu. Backend dùng U07 cho khung và kiểm tài liệu, gọi U09 khi nộp, ghi dữ liệu hoạt động qua U08. Worker tạo tài liệu nhóm khi bài mở và tự nộp khi hết hạn.
