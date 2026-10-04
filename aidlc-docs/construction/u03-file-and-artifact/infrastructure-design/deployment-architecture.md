# U03 File, Job & Event - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --(không đệm, 50 MB)--> [backend]
                                                     |  /tmp/uploads (volume)
                                                     |
                               +---------------------+----------------+
                               v                                      v
                           [redis]                          Google Shared Drive
                           file:download-token:*            byte + appProperties (443)
                                                                  ^
                                         [worker] --DriveJobHandler+
```

**Text alternative**: Trình duyệt gửi file qua HTTPS tới Nginx; Nginx chuyển thẳng (không đệm) tới backend với giới hạn 50 MB. Backend ghi file tạm vào volume `/tmp/uploads`, đẩy byte file và metadata (`appProperties`) lên Google Shared Drive qua Internet, lưu token tải về vào Redis. U03 không ghi PostgreSQL. Worker xử lý việc dọn file Drive còn sót khi upload lỗi.

## Luồng upload
1. Nginx chuyển stream sang backend.
2. Backend ghi `/tmp/uploads`, kiểm tra, đẩy lên Drive kèm `appProperties`, xóa file tạm, trả `FileRef`.

## Luồng tải về
1. Unit sở hữu cấp token.
2. Trình duyệt gọi `/api/v1/files/download/{token}`; backend stream từ Drive qua Nginx.

## Chạy local không có key
- Backend tự dùng `LocalFolderStorageAdapter`, lưu vào volume `files-local`.

## Việc nền (chuyển từ U02)

```
 mạng internal
 +--------------------------------------------------------------+
 |  [backend] --ghi dòng nghiệp vụ--> [postgres]|
 |      |                                          ^            |
 |      | afterCommit                              |            |
 |      v                                          |            |
 |  [rabbitmq]  7 queue jobs.* + jobs.retry.* --> [worker]      |
 |                       <-- sweeper / scanner đọc bảng --      |
 +--------------------------------------------------------------+
```

**Text alternative**: Backend ghi dòng nghiệp vụ vào PostgreSQL trong transaction, rồi gửi message việc nền sang RabbitMQ sau commit. Worker nhận message từ 7 queue `jobs.*`; lỗi tạm đi qua các queue `jobs.retry.*` có TTL rồi quay lại queue gốc. Worker chạy sweeper gửi lại việc bị mất và scanner cho việc hẹn giờ bằng cách đọc bảng nghiệp vụ. Cả bốn container nằm trên mạng `internal`, không container nào publish cổng ra ngoài.

## Luồng việc nền

1. Backend ghi dòng nghiệp vụ ở trạng thái chờ trong transaction nghiệp vụ, commit.
2. `afterCommit` gửi `JobMessage` sang exchange `jobs`.
3. Worker nhận, handler cập nhật dòng nghiệp vụ, ack.

## Khi RabbitMQ khởi động lại

- Message persistent vẫn còn.
- Message gửi lỗi trong lúc RabbitMQ tắt: dòng nghiệp vụ vẫn ở trạng thái chờ, sweeper gửi lại sau tối đa 5 phút. Việc hẹn giờ đọc bảng nên không bị ảnh hưởng. Audit không đi qua RabbitMQ; chỉ event thông báo trong khoảng đó có thể mất.
