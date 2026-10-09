# U07 Payment & AI Credit - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 08, 09, 10, 11, 68, 69, 72 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); mục credit tặng định kỳ của Settings (UC 70–71); primary stories: US-PAY-001, US-PAY-002, US-PAY-004, US-PAY-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| PayOS | Gọi REST bằng Spring `RestClient` (tạo link `/v2/payment-requests`, lấy trạng thái), tự tính HMAC-SHA256 bằng `javax.crypto` | Ít phụ thuộc; SDK Java chính thức cũng dùng được nếu tiện |
| Khóa số dư | `SELECT ... FOR UPDATE` qua JPA `PESSIMISTIC_WRITE` | Đơn giản, đúng |
| Việc nền | Scanner U03 mỗi 10 phút + việc `PAYOS_CHECK` trên `jobs.payos`; trả credit giữ quá hạn do scanner của U13 | Dùng lại U03, không có bảng job |
| Rate limit webhook | Bucket4j + Redis | Đã có |
