# U03 File & Artifact - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --(không đệm, 50 MB)--> [backend]
                                                     |  /tmp/uploads (volume)
                                                     |
                               +---------------------+----------------+
                               v                     v                v
                          [postgres]             [redis]     Google Shared Drive
                          artifacts              u03:dl:*    (qua Internet, 443)
                                                                  ^
                                         [worker] --DriveJobHandler+
```

**Text alternative**: Trình duyệt gửi file qua HTTPS tới Nginx; Nginx chuyển thẳng (không đệm) tới backend với giới hạn 50 MB. Backend ghi file tạm vào volume `/tmp/uploads`, lưu metadata vào PostgreSQL, token tải về vào Redis và byte file lên Google Shared Drive qua Internet. Worker xử lý job xóa và dọn file trên Drive.

## Luồng upload
1. Nginx chuyển stream sang backend.
2. Backend ghi `/tmp/uploads`, kiểm tra, đẩy lên Drive, ghi `artifacts`, xóa file tạm.

## Luồng tải về
1. Unit sở hữu cấp token.
2. Trình duyệt gọi `/api/v1/files/download/{token}`; backend stream từ Drive qua Nginx.

## Chạy local không có key
- Backend tự dùng `LocalFolderStorageAdapter`, lưu vào volume `files-local`.
