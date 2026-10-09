# U03 File, Job & Event - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 70, 71 (Settings, người dùng chốt U03 giữ ngày 2026-10-09) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-SET-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Google Drive | Google Drive API v3 Java client + google-auth-library, xác thực bằng JSON key service account từ `.env` | Chính thức; một key duy nhất, không phụ thuộc tài khoản cá nhân |
| Nhận dạng loại file | Apache Tika core | Đọc magic bytes, không cần cả bộ parser |
| Upload | Spring `MultipartFile` ghi ra thư mục tạm (`spring.servlet.multipart.file-size-threshold = 0`) | Không giữ file trong RAM |
| Giới hạn đồng thời | `Semaphore` 5 permit trong service upload | Đơn giản, đủ cho một instance backend |
| Download token | Redis, TTL 5 phút | Đã có Redis |
| Local/test | `LocalFolderStorageAdapter` thay Drive | Chạy được khi chưa có credential |
| Cài đặt hệ thống | Bảng `system_settings` (cột `value` jsonb) + cache trong bộ nhớ tự viết, TTL 30 s | Ít mục, đọc nhiều sửa ít; không cần thêm thư viện cache |

## Việc nền (chuyển từ U02)

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Messaging | Spring AMQP với RabbitMQ | Đã chốt RabbitMQ; Spring AMQP có ack thủ công, prefetch, tự kết nối lại |
| Thử lại | Queue `jobs.retry.*` có TTL, dead-letter về `jobs` | Không có bảng job (database chỉ gồm bảng của ERD); không cần plugin delayed message |
| Sweeper, scanner | `@Scheduled` trong worker | Chỉ một worker, không cần khóa phân tán |
| Worker | Cùng project Maven backend, profile `worker` | Không phải duy trì hai codebase |
| Test | JUnit 5, Testcontainers (PostgreSQL, RabbitMQ) | Theo NFR-004 |
