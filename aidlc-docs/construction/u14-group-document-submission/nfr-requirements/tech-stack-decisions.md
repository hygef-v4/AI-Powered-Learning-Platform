# U14 Group Document & Submission - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Realtime | Server-Sent Events bằng Spring MVC `SseEmitter` (async, không giữ thread) | Chỉ cần server → client; đơn giản hơn WebSocket |
| Phát sự kiện | Sự kiện sau commit → RabbitMQ fanout `platform.realtime` → mỗi backend đẩy tới emitter của mình (worker cũng phát được, ví dụ tự nộp) | Không phụ thuộc số instance |
| Client | `EventSource` với tự kết nối lại | Có sẵn trong trình duyệt |
| Lưu | PostgreSQL `jsonb` cho block (mô hình U09) | Như U11 |
