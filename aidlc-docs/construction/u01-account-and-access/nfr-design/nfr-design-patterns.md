# U01 Account & Access - NFR Design Patterns

Mỗi pattern ghi yêu cầu nó phục vụ (`NFR-U01-xx`, `BR-U01-xx`).

## 1. Bảo mật

### P1 - Xác thực không trạng thái cho access, có trạng thái cho refresh
- Access token JWT HMAC-SHA256, 15 phút, gồm `sub`, `role`, `cv` (credentialVersion), `exp`, `jti`. Filter của Spring Security chỉ kiểm chữ ký và hạn, **không** gọi Redis hay DB (NFR-U01-04, 10).
- Refresh token là chuỗi ngẫu nhiên 256 bit; Redis lưu băm SHA-256 kèm `accountId`, `cv`, `issuedAt`, `lastUsedAt` (NFR-U01-11).
- Refresh: đọc bản ghi, so `cv` với `accounts.credential_version`; lệch → xóa và từ chối. Khớp → cấp access mới, **xoay** refresh mới, xóa bản cũ. Refresh cũ bị dùng lại → xóa cả phiên (BR-U01-46).
- Idle 2 giờ bằng TTL làm mới mỗi lần refresh; tối đa 7 ngày bằng kiểm `issuedAt` (NFR-U01-11).
- Đăng xuất: xóa bản ghi refresh, xóa cả hai cookie. Access còn hạn không bị chặn (Câu D3, NFR-U01-13).

### P2 - Cookie an toàn
- `access_token`: `Secure`, `HttpOnly`, `SameSite=Lax`, `Path=/api`, `Max-Age` 15 phút.
- `refresh_token`: `Secure`, `HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth/refresh`, chỉ gửi tới đúng endpoint refresh.
- Local chạy HTTP được cấu hình tắt `Secure` riêng cho profile `local`, không bao giờ cho production (NFR-U01-12).

### P3 - Chống dò tài khoản
- Đăng nhập với email không tồn tại vẫn chạy `bcrypt.matches` với hash giả tạo sẵn lúc khởi động (NFR-U01-16).
- Mọi thất bại đăng nhập trả cùng mã lỗi và thông điệp. Yêu cầu OTP luôn `202 Accepted` với cùng thân phản hồi (BR-U01-12, 40).

### P4 - Băm mật khẩu
- `BCryptPasswordEncoder` cost cấu hình, mặc định 12. Kiểm độ dài UTF-8 ≤ 72 byte trước khi băm (NFR-U01-14).
- OTP và refresh token băm SHA-256; không cần hàm chậm vì đã có TTL, giới hạn lượt thử và entropy cao.

### P5 - Phân quyền mặc định từ chối
- `AuthorizationService.authorize()` là cổng duy nhất; controller không tự so role.
- Mọi endpoint mặc định cần access token; danh sách endpoint public khai báo tường minh: đăng nhập, refresh, yêu cầu OTP, kích hoạt, đặt lại mật khẩu (NFR-U01-53).

## 2. Giới hạn tần suất

### P6 - Token bucket phân tán (Bucket4j + Redis)
| Khóa | Dung lượng | Nạp lại | Nguồn |
|---|---|---|---|
| `otp:email:{hash(email)}` | 5 | 5/giờ, tối thiểu 60 giây giữa hai lần | NFR-U01-20 |
| `otp:ip:{ip}` | 20 | 20/giờ | NFR-U01-20 |
| `login:ip:{ip}` | 30 | 30/5 phút | NFR-U01-21 |

- Email trong khóa được băm để Redis không chứa email rõ.
- Hết lượt: OTP vẫn trả `202` trung tính; đăng nhập trả lỗi trung tính (NFR-U01-22).
- Ngưỡng đọc từ cấu hình (NFR-U01-23).
- Khóa tạm 15 phút sau 5 lần sai là trạng thái trên `accounts` (`failed_login_count`, `locked_until`), **không** nằm trong Bucket4j.

## 3. Chịu lỗi

### P7 - Timeout tường minh, fail closed
| Phụ thuộc | Timeout | Khi lỗi | Nguồn |
|---|---|---|---|
| PostgreSQL | Kết nối 2 s, truy vấn 3 s | Lỗi an toàn `503` | NFR-U01-41 |
| Redis | Kết nối 1 s, lệnh 500 ms | Đăng nhập, refresh, OTP trả `503` "hệ thống tạm bận"; access còn hạn vẫn chạy | NFR-U01-40 |
| U04 (phạm vi) | 1 s | Từ chối quyền | NFR-U01-42 |
| U03 (`AvatarPort`) | 2 s | Tắt đổi ảnh, hồ sơ còn lại chạy | NFR-U01-43 |
| SMTP | Kết nối 5 s, gửi 10 s | Worker retry, không ảnh hưởng người dùng | NFR-U01-31 |

- **Không dùng circuit breaker** (Câu D2). Lý do: quy mô ≤ 100 người đồng thời, mọi timeout ngắn và pool hữu hạn nên phụ thuộc chết không làm cạn tài nguyên; SMTP đã tách sang worker bất đồng bộ. RESILIENCY-10 cho phép ghi "không áp dụng" với circuit breaker.

### P8 - Pool hữu hạn (bulkhead nhẹ)
- Pool PostgreSQL tối đa 10 kết nối; pool Redis tối đa 16.
- Gửi mail chạy ở worker, tách khỏi thread xử lý request (RESILIENCY-10).

### P9 - Outbox gửi OTP
- Request chỉ ghi bản ghi outbox `OTP_DELIVERY(accountId, purpose)` trong giao dịch PostgreSQL rồi trả `202`. Không sinh mã ở request.
- Worker nhận job, **sinh mã tại chỗ**, ghi băm vào Redis (xóa mã cũ), gửi SMTP. Mã rõ chỉ tồn tại trong bộ nhớ worker và trong email; không nằm trong queue, DB hay log.
- SMTP lỗi → retry 5 lần, backoff 30 s, 1 phút, 2 phút, 4 phút, 8 phút; mỗi lần retry sinh mã mới. Hết lượt → dead-letter + log (NFR-U01-31).
- Redis lỗi lúc worker chạy → job retry như lỗi SMTP.
- Hạn 10 phút của OTP tính từ lúc worker ghi mã, không phải lúc người dùng bấm.

## 4. Quan sát

### P10 - Log, metric, health
- Log JSON: `timestamp`, `level`, `correlationId`, `event`, `accountId` khi có. Bộ lọc che `password`, `otp`, `token`, `phone` trước khi ghi (NFR-U01-50).
- Metric: số đăng nhập thành công/thất bại, số khóa tạm, số OTP gửi/thất bại, số lần chạm rate limit, độ trễ p95 từng endpoint.
- Cảnh báo: > 20 đăng nhập thất bại/5 phút từ một IP; > 10 lần từ chối quyền/5 phút cho một tài khoản; mọi thay đổi role (NFR-U01-52).
- Health: `/health/live` nông; `/health/ready` kiểm PostgreSQL và Redis (REL-005, RESILIENCY-06).

## 5. Kiểm thử chịu lỗi (RESILIENCY-14)

Integration test với Testcontainers, chạy trong CI mỗi lần build; kết quả lưu cùng báo cáo test (Câu D4):

| Kịch bản | Kỳ vọng |
|---|---|
| Dừng Redis rồi đăng nhập | `503`, không tạo phiên |
| Dừng Redis, gọi API bằng access còn hạn | Vẫn thành công |
| Dừng Redis rồi yêu cầu OTP | `503` từ rate limit, không ghi outbox |
| Dừng Mailpit rồi yêu cầu OTP | `202`; job retry rồi vào dead-letter |
| Dừng PostgreSQL rồi đăng nhập | `503` an toàn, không lộ chi tiết |
| U04 giả trả chậm quá timeout | Từ chối quyền |
| Bật lại phụ thuộc | Luồng tự phục hồi, không cần khởi động lại backend |
