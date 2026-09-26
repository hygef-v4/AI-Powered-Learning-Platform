# U16 Reporting & Notification - NFR Design Patterns

## P1 - Fan-out thông báo theo lô
- `NotificationFanout.create(event, recipients[])`: một `INSERT ... SELECT unnest(...) ON CONFLICT DO NOTHING` cho thông báo và outbox; đọc preference một lần cho cả lô (NFR-U16-02).
- Sau commit: phát `u16.notification.created {recipientIds}` lên fanout realtime (dùng chung với U14) → `SseHub` đẩy tới kênh người dùng.

## P2 - Dispatcher email có trần và ưu tiên
- Job mỗi phút: `SELECT ... FROM email_outbox WHERE status IN ('QUEUED','DEFERRED') AND scheduled_for <= now() ORDER BY priority, created_at LIMIT 60 FOR UPDATE SKIP LOCKED`.
- Mỗi email: `INCR u16:email:{yyyyMMdd}`; vượt 300 → `DECR`, chuyển mọi mục còn lại sang `DEFERRED`, `scheduled_for = 00:05 ngày sau`.
- Tạo job gửi, cách nhau ≥ 1 s (Guava `RateLimiter` 1/s trong handler) (NFR-U16-03, 10).

## P3 - Gửi idempotent
- Handler gửi: chỉ gửi khi `status IN ('QUEUED','DEFERRED')` → gửi → `SENT`; lỗi tạm → ném để U02 retry; lỗi vĩnh viễn (địa chỉ sai) → `FAILED` + `DECR` bộ đếm.
- Nhắc hạn đã quá hạn nộp → `SKIPPED` (BR-U16-13).

## P4 - Nhắc hạn theo thời điểm kỳ vọng
- Job `U16_DEADLINE_REMINDER {publicationId, expectedClosesAt}`; chạy thì so với `closesAt` hiện tại (U08), khác → không nhắc và tạo job mới theo hạn mới nếu còn ở tương lai (NFR-U16-11).

## P5 - Báo cáo tiến độ một query
- Người học đang ghi danh (U04) LEFT JOIN trạng thái lượt (U11) hoặc bản nộp nhóm (U14) theo publication.

## P6 - Dashboard và xuất bảng điểm
- Dashboard tổng hợp theo accountId lấy từ phiên đăng nhập, chỉ đọc qua U04/U08/U11/U14/U15; không nhận accountId khác từ client. Phân bố dùng ngưỡng BR-U16-42 trước khi trả dữ liệu.
- Export kiểm quyền lớp/bài trước query; đọc dữ liệu từ `GradebookQueryPort` và stream CSV/XLSX trực tiếp. Chuỗi CSV bắt đầu bằng `=`, `+`, `-`, `@` được escape; không log nội dung tệp.
