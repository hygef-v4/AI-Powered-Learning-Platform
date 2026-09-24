# U07 Payment & AI Credit - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| PayOS | Gọi REST bằng Spring `RestClient` (tạo link `/v2/payment-requests`, lấy trạng thái), tự tính HMAC-SHA256 bằng `javax.crypto` | Ít phụ thuộc; SDK Java chính thức cũng dùng được nếu tiện |
| Khóa ví | `SELECT ... FOR UPDATE` qua JPA `PESSIMISTIC_WRITE` | Đơn giản, đúng |
| Job | U02 `U07_RECONCILE` (10 phút), `U07_RESERVATION_SWEEP` (5 phút) qua scheduler + JobPort | Dùng lại U02 |
| Rate limit webhook | Bucket4j + Redis | Đã có |
