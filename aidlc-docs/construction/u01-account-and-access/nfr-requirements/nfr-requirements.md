# U01 Account & Access - NFR Requirements

Mã `NFR-U01-xx` để truy vết sang NFR Design và test. Nguồn quyết định: `plans/u01-account-and-access-nfr-requirements-questions.md`.

## 1. Quy mô và hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U01-01 | Phục vụ tới 1.000 tài khoản và 100 người dùng đồng thời. | NFR-003 |
| NFR-U01-02 | Đăng nhập, refresh, đăng xuất, hồ sơ, danh sách tài khoản: p95 ≤ 500 ms ở tải mục tiêu. Thời gian bcrypt được tính vào ngân sách này. | NFR-003 |
| NFR-U01-03 | Yêu cầu OTP trả `Accepted` mà không chờ gửi mail; mail đi qua outbox. | NFR-003, BR-U01-92 |
| NFR-U01-04 | Kiểm tra access token không gọi Redis hay database. | Câu N5 |
| NFR-U01-05 | Nhập CSV 1000 dòng: bước kiểm tra xong trong ≤ 10 giây. | BR-U01-80 |

## 2. Phiên và xác thực

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U01-10 | Access token JWT ký bằng khóa bí mật phía server, sống 15 phút, chứa `accountId`, `role`, `credentialVersion`. | Câu N1, N5 |
| NFR-U01-11 | Refresh token ngẫu nhiên, chỉ lưu bản băm ở Redis. Idle 2 giờ, tối đa 7 ngày. Mỗi lần refresh cấp token mới và hủy token cũ; phát hiện dùng lại thì thu hồi phiên. | Câu N2, BR-U01-46 |
| NFR-U01-12 | Cả hai token nằm trong cookie `Secure`, `HttpOnly`, `SameSite=Lax`; không để trong local storage hay URL. | SEC-002 |
| NFR-U01-13 | Thu hồi quyền (đổi role, vô hiệu hóa, đổi/đặt lại mật khẩu) chặn refresh ngay; access token còn hạn dùng tối đa 15 phút. **Rủi ro được chấp nhận.** | Câu N5 |
| NFR-U01-14 | Mật khẩu băm bằng bcrypt, cost ≥ 12; cost cấu hình được. Mật khẩu dài quá 72 byte bị từ chối rõ ràng thay vì bị cắt ngầm. | Câu N4 |
| NFR-U01-15 | Không có MFA; không kiểm danh sách mật khẩu bị lộ. **Ngoại lệ SECURITY-12 được chấp nhận.** | Câu N3, N6 |
| NFR-U01-16 | So khớp mật khẩu cho email không tồn tại vẫn chạy bcrypt với hash giả để thời gian phản hồi đồng đều. | BR-U01-40 |

## 3. Giới hạn tần suất

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U01-20 | Yêu cầu OTP (kích hoạt, quên mật khẩu): chờ 60 giây giữa hai lần, tối đa 5 lần/giờ mỗi email, 20 lần/giờ mỗi IP. | Câu N7 |
| NFR-U01-21 | Đăng nhập: khóa tạm tài khoản 15 phút sau 5 lần sai; giới hạn theo IP 30 lần/5 phút. | Câu 5 FD, BR-U01-43 |
| NFR-U01-22 | Vượt giới hạn vẫn trả phản hồi trung tính cho OTP; đăng nhập trả cùng lỗi trung tính. | BR-U01-25, 40 |
| NFR-U01-23 | Ngưỡng là cấu hình, không cố định trong code. | Bảo trì |

## 4. Email

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U01-30 | Gửi mail qua Mail Port dùng SMTP. Local/demo dùng Mailpit, không gửi mail thật. Production cấu hình SMTP bằng biến môi trường/secret. | Câu N8, NFR-005 |
| NFR-U01-31 | Kết nối SMTP có timeout; lỗi thì outbox retry tối đa 5 lần với backoff tăng dần; hết lượt thì chuyển dead-letter và ghi log, không báo lỗi cho người dùng. | REL-007, BR-U01-92 |
| NFR-U01-32 | Nội dung mail chỉ chứa mã OTP và thời hạn; không chứa mật khẩu, link đăng nhập hay thông tin tài khoản khác. | SEC-001 |

## 5. Khả dụng và lỗi phụ thuộc

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U01-40 | Redis không khả dụng: không đăng nhập mới, không refresh, không gửi OTP, trả "hệ thống tạm bận". Access token còn hạn vẫn dùng được. | Câu N9, SEC-007 |
| NFR-U01-41 | PostgreSQL không khả dụng: mọi thao tác ghi và đăng nhập trả lỗi an toàn. | SEC-007 |
| NFR-U01-42 | U04 không trả lời khi kiểm phạm vi: từ chối quyền. | BR-U01-93 |
| NFR-U01-43 | U03 không khả dụng: tắt đổi ảnh đại diện, phần hồ sơ còn lại vẫn chạy. | BR-U01-52 |
| NFR-U01-44 | Mọi lời gọi Redis, database, U03, U04, SMTP có timeout hữu hạn. | NFR-003, REL-007 |
| NFR-U01-45 | Mức quan trọng Trung bình; RTO/RPO tính bằng giờ theo REL-002. | REL-001, REL-002 |

## 6. Bảo mật dữ liệu và log

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U01-50 | Log có cấu trúc gồm timestamp, correlation ID, level, message; không chứa mật khẩu, OTP, token, số điện thoại. | SEC-005, BR-U01-91 |
| NFR-U01-51 | Khóa ký JWT và thông tin SMTP chỉ lấy từ biến môi trường/secret store. | NFR-005 |
| NFR-U01-52 | Cảnh báo khi đăng nhập thất bại lặp lại, bị từ chối quyền nhiều lần, đổi role. | SEC-005 |
| NFR-U01-53 | Endpoint public (đăng nhập, yêu cầu OTP, kích hoạt, đặt lại) có rate limit; mọi endpoint khác yêu cầu access token. | SEC-003 |

## 7. Khả năng kiểm thử và dùng được

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U01-60 | Unit test cho mọi `BR-U01-xx`; integration test với PostgreSQL và Redis thật trong container; test Mailpit nhận đúng OTP. | NFR-004 |
| NFR-U01-61 | Negative test bảo mật: dò tài khoản, brute-force, OTP sai/hết hạn, truy cập hồ sơ người khác, admin tự hạ quyền, hạ admin cuối cùng. | NFR-004, SECURITY-08 |
| NFR-U01-62 | Form đăng nhập, kích hoạt, đổi mật khẩu dùng được bằng bàn phím, có nhãn cho trình đọc màn hình. | NFR-002 |

## 8. Compliance

| Rule | Trạng thái | Ghi chú |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U01-50 |
| SECURITY-05 | Compliant | Rate limit và validation endpoint public |
| SECURITY-08 | Compliant | Mặc định từ chối, NFR-U01-42 |
| SECURITY-11 | Compliant | Phản hồi trung tính, thời gian đồng đều |
| SECURITY-12 | **Ngoại lệ được chấp nhận** | Đạt: bcrypt, ≥ 8 ký tự, cookie an toàn, hết hạn phía server, chống brute-force. Không đạt theo quyết định người dùng: MFA admin, kiểm mật khẩu bị lộ. Thêm rủi ro: thu hồi trễ tối đa 15 phút |
| SECURITY-15 | Compliant | NFR-U01-40…44 fail closed |
| RESILIENCY-10 | Compliant | Timeout, retry hữu hạn, dead-letter cho SMTP |
| Các rule còn lại | N/A | Hạ tầng, backup, alarm chốt ở Infrastructure Design |
