# U01 Account & Access - Business Rules

**Bản tài liệu 2026-10-09**: UC 01, 02, 03, 04, 05, 06, 07, 59, 60, 61, 62, 63 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Mỗi rule có mã `BR-U01-xx` để truy vết sang test. Ngưỡng có ghi "chốt ở NFR" là tham số cấu hình, không cố định ở đây.

## 1. Định danh và email

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-01 | Không có đăng ký công khai. Chỉ admin tạo hoặc nhập tài khoản. | US-IAM-001, FR-001 |
| BR-U01-02 | Email được chuẩn hóa (cắt khoảng trắng, chữ thường) trước mọi so sánh. | US-IAM-007 |
| BR-U01-03 | Email phải duy nhất và thuộc tên miền trong cấu hình triển khai `U01_ALLOWED_EMAIL_DOMAINS`; sai thì từ chối tạo. | US-IAM-001 S2, US-IAM-007 |
| BR-U01-04 | Email là định danh đăng nhập, không ai sửa được sau khi tạo, kể cả admin. | UC 06, 07, 60, 62 |

## 2. Kích hoạt

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-10 | Tạo hoặc nhập tài khoản cho trạng thái `PENDING`, **không gửi email**, admin không đặt mật khẩu. | FR-015, UC 60 |
| BR-U01-11 | OTP kích hoạt chỉ được gửi khi người dùng tự yêu cầu từ liên kết "Kích hoạt tài khoản lần đầu". Admin không có thao tác gửi OTP. | US-IAM-001, Câu hỏi FU3 |
| BR-U01-12 | Yêu cầu kích hoạt luôn trả phản hồi trung tính giống nhau. Chỉ gửi OTP khi email khớp tài khoản `PENDING`. | US-IAM-001 S2 |
| BR-U01-13 | Kích hoạt thành công: lưu mật khẩu, chuyển `ACTIVE`, xóa ticket, ghi audit, rồi **tự đăng nhập**: tạo phiên như đăng nhập (BR-U01-44) và đưa người dùng tới dashboard theo role (BR-U01-48). Đặt lại mật khẩu (UC 04) không tự đăng nhập. | US-IAM-001 S1; người dùng chốt 2026-10-04 |

## 3. OTP (kích hoạt và đặt lại)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-20 | OTP gồm 6 chữ số, sinh ngẫu nhiên an toàn, chỉ lưu dạng băm. | Câu 4 |
| BR-U01-21 | OTP hết hạn sau 10 phút. | Câu 4 |
| BR-U01-22 | Nhập sai 5 lần thì hủy OTP; phải yêu cầu mã mới. | Câu 4 |
| BR-U01-23 | Mỗi tài khoản và mục đích chỉ có một OTP hiệu lực; mã mới làm mã cũ mất hiệu lực. | Câu 4 |
| BR-U01-24 | OTP dùng một lần; xóa ngay khi dùng thành công. | component-methods |
| BR-U01-25 | Yêu cầu gửi OTP bị giới hạn tần suất theo email và theo client; vượt giới hạn thì không gửi nhưng vẫn trả phản hồi trung tính. | US-IAM-001 S3 |
| BR-U01-26 | OTP của mục đích này không dùng được cho mục đích kia. | Thiết kế |
| BR-U01-28 | Nhập OTP và đặt mật khẩu là hai bước: người dùng nhập OTP trước; chỉ khi OTP đúng hệ thống mới xóa challenge, cấp `otpTicket` (dùng một lần, 10 phút, gắn tài khoản và mục đích) và mở bước nhập mật khẩu mới. Mật khẩu không đạt chính sách thì nhập lại với cùng ticket; ticket hết hạn phải xin OTP mới. | Người dùng chốt 2026-10-03 |
| BR-U01-27 | OTP không xuất hiện trong log, audit, phản hồi API hay màn hình admin. | SECURITY-03 |

## 4. Mật khẩu

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-30 | Tối thiểu 8 ký tự, có ít nhất một chữ cái và một chữ số. | Câu 3 |
| BR-U01-31 | Từ chối mật khẩu chứa phần tên trong email. Không kiểm danh sách mật khẩu bị lộ (ngoại lệ SECURITY-12 được chấp nhận). | U01 NFR |
| BR-U01-32 | Mật khẩu băm bằng bcrypt; không bao giờ lưu hay ghi log bản rõ. | SECURITY-12, U01 NFR |
| BR-U01-33 | Đổi mật khẩu phải xác nhận đúng mật khẩu hiện tại. | US-IAM-006 |
| BR-U01-34 | Đổi hoặc đặt lại mật khẩu tăng `credentialVersion`: mọi phiên khác bị thu hồi. Đổi mật khẩu giữ phiên hiện tại; đặt lại qua OTP không giữ phiên nào. | US-IAM-006 S1, Câu 14 |
| BR-U01-35 | Không có mật khẩu tạm thời. | Câu 1 |

## 5. Đăng nhập và phiên

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-40 | Chỉ tài khoản `ACTIVE` đăng nhập được. `PENDING`, `DISABLED`, sai mật khẩu, email không tồn tại đều nhận **cùng một** thông báo lỗi. | US-IAM-002 S2, Câu 2 |
| BR-U01-41 | Sai mật khẩu liên tiếp 5 lần thì đặt `lockedUntil` = hiện tại + 15 phút. Trong thời gian này mọi lần đăng nhập đều bị từ chối với cùng thông báo trung tính. | Câu 5, SECURITY-12 |
| BR-U01-42 | Đăng nhập đúng xóa `failedLoginCount`. Hết `lockedUntil` thì tự mở, không cần admin. | Câu 5 |
| BR-U01-43 | Ngoài khóa theo tài khoản còn giới hạn theo client để chặn thử nhiều tài khoản. | Câu 5 |
| BR-U01-44 | Phiên gồm access token 15 phút và refresh token lưu phía server (idle 2 giờ, tối đa 7 ngày). Refresh chỉ thành công khi `credentialVersion` còn khớp. Access token còn hạn không bị kiểm lại, nên mọi thu hồi quyền có hiệu lực chậm **tối đa 15 phút** (được chấp nhận). | US-IAM-002 S1/S3, U01 NFR |
| BR-U01-45 | Đăng xuất chỉ thu hồi phiên hiện tại. Không có "đăng xuất mọi thiết bị"; người dùng đổi mật khẩu để đá phiên khác. | Câu 14 |
| BR-U01-46 | Refresh token bị dùng lại thì thu hồi phiên đó. | Thiết kế |
| BR-U01-47 | Không có MFA, kể cả `ADMIN` (ngoại lệ SECURITY-12 được chấp nhận). | U01 NFR |
| BR-U01-48 | Đăng nhập hoặc kích hoạt thành công đi qua Post-Login rồi tới dashboard theo role (`homePath`): `STUDENT`, `TEACHER` → Class Dashboard; `SUBJECT_MANAGER` → Manager Dashboard, có nút sang Class Dashboard để vào lớp được giao dạy; `ADMIN` → Admin Dashboard. Admin không vào Manager Dashboard hay Class Dashboard. Khung sau đăng nhập có lối vào User Profile và Logout. | Screen flow G21 Page-2; người dùng chốt 2026-10-09 |

## 6. Hồ sơ

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-50 | Chỉ sửa tên hiển thị/số điện thoại của chính mình; không cập nhật avatar. | FR-001, UC 06–07 |
| BR-U01-51 | Không tự sửa email, role, trạng thái. | UC 06, 07 |
| BR-U01-52 | DTO hồ sơ chỉ nhận displayName/phoneNumber; từ chối avatar/fileRef/email/role/status và ID người khác. Không gọi U03 đổi ảnh. | FR-001, UC 07 |
| BR-U01-53 | Số điện thoại là dữ liệu cá nhân: không ghi log, chỉ chủ tài khoản và admin xem. | US-IAM-004 S1 |
| BR-U01-54 | Định danh người khác trong request sửa hồ sơ bị từ chối phía server, không lộ dữ liệu đối tượng. | US-IAM-004 S2 |

## 7. Role và phân quyền

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-60 | Mỗi tài khoản một role. Student → User, Teacher → User, Subject Manager → Teacher. `ADMIN` chỉ dùng chức năng User và chức năng quản trị; không dùng chức năng Subject Manager/Teacher và không nhận phân công môn/lớp. R1 chỉ dữ liệu của mình; R2 (môn) và R3/R4 (lớp) cần phân công hiện thời và tài khoản `ACTIVE`. Không vai trò nào kế thừa quyền Student. | FR-002; người dùng chốt 2026-10-09 |
| BR-U01-61 | U01 chỉ đặt role. Gán môn/lớp cụ thể là việc của U04. | Câu 7 |
| BR-U01-62 | Mọi quyết định quyền chạy phía server, mặc định từ chối, kết hợp role của U01 với phạm vi của U04. | SECURITY-08 |
| BR-U01-63 | Đổi role tăng `credentialVersion`: refresh token của người đó hết hiệu lực ngay; access token còn hạn dùng tối đa 15 phút (BR-U01-44). | Câu 6, US-IAM-005 S2 |
| BR-U01-64 | Đổi role bị chặn nếu role mới không giữ được phân công hiện có: Chủ nhiệm môn cần `SUBJECT_MANAGER`; giảng viên lớp cần `TEACHER` hoặc `SUBJECT_MANAGER`. Gồm cả hạ role và đổi sang `ADMIN`. Lỗi nêu môn/lớp còn phụ trách. | Câu 8; người dùng chốt 2026-10-09 |
| BR-U01-65 | Admin không được tự hạ role hoặc tự vô hiệu hóa chính mình. | Câu 9 |
| BR-U01-66 | Không được hạ role hoặc vô hiệu hóa `ADMIN` đang hoạt động cuối cùng. | Câu 9 |
| BR-U01-67 | Chỉ `ADMIN` được đổi role và trạng thái; lần gọi trái phép bị từ chối và ghi audit. | US-IAM-005 S3, US-IAM-007 S3 |

## 8. Vòng đời tài khoản

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-70 | Chỉ có 3 trạng thái: `PENDING`, `ACTIVE`, `DISABLED`. | Câu 10 |
| BR-U01-71 | Vô hiệu hóa: chuyển `DISABLED`, tăng `credentialVersion`, hủy OTP còn hiệu lực. Không xóa tài khoản hay lịch sử. | US-IAM-007, UC 63 |
| BR-U01-72 | Mở lại: về `ACTIVE` nếu đã có mật khẩu, về `PENDING` nếu chưa. | Câu 10 |
| BR-U01-73 | Không có thao tác xóa tài khoản. | UC 59–63 |

## 9. Nhập hàng loạt (luồng phụ của UC 60)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-80 | Nút "Nhập CSV" trên Account List. Chỉ nhận CSV, tối đa 1000 dòng; cột bắt buộc `email`, `display_name`, `role`. | Câu 13; người dùng chốt giữ 2026-10-09 |
| BR-U01-81 | Kiểm tra toàn bộ file trước, trả kết quả từng dòng; admin xác nhận thì mới tạo các dòng hợp lệ. | US-IAM-007 S2 |
| BR-U01-82 | Email đã tồn tại hoặc trùng trong file bị báo lỗi dòng, không ghi đè. | Câu 13 |
| BR-U01-83 | Nhập lại cùng file không tạo trùng: email đã tồn tại bị báo lỗi dòng (BR-U01-82). Kết quả nhập không lưu; audit ghi checksum file. | US-IAM-007 S2 |
| BR-U01-84 | Nhập hàng loạt không được tạo `ADMIN`; tạo admin chỉ làm từng tài khoản. | SECURITY-11 |
| BR-U01-85 | Nhập hàng loạt không gửi email. | FR-015 |

## 10. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U01-90 | Ghi audit qua U02: đăng nhập thất bại, khóa tạm, kích hoạt, đổi/đặt lại mật khẩu, đổi hồ sơ, Admin sửa thông tin tài khoản, đổi role (trước/sau), đổi trạng thái, nhập hàng loạt, truy cập bị từ chối. | FR-014, SECURITY-03 |
| BR-U01-91 | Audit và log không chứa mật khẩu, OTP, token, số điện thoại. | SECURITY-03 |
| BR-U01-92 | Lỗi gửi email không làm hỏng yêu cầu: người dùng vẫn nhận phản hồi trung tính, job retry hữu hạn. | US-IAM-003 S2, RESILIENCY |
| BR-U01-93 | Khi không kiểm được quyền (phụ thuộc lỗi) thì từ chối. | SECURITY-15 |
| BR-U01-94 | Lỗi trả client dạng an toàn, không lộ stack trace hay trạng thái tài khoản. | SECURITY-15 |
| BR-U01-95 | Redis không khả dụng thì không tạo phiên mới, không refresh, không gửi OTP; trả "hệ thống tạm bận". Access token còn hạn vẫn dùng được. | SEC-006, U01 NFR |
