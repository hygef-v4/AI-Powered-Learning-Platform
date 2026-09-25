# U03 File & Artifact - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Google Drive | Google Drive API v3 Java client + google-auth-library, xác thực bằng JSON key service account từ `.env` | Chính thức; một key duy nhất, không phụ thuộc tài khoản cá nhân |
| Nhận dạng loại file | Apache Tika core | Đọc magic bytes, không cần cả bộ parser |
| Upload | Spring `MultipartFile` ghi ra thư mục tạm (`spring.servlet.multipart.file-size-threshold = 0`) | Không giữ file trong RAM |
| Giới hạn đồng thời | `Semaphore` 5 permit trong service upload | Đơn giản, đủ cho một instance backend |
| Download token | Redis, TTL 5 phút | Đã có Redis |
| Local/test | `LocalFolderStorageAdapter` thay Drive | Chạy được khi chưa có credential |
