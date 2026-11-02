# U03 Job and Settings — tech-stack-decisions

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Google Drive | Google Drive API v3 Java client + google-auth-library, JSON key service account | Client chính thức cho Java |
| Nhận dạng loại tệp | Apache Tika core | Đọc magic bytes |
| Upload | Spring `MultipartFile` ghi ra thư mục tạm | Không giữ tệp trong RAM |
| Giới hạn đồng thời | `Semaphore` 5 permit | Đủ cho một instance backend |
| Download token | Redis, TTL 5 phút | Đã có Redis |
| Local và test | `LocalFolderStorageAdapter` thay Drive | Chạy được khi chưa có credential |
| Messaging | Spring AMQP với RabbitMQ, ack thủ công | Đã có trong code |
| Thử lại | Queue `jobs.retry.*` có TTL, dead-letter về `jobs` | Không cần bảng job hay plugin |
| Sweeper, scanner | `@Scheduled` trong worker | Một worker, không cần khóa phân tán |
| Cài đặt hệ thống | Bảng `system_settings` (`value` jsonb) + cache trong bộ nhớ TTL 30 giây | Ít mục, đọc nhiều sửa ít |
| Dump database | `pg_dump` và `pg_restore` của PostgreSQL 16 cài trong image worker | Công cụ chuẩn, snapshot nhất quán |
| Mã hóa backup | JCE có sẵn trong Java: AES-256-GCM cho dữ liệu, RSA-OAEP-256 bọc khóa | Không thêm thư viện; private key giữ ngoài VPS |
| Test | JUnit 5; Testcontainers (PostgreSQL, RabbitMQ) cho integration test do tester viết | Theo NFR-004 |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
