# U03 File, Job & Event - Infrastructure Design

**Bản tài liệu 2026-10-08**: không primary UC; primary stories: không primary story. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| `FileUploadController`, `UploadService`, `ContentInspector`, `DownloadController`, `DownloadTokenService`, `ArtifactService` | `backend` |
| `DriveJobHandler` | `worker` |
| `JobPublisher`, `EventPublisherPort`, `AmqpPublisher` | `backend`, `worker` |
| `JobListener`, `JobRetryPublisher`, `JobHandlerRegistry`, `PendingSweepRunner`, `ScheduledScanRunner` | `worker` |
| Exchange, queue | `rabbitmq` |
| Metadata tệp | Thuộc tính `appProperties` của tệp trên Google Shared Drive; không có bảng PostgreSQL |
| Download token | `redis`, tiền tố `file:download-token:` |
| Byte file | Google Shared Drive (ngoài VPS); local dùng volume `files-local` |

## 2. Google Drive

| Mục | Giá trị |
|---|---|
| Xác thực | JSON key service account trong `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY` (base64) |
| Shared Drive | `GOOGLE_SHARED_DRIVE_ID`; service account là Content manager |
| Thư mục | Mỗi `purpose` một thư mục, tạo lúc khởi động nếu chưa có |
| Kết nối ra ngoài | Backend và worker cần đi được tới `www.googleapis.com:443`; firewall VPS không chặn chiều ra |
| Chi phí | Dùng quota miễn phí của Drive API và dung lượng Shared Drive của trường |

## 3. Container

| Mục | Giá trị |
|---|---|
| Volume `upload-tmp` | Gắn vào `backend` tại `/tmp/uploads`, giới hạn 1 GB (5 × 50 MB có dư) |
| Volume `files-local` | Chỉ khi chạy local không có key; gắn tại `./data/files` |
| Nginx | `client_max_body_size 50m`; `proxy_request_buffering off` cho `/api/v1/files` để không đệm hai lần; `proxy_read_timeout 120s` |
| Backend | `spring.servlet.multipart.max-file-size=50MB`, `max-request-size=51MB`, `file-size-threshold=0`, `location=/tmp/uploads` |

## 4. Migration

U03 không có migration: không có bảng PostgreSQL. Bảng sở hữu lưu `file_id` (xem [mô hình dữ liệu của unit](../functional-design/domain-entities.md)).

## 5. RabbitMQ (chuyển từ U02)

| Mục | Giá trị |
|---|---|
| vhost | `/platform` |
| User | `app` với quyền trên `/platform`; tắt `guest` |
| Exchange | `jobs` (direct), `jobs.retry` (direct), `platform.events` (topic) durable; `platform.realtime` (fanout) do U14 khai báo |
| Queue việc nền | 7 queue durable do U03 khai báo: `jobs.triggered`, `jobs.email` (priority), `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.drive`, `jobs.payos`; mỗi unit đăng ký `jobType` → queue |
| Queue thử lại | 5 queue không consumer `jobs.retry.30s`, `.1m`, `.2m`, `.4m`, `.8m` có TTL cố định, dead-letter về exchange `jobs` giữ nguyên routing key |
| Queue thông báo | `jobs.notification` do U16 khai báo, bind `platform.events` |
| Khai báo | Spring AMQP khai báo lúc khởi động bằng `Declarables`; không cấu hình tay |
| Management UI | Cổng 15672 chỉ trên mạng `internal`, vào qua SSH tunnel |

## 6. Worker

| Mục | Giá trị |
|---|---|
| Image | Cùng image backend, biến `SPRING_PROFILES_ACTIVE=worker` |
| Giới hạn | 1 CPU, 1,5 GB (như shared-infrastructure; VPS < 8 GB: 1 GB) |
| Mạng | Chỉ `internal` |
| Healthcheck | `/health` kiểm PostgreSQL, RabbitMQ |
| Số instance | 1 (scanner và sweeper không cần khóa phân tán) |

## 7. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-09 | Compliant | Key từ `.env`, không commit; Drive không chia sẻ công khai |
| RESILIENCY-04 | Compliant | Deploy cùng Compose |
| RESILIENCY-06 | Compliant | Healthcheck có Drive |
| Rule còn lại | N/A | Đã xử lý ở mức ứng dụng hoặc ngoài phạm vi đồ án |
