# U14 Group Document & Submission - Deployment Architecture

**Bản tài liệu 2026-10-08**: UC 23; primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
 Trình duyệt ==SSE (HTTPS, không đệm)==> [nginx] ==> [backend: SseHub] <== queue riêng <== fanout platform.realtime
      |                                                   |                                   ^
      +--REST (nhận/lưu/xong/nộp)--> [nginx] --> [backend: U14] --sau commit--> fanout -----+
                                                          |                                   |
                                                          v                                   |
                                                    [postgres]            [worker: dựng tài liệu, tự nộp]
```

**Text alternative**: Trình duyệt giữ một kết nối SSE qua Nginx (tắt đệm, timeout 1 giờ) tới `SseHub` trong backend và gọi REST để nhận/lưu/xong mục và nộp. Sau mỗi thay đổi, backend (hoặc worker khi dựng tài liệu và tự nộp) gửi sự kiện lên fanout `platform.realtime` của RabbitMQ; mỗi backend nhận qua queue riêng và đẩy xuống trình duyệt. Dữ liệu nằm trong PostgreSQL.
