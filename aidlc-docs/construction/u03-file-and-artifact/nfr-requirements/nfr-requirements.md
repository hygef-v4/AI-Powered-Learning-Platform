# U03 File & Artifact - NFR Requirements

## 1. Hiệu năng và tài nguyên

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-01 | Tối đa 5 upload xử lý cùng lúc trên backend; upload thứ 6 nhận `503` "hệ thống đang bận, thử lại sau". | Câu N2 |
| NFR-U03-02 | File nhận vào được ghi ra thư mục tạm trên đĩa, không giữ toàn bộ trong RAM; xóa ngay khi xong, kể cả khi lỗi. | Câu N2 |
| NFR-U03-03 | Nginx `client_max_body_size 50m`; backend từ chối sớm khi `Content-Length` > 50 MB. | BR-U03-02 |
| NFR-U03-04 | Upload 50 MB hoàn tất trong ≤ 60 giây trên mạng VPS thông thường; timeout request upload 120 giây. | NFR-003 |
| NFR-U03-05 | Tải về stream từng phần, bộ đệm ≤ 64 KB; không nạp file vào RAM. | NFR-003 |
| NFR-U03-06 | Cấp download token p95 ≤ 100 ms (chỉ ghi Redis). | NFR-003 |

## 2. Google Drive

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-10 | Backend upload bằng **JSON key của service account** đặt trong `.env`: `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` (nội dung JSON mã hóa base64) và `GOOGLE_SHARED_DRIVE_ID`. Service account được thêm làm Content manager của Shared Drive. Không dùng API key thường vì Drive không cho upload bằng loại key đó. Không commit `.env`. | Câu N1, SEC-006 |
| NFR-U03-11 | Mỗi `purpose` một thư mục trên Shared Drive; ID thư mục lấy từ cấu hình hoặc tạo lúc khởi động. | Thiết kế |
| NFR-U03-12 | Lời gọi Drive có timeout: kết nối 5 s, upload 120 s, tải về 60 s mỗi lần đọc. | REL-003 |
| NFR-U03-13 | Lỗi tạm của Drive (429, 5xx) retry tối đa 3 lần với backoff 1, 2, 4 s; lỗi quyền hoặc abuse không retry. | REL-003 |
| NFR-U03-14 | Thiếu hoặc sai cấu hình Drive: backend vẫn khởi động, upload/tải về trả "tạm thời không khả dụng", healthcheck báo Drive lỗi. | REL-002 |
| NFR-U03-15 | Quota API Drive miễn phí của Google đủ cho quy mô ≤ 100 người đồng thời; không cần gói trả phí. | REL-005 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-20 | Parser XML cấu hình tắt DTD, external entity, XInclude. | BR-U03-10, SEC-003 |
| NFR-U03-21 | Nhận dạng loại file bằng magic bytes (Apache Tika). | BR-U03-03 |
| NFR-U03-22 | Download token 256 bit, Redis lưu băm, TTL 5 phút, gắn `accountId`. | BR-U03-21 |
| NFR-U03-23 | Phản hồi tải về có `Content-Disposition` đúng, `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store`. | BR-U03-23 |
| NFR-U03-24 | Không log `providerFileId`, thông tin đăng nhập Drive hay token tải về. | SEC-005 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U03-30 | Unit test mọi `BR-U03-xx`, gồm XML có DOCTYPE/XXE, file đổi đuôi, file > 50 MB, token dùng sai người. | NFR-004 |
| NFR-U03-31 | Drive được thay bằng adapter giả lưu ra thư mục tạm trong test và khi chạy local không có credential. | NFR-004, NFR-005 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U03-24 |
| SECURITY-04 | Compliant | NFR-U03-23 |
| SECURITY-05 | Compliant | Kiểm loại, kích thước, XML |
| SECURITY-08 | Compliant | Unit sở hữu kiểm quyền; token gắn người dùng |
| SECURITY-09 | Compliant | Credential từ `.env`, không commit |
| SECURITY-12 | N/A | U03 không xác thực người dùng |
| SECURITY-15 | Compliant | NFR-U03-14, dọn file khi lỗi |
| RESILIENCY-06 | Compliant | Healthcheck gồm Drive |
| RESILIENCY-10 | Compliant | NFR-U03-12, 13 |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
