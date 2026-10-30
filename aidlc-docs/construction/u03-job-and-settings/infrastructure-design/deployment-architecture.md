# U03 Job and Settings — deployment-architecture

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

```
browser --> nginx --> backend : upload, tải về, Settings
backend --> Google Drive (thư mục MATERIAL, DOCUMENT_IMAGE) : lưu và đọc tệp
backend --> RabbitMQ (exchange jobs, platform.events) : việc nền, sự kiện
RabbitMQ --> worker : jobs.triggered, jobs.email, jobs.ai, jobs.drive, jobs.backup
worker --> PostgreSQL : pg_dump bằng role backup_reader
worker --> Google Drive (thư mục backup riêng) : archive dump đã mã hóa
backend --> PostgreSQL : system_settings, backup_runs
```

**Text alternative**: Trình duyệt đi qua Nginx tới backend để upload, tải tệp và sửa Settings. Backend lưu và đọc tệp trên Google Drive trong thư mục theo mục đích, gửi việc nền và sự kiện lên RabbitMQ. Worker nhận việc từ 5 queue. Khi backup, worker dump PostgreSQL bằng role chỉ đọc và upload archive đã mã hóa vào thư mục Drive riêng cho backup. Backend đọc và ghi bảng cài đặt và bảng lịch sử backup trong PostgreSQL.

Khôi phục chạy theo runbook ở môi trường cô lập, không đi qua backend production.
