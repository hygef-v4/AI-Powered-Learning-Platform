# U16 Reporting & Notification - NFR Design Patterns

**Bản tài liệu 2026-10-09**: UC 12, 58 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); luồng phụ: phân bố điểm của UC 22, tiến độ nộp của UC 37, phần xuất tệp của UC 40; primary stories: US-NTF-001, US-RPT-001, US-RPT-002, US-RPT-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Fan-out thông báo theo lô
- `NotificationFanout.create(event, recipients[])`: `INSERT ... SELECT unnest(...) ON CONFLICT DO NOTHING` theo lô `U16_FANOUT_BATCH_SIZE` dòng (kèm `email_status`); đọc `email_preferences` một lần cho cả lô (NFR-U16-02); sau commit gửi việc `EMAIL_SEND` cho các dòng `QUEUED`.
- Bài/quiz của môn: lấy danh sách lớp `OPEN` của môn một lần qua U04, gộp người học `ACTIVE` từng lớp, khử trùng `accountId` trước khi chèn (BR-U16-09, NFR-U16-06).
- `assignment.opened` có `type = MULTIPLE_CHOICE_QUIZ` → `QUIZ_PUBLISHED`, `email_status = NONE`.
- Sau commit: phát `notification.created {recipientIds}` lên fanout `platform.realtime` (dùng chung với U14) → `SseHub` đẩy tới kênh người dùng.

## P2 - Email có trần và ưu tiên
- Việc `EMAIL_SEND` đi trên `jobs.email` (priority queue: nhắc hạn > bài mới mở > điểm > ghi danh); consumer prefetch 1, Guava `RateLimiter` 1/s (NFR-U16-03, 10).
- Mỗi email: `INCR email:daily-count:{yyyyMMdd}`; vượt 300 → `DECR`, `email_status = DEFERRED`, ack việc.
- `EmailDeferredScanner` lúc 00:05: `UPDATE notifications SET email_status='QUEUED' WHERE email_status='DEFERRED' RETURNING id` theo ưu tiên, thời điểm tạo, rồi gửi lại việc.

## P3 - Gửi idempotent
- Handler: `UPDATE ... WHERE id=? AND email_status IN ('QUEUED','DEFERRED')` khóa dòng → gửi → `SENT`; lỗi tạm → ném để U03 retry; lỗi vĩnh viễn (địa chỉ sai) → `FAILED` + `DECR` bộ đếm. Việc trùng thấy `SENT` thì bỏ qua.
- Nhắc hạn đã quá hạn nộp lúc gửi → `SKIPPED` (BR-U16-13).

## P4 - Nhắc hạn bằng scanner
- `DeadlineReminderScanner` mỗi phút theo điều kiện ở `domain-entities.md` §5 (bỏ quiz, chỉ bài có `closes_at`); tạo thông báo và ghi `reminder_sent_at` trong một transaction, khóa dòng bài bằng `FOR UPDATE SKIP LOCKED` nên hai worker không nhắc trùng (NFR-U16-11).
- Bài của môn: người chưa nộp tính trên mọi lớp `OPEN` của môn, chèn theo lô như P1.

## P5 - Tiến độ nộp một query
- Người học `ACTIVE` của lớp (U04) LEFT JOIN trạng thái lượt (U11) hoặc bản nộp nhóm (U14) theo bài; bài của môn lọc theo `classId` được truyền và đã kiểm R3/R4.

## P6 - Admin Dashboard, phân bố điểm và xuất bảng điểm
- Admin Dashboard chỉ cho `ADMIN`, đếm bằng truy vấn `COUNT ... GROUP BY` qua port U01/U04, không trả hàng dữ liệu cá nhân, không cache.
- Phân bố điểm lấy `accountId` từ phiên đăng nhập, kiểm ghi danh và cờ lớp, đọc điểm `PUBLISHED` của lớp qua U15 (bài của môn theo lớp), áp ngưỡng BR-U16-42 trước khi trả dữ liệu; loại quiz và `PRACTICE`.
- Export kiểm R3/R4 của lớp và bài trước query; đọc dữ liệu từ `GradebookQueryPort` và stream CSV/XLSX trực tiếp. Chuỗi CSV bắt đầu bằng `=`, `+`, `-`, `@` được escape; không log nội dung tệp.
