# U16 Reporting & Notification - NFR Design Patterns

**Bản tài liệu 2026-10-08**: UC 11, 57; primary stories: US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Fan-out thông báo theo lô
- `NotificationFanout.create(event, recipients[])`: một `INSERT ... SELECT unnest(...) ON CONFLICT DO NOTHING` cho thông báo (kèm `email_status`); đọc `email_preferences` một lần cho cả lô (NFR-U16-02); sau commit gửi việc `EMAIL_SEND` cho các dòng `QUEUED`.
- Sau commit: phát `notification.created {recipientIds}` lên fanout `platform.realtime` (dùng chung với U14) → `SseHub` đẩy tới kênh người dùng.

## P2 - Email có trần và ưu tiên
- Việc `EMAIL_SEND` đi trên `jobs.email` (priority queue: nhắc hạn > bài mới mở > điểm > ghi danh); consumer prefetch 1, Guava `RateLimiter` 1/s (NFR-U16-03, 10).
- Mỗi email: `INCR email:daily-count:{yyyyMMdd}`; vượt 300 → `DECR`, `email_status = DEFERRED`, ack việc.
- `EmailDeferredScanner` lúc 00:05: `UPDATE notifications SET email_status='QUEUED' WHERE email_status='DEFERRED' RETURNING id` theo ưu tiên, thời điểm tạo, rồi gửi lại việc.

## P3 - Gửi idempotent
- Handler: `UPDATE ... WHERE id=? AND email_status IN ('QUEUED','DEFERRED')` khóa dòng → gửi → `SENT`; lỗi tạm → ném để U03 retry; lỗi vĩnh viễn (địa chỉ sai) → `FAILED` + `DECR` bộ đếm. Việc trùng thấy `SENT` thì bỏ qua.
- Nhắc hạn đã quá hạn nộp lúc gửi → `SKIPPED` (BR-U16-13).

## P4 - Nhắc hạn bằng scanner
- `DeadlineReminderScanner` mỗi phút theo điều kiện ở `domain-entities.md` §5; tạo thông báo và ghi `reminder_sent_at` trong một transaction, khóa dòng bài bằng `FOR UPDATE SKIP LOCKED` nên hai worker không nhắc trùng (NFR-U16-11).

## P5 - Báo cáo tiến độ một query
- Người học đang ghi danh (U04) LEFT JOIN trạng thái lượt (U11) hoặc bản nộp nhóm (U14) theo bài.

## P6 - Thống kê, phân bố điểm và xuất bảng điểm
- Thống kê chỉ cho `ADMIN`, đếm bằng truy vấn `COUNT ... GROUP BY` qua port U01/U04, không trả hàng dữ liệu cá nhân. Phân bố điểm lấy accountId từ phiên đăng nhập, kiểm ghi danh, dùng ngưỡng BR-U16-42 trước khi trả dữ liệu.
- Export kiểm quyền lớp/bài trước query; đọc dữ liệu từ `GradebookQueryPort` và stream CSV/XLSX trực tiếp. Chuỗi CSV bắt đầu bằng `=`, `+`, `-`, `@` được escape; không log nội dung tệp.
