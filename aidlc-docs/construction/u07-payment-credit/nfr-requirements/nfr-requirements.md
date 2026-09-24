# U07 Payment & AI Credit - NFR Requirements

## 1. Toàn vẹn tiền và credit

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U07-01 | Mọi thay đổi số dư đi qua một hàm duy nhất: khóa hàng ví (`SELECT ... FOR UPDATE`), ghi sổ cái, cập nhật số dư trong cùng transaction. | BR-U07-12, 34 |
| NFR-U07-02 | Unique DB: `orderCode`, `(account_id, idempotency_key)`, `eventKey`, `requestRef`; cộng credit `PURCHASE` unique theo `payment_id`. | BR-U07-04, 12, 41 |
| NFR-U07-03 | Tiền lưu số nguyên VND (`bigint`), credit số nguyên; không dùng số thực. | Thiết kế |
| NFR-U07-04 | User `app` không được UPDATE/DELETE bảng sổ cái (chỉ INSERT, SELECT). | BR-U07 sổ cái |
| NFR-U07-05 | Test tổng sổ cái = số dư ví sau mọi luồng (mua, tặng, giữ, trừ, trả, điều chỉnh, đồng thời). | NFR-004 |

## 2. PayOS

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U07-10 | `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY` đọc từ `.env`; không log. | SEC-006 |
| NFR-U07-11 | Kiểm chữ ký bằng so sánh hằng thời gian; trường ký sắp theo thứ tự PayOS quy định. | SEC-007 |
| NFR-U07-12 | Timeout gọi PayOS: kết nối 5 s, đọc 10 s; không retry tạo link (người dùng tự thử lại), đối soát retry ở lần job kế. | REL-003 |
| NFR-U07-13 | Webhook trả lời ≤ 2 s; chỉ nhận `POST` JSON ≤ 16 KB tại endpoint riêng, không cần đăng nhập, rate limit 60/phút/IP. | SEC-007 |
| NFR-U07-14 | Webhook URL đăng ký với PayOS là HTTPS của domain hệ thống. | SEC-004 |
| NFR-U07-15 | Không có `PAYOS_*` → dùng adapter giả (link giả trỏ về trang xác nhận nội bộ) cho local/test; tắt ở production. | NFR-005 |

## 3. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U07-20 | `reserve`/`settle`/`release` p95 ≤ 50 ms. | U13 cần nhanh |
| NFR-U07-21 | Đọc số dư p95 ≤ 50 ms. | NFR-003 |

## 4. Bảo mật khác

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U07-30 | Người dùng chỉ thấy giao dịch/sổ cái của mình; ngoài quyền trả `404`. | SEC-002 |
| NFR-U07-31 | `CreditPort` chỉ gọi nội bộ, không có endpoint HTTP cho `reserve`/`settle`. | Thiết kế |
| NFR-U07-32 | Không lưu dữ liệu thẻ/ngân hàng của người dùng; chỉ lưu mã tham chiếu PayOS. | SEC-007 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U07-10 |
| SECURITY-05 | Compliant | NFR-U07-11, 13 |
| SECURITY-08 | Compliant | NFR-U07-30, 31 |
| SECURITY-09 | Compliant | Key trong `.env` |
| SECURITY-15 | Compliant | Webhook sai → từ chối, không cộng |
| RESILIENCY-10 | Compliant | NFR-U07-12 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
