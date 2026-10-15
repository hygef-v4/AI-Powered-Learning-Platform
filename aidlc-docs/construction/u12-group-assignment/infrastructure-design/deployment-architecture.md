# U12 Group Assignment — deployment-architecture

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

```
browser --REST--> nginx --> backend (package groups, groupdocs) --> postgres (nhóm, tài liệu nhóm)
browser <--SSE--- nginx <-- backend <-- RabbitMQ fanout platform.realtime <-- backend (instance khác)
backend --> U07 (khung, kiểm tài liệu) ; backend --> U09 (đánh giá khi nộp) ; backend --> U08 (dữ liệu hoạt động)
worker (tạo tài liệu nhóm, tự nộp) --> postgres
```

**Text alternative**: Giảng viên và sinh viên dùng trình duyệt gọi REST qua Nginx tới backend để quản lý nhóm, làm và nộp tài liệu nhóm; trình duyệt mở kênh SSE để nhận cập nhật. Instance backend nào xử lý thay đổi cũng gửi sự kiện lên exchange fanout của RabbitMQ để mọi instance đẩy tới người đang mở tài liệu. Backend dùng U07 cho khung và kiểm tài liệu, gọi U09 khi nộp, ghi dữ liệu hoạt động qua U08. Worker tạo tài liệu nhóm khi bài mở và tự nộp khi hết hạn.
