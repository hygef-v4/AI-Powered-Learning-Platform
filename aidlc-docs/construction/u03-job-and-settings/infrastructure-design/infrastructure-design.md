# U03 Job and Settings — infrastructure-design

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U03 Job and Settings thuộc Iter 1, người code: Người 3. Viết lại từ thiết kế chi tiết 2026-10-09 (bản khớp với code tệp và việc nền đã sinh ngày 2026-10-05) theo phạm vi 66 UC: bỏ avatar và việc nền của Code Lab, PayOS, tóm tắt học liệu; thêm backup và các nhóm cài đặt mới. Chỉ sửa tài liệu; phần Settings và backup chưa có code.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

Hạ tầng chung ở [shared-infrastructure](../../shared-infrastructure.md).

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Controller, service tệp và Settings | `backend` |
| `DriveJobHandler`, `BackupJobHandler`, `BackupScheduleScanner`, runner việc nền | `worker` |
| Bảng `system_settings`, `backup_runs`, `backup_file_entries`, `file_id_aliases` | `postgres` |
| Download token, cache cài đặt (trong bộ nhớ) | `redis` (token), bộ nhớ instance (cache) |
| Exchange và queue | `rabbitmq` |
| Byte tệp đang dùng | Google Shared Drive, mỗi `purpose` một thư mục |
| Bản backup | Thư mục Drive riêng `GOOGLE_BACKUP_FOLDER_ID` |

## 2. Google Drive

| Mục | Giá trị |
|---|---|
| Xác thực | JSON key service account (base64) trong `.env` |
| Thư mục tệp | `MATERIAL`, `DOCUMENT_IMAGE`, tạo lúc khởi động nếu chưa có |
| Thư mục backup | `backup/{runId}/` chứa archive mã hóa, manifest mã hóa và `files/` (bản sao tệp) |
| Kết nối ra | `www.googleapis.com:443` từ backend và worker |

## 3. Container và volume

| Mục | Giá trị |
|---|---|
| Volume `upload-tmp` | `backend` tại `/tmp/uploads`, giới hạn 1 GB |
| Volume `backup-tmp` | `worker` tại `/tmp/backup`, đủ ≥ 2 lần kích thước database |
| Volume `files-local` | Chỉ khi chạy local không có key Drive |
| Nginx | `client_max_body_size 50m`, `proxy_request_buffering off` cho `/api/v1/files`, `proxy_read_timeout 120s` |
| Image worker | Cùng image backend, profile `worker`, cài thêm `postgresql16-client` cho `pg_dump`/`pg_restore` |

## 4. Migration

| File | Nội dung |
|---|---|
| `db/migration/settings/V20260925_0910__create_system_settings.sql` | Bảng `system_settings`; FK `updated_by` do migration `accounts` của U01 thêm |
| `db/migration/backup/V20260925_0920__create_backup_tables.sql` | `backup_runs` (unique `scheduled_for`), `backup_file_entries`, `file_id_aliases` |
| Script khởi tạo role (`01-roles.sh`) | Thêm role `backup_reader` chỉ có `SELECT` trên mọi bảng |

Thứ tự: `audit_logs` (U02, 0900) → `system_settings` (0910) → bảng backup (0920) → `accounts` (U01, 0930).

## 5. RabbitMQ

| Mục | Giá trị |
|---|---|
| vhost, user | `/platform`, user `app`; tắt `guest` |
| Exchange | `jobs` (direct), `jobs.retry` (direct), `platform.events` (topic) |
| Queue việc nền | `jobs.triggered`, `jobs.email` (priority 10), `jobs.ai`, `jobs.drive`, `jobs.backup` |
| Queue thử lại | `jobs.retry.30s`, `.1m`, `.2m`, `.4m`, `.8m` có TTL, dead-letter về `jobs` |
| Queue thông báo | `jobs.notification` do U14 khai báo, bind `platform.events` |
| Queue bỏ so với bản cũ | `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.payos` |

## 6. Worker

| Mục | Giá trị |
|---|---|
| Số instance | 1 |
| Giới hạn | 1 CPU, 1,5 GB |
| Health | PostgreSQL, RabbitMQ |
