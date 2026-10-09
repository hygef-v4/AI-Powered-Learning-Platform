# U14 Group Document & Submission - Deployment Architecture

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
+----------------------------+
| Trình duyệt (thành viên,   |
| giảng viên chính của lớp)  |
+----------------------------+
      |  REST       ^  SSE
      v             |
+----------------------------+
| nginx (SSE không đệm)      |
+----------------------------+
      |             ^
      v             |
+----------------------------+      +----------------------------+
| backend: U14 REST, SseHub  |----->| postgres: group_documents  |
+----------------------------+      +----------------------------+
      |  phát       ^  queue riêng                ^
      v             |                             |
+----------------------------+      +----------------------------+
| RabbitMQ fanout            |<-----| worker: dựng tài liệu,     |
| platform.realtime          |      | tự nộp                     |
+----------------------------+      +----------------------------+
```

**Text alternative**: Trình duyệt giữ một kết nối SSE qua Nginx (tắt đệm, timeout 1 giờ) tới `SseHub` trong backend và gọi REST để nhận/giao/lưu/xong/nhả mục và nộp. Backend đọc ghi bảng `group_documents` trong PostgreSQL. Sau mỗi thay đổi, backend (hoặc worker khi dựng tài liệu và tự nộp) gửi sự kiện lên fanout `platform.realtime` của RabbitMQ; mỗi backend nhận qua queue riêng và đẩy xuống trình duyệt. Worker cũng ghi `group_documents` khi dựng tài liệu và tự nộp.

U14 không thêm container hay volume.
