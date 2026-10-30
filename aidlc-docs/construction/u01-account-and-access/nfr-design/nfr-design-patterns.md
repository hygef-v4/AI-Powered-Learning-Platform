# U01 Account & Access — nfr-design-patterns

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12. **Primary stories:** US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. **Requirements:** FR-001/002/015.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md). Baseline cũ: snapshot. Folder `u01-account-and-access` là thiết kế của U01 hiện hành.

Mỗi pattern ghi yêu cầu nó phục vụ (`NFR-U01-xx`, `BR-U01-xx`).

## 1. Bảo mật

### P1 - Xác thực không trạng thái cho access, có trạng thái cho refresh
- Access token JWT HMAC-SHA256, 15 phút, gồm `sub`, `role`, `exp`, `jti`, `sid` (mã phiên). Filter kiểm chữ ký/hạn, sau đó ISessionAuthority kiểm phiên `sid` còn trên Redis và account `ACTIVE` trên PostgreSQL. IAuthorization tiếp tục kiểm role/scope/resource từ authority hiện tại; dependency unavailable fail closed (NFR-U01-04, 10).
- Refresh token là chuỗi ngẫu nhiên 256 bit; Redis lưu băm SHA-256 kèm `sid`, `accountId`, `issuedAt`, `lastUsedAt` (NFR-U01-11).
- Refresh: đọc bản ghi theo băm; phiên `sid` không còn → từ chối. Còn → cấp access mới, **xoay** refresh mới, xóa bản cũ. Refresh cũ bị dùng lại → xóa cả phiên (BR-U01-46).
- Thu hồi phiên: `account:sessions:{accountId}` giữ các `sid` của tài khoản. Đổi hoặc đặt lại mật khẩu, đổi role, vô hiệu hóa xóa các phiên (đổi mật khẩu giữ phiên hiện tại) trước khi commit và xóa lại một lần sau commit để không sót phiên tạo chen giữa; Redis lỗi thì hủy thao tác, trả `503` (BR-U01-34, 63, 71).
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
| `ratelimit:auth:otp-email:{hash(email)}` | 5 | 5/giờ, tối thiểu 60 giây giữa hai lần | NFR-U01-20 |
| `ratelimit:auth:otp-ip:{ip}` | 20 | 20/giờ | NFR-U01-20 |
| `ratelimit:auth:login-ip:{ip}` | 30 | 30/5 phút | NFR-U01-21 |

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
| U04, U05 (phạm vi) | 1 s | Từ chối quyền | NFR-U01-42 |
| SMTP | Kết nối 5 s, gửi 10 s | Worker retry, không ảnh hưởng người dùng | NFR-U01-31 |

- **Không dùng circuit breaker** (Câu D2): quy mô ≤ 100 người đồng thời, timeout ngắn và pool hữu hạn là đủ.

### P8 - Pool hữu hạn (bulkhead nhẹ)
- Pool PostgreSQL tối đa 10 kết nối; pool Redis tối đa 16.
- Gửi mail chạy ở worker, tách khỏi thread xử lý request (RESILIENCY-10).

### P9 - Gửi OTP qua job U03
- Request không ghi database: gọi `IJob.enqueue` gửi message `OTP_DELIVERY(accountId, purpose)` sang queue `jobs.email` của RabbitMQ (không có bảng job) rồi trả `202`. Không sinh mã ở request.
- Worker nhận job, **sinh mã tại chỗ**, ghi băm vào Redis (xóa mã cũ), gửi SMTP. Mã rõ chỉ tồn tại trong bộ nhớ worker và trong email; không nằm trong queue, DB hay log.
- SMTP lỗi → retry 5 lần, backoff 30 s, 1 phút, 2 phút, 4 phút, 8 phút; mỗi lần retry sinh mã mới. Hết lượt → bỏ message, log ERROR `OTP_DELIVERY_FAILED` (U03 không có dead-letter); người dùng yêu cầu OTP lại (NFR-U01-31).
- Redis lỗi lúc worker chạy → message retry như lỗi SMTP.
- Hạn 10 phút của OTP tính từ lúc worker ghi mã, không phải lúc người dùng bấm.

## 4. Quan sát

### P10 - Log và health
- Log JSON: `timestamp`, `level`, `correlationId`, `event`, `accountId` khi có. Bộ lọc che `password`, `otp`, `token`, `phone` trước khi ghi (NFR-U01-50).
- Xem log bằng `docker compose logs`. Không có metric, dashboard hay cảnh báo tự động (ngoài phạm vi đồ án).
- Health: `/health` kiểm PostgreSQL và Redis; Docker Compose dùng nó làm healthcheck.

## 5. Kiểm thử lỗi phụ thuộc

Không bắt buộc (RESILIENCY-14 ngoài phạm vi đồ án). Hành vi fail-closed ở P7 vẫn được kiểm bằng unit test với adapter giả trả lỗi.

## Quy tắc tích hợp revision hiện hành

- Tài khoản trường cấp, không public registration. Admin tạo/import không gửi OTP hoặc đặt mật khẩu; người dùng yêu cầu activation ở login đầu và tự đặt mật khẩu sau OTP.
- Account role cao nhất trong STUDENT/TEACHER/SUBJECT_MANAGER/ADMIN; authorization theo action/resource/current state, không chỉ role JWT. Admin chỉ quản trị, không học vụ.
- Profile cá nhân chỉ displayName/phoneNumber, email định danh/role/status bất biến trong /me. Reset không tiết lộ account tồn tại.
- MCP credentials xác thực actor qua U06 và cùng IAuthorization; không dùng provider API key làm ALAP authentication. Student không shared quota.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. Decided 2026-10-15: D01 Quiz Practice only, D02 Group Graded only, D03 dropped (no student AI for group work), D04 daily quota plus global cap. Backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Tạo/import không gửi OTP, activation hợp lệ/sai/hết hạn/rate limit; recovery không lộ account.
- Đăng nhập/đăng xuất/phiên revoked; R1 own profile; Admin bị chặn học vụ; Student shared quota bị từ chối.

ISessionAuthority xác minh phiên `sid` còn (Redis) và account status (PostgreSQL) trước action; JWT không chứa quyền academic đầy đủ, role từ current account. PostgreSQL lỗi thì fail closed; Redis lỗi thì bỏ qua bước kiểm phiên theo NFR-U01-40; cache nếu dùng cần invalidation khi đổi role/status/logout, không TTL grace bỏ qua revoke.

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
