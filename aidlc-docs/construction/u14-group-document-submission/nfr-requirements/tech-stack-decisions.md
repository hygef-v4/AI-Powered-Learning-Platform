# U14 Group Document & Submission - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Realtime | Server-Sent Events bằng Spring MVC `SseEmitter` (async, không giữ thread) | Chỉ cần server → client; đơn giản hơn WebSocket |
| Phát sự kiện | Sự kiện sau commit → RabbitMQ fanout `platform.realtime` → mỗi backend đẩy tới emitter của mình (worker cũng phát được, ví dụ tự nộp) | Không phụ thuộc số instance |
| Client | `EventSource` với tự kết nối lại | Có sẵn trong trình duyệt |
| Lưu | PostgreSQL `jsonb` cho block (mô hình U09) | Như U11 |
