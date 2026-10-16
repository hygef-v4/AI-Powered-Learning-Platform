# U03 Job and Settings — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U03 Job and Settings thuộc Iter 1, người code: Người 3; UC 14–15. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038.

Thiết kế: [business-logic-model](../../u03-job-and-settings/functional-design/business-logic-model.md), [business-rules](../../u03-job-and-settings/functional-design/business-rules.md), [domain-entities](../../u03-job-and-settings/functional-design/domain-entities.md), [frontend-components](../../u03-job-and-settings/functional-design/frontend-components.md), [NFR](../../u03-job-and-settings/nfr-requirements/nfr-requirements.md), [NFR design](../../u03-job-and-settings/nfr-design/nfr-design-patterns.md), [infrastructure](../../u03-job-and-settings/infrastructure-design/infrastructure-design.md). Phụ thuộc và thứ tự: [unit-of-work](../../../inception/application-design/unit-of-work.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/files/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../files/contract/` | interface `IArtifact`; kiểu dữ liệu `ArtifactPurpose`, `DownloadToken`, `FileRef`, `FileUpload`, `StoredFileInfo` |
| `backend/.../files/contract/stub/FilesContractStubs` | Bản tạm trả dữ liệu mẫu cho `IArtifact` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/files/` | Chỉ có `index.ts` trống |
| `backend/src/main/java/edu/aiplatform/jobs/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../jobs/contract/` | interface `IEventPublisher`, `IJob`, `IJobHandler`, `IPendingSweeper`, `IScheduledScanner`; kiểu dữ liệu `DomainEvent`, `EventTypes`, `JobMessage`, `JobQueue`, `JobTypes`, `PendingJob` |
| `backend/.../jobs/contract/stub/JobsContractStubs` | Bản tạm trả dữ liệu mẫu cho `IJob`, `IEventPublisher` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `backend/src/main/java/edu/aiplatform/settings/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../settings/contract/` | interface `ISettings`, `ISettingsSectionProvider`; kiểu dữ liệu `SettingDefinition`, `SettingGroup`, `SettingKey`, `SettingType` |
| `backend/.../settings/contract/stub/SettingsContractStubs` | Bản tạm trả dữ liệu mẫu cho `ISettings` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/settings/slots.ts` | Slot đã khai báo: `settingsSectionRegistry` |
| `backend/src/main/java/edu/aiplatform/backup/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../backup/contract/` | interface `IBackupStatus`; kiểu dữ liệu `BackupRunView`, `BackupState` |
| `backend/.../backup/contract/stub/BackupContractStubs` | Bản tạm trả dữ liệu mẫu cho `IBackupStatus` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `backend/.../shared/config/WorkerConfig`, `application-worker.yml` | Profile `worker` bật việc hẹn giờ |
| `backend/.../shared/security/ForbiddenKeyGuard` | Có; dùng chặn khóa cấm trong payload việc nền |
| `infra/` | Compose có `worker` (cùng image backend, profile `worker`), volume `upload-tmp`, `backup-tmp`; RabbitMQ vhost `/platform`, user `app` |

## Checklist

- [ ] **Bước 1 — Tệp (F1–F5):** `FileUploadController`, `UploadService`, `ContentInspector` (Tika), `ArtifactService` cài `IArtifact`, `DownloadController`, `DownloadTokenService` (Redis 5 phút), `IStorage` với adapter Drive, thư mục local và chưa cấu hình; `DriveJobHandler` cho `DRIVE_CLEANUP`.
- [ ] **Bước 2 — Việc nền (J1–J4):** `JobPublisher` cài `IJob` (gửi sau commit), `AmqpPublisher`, `AmqpTopology` (exchange `jobs`, `jobs.retry`, `platform.events`; 5 queue việc nền và queue thử lại 30 giây–8 phút), `JobListener` (ack thủ công), `JobRetryPublisher`, `JobHandlerRegistry`, `PendingSweepRunner`, `ScheduledScanRunner`; `JobQueue`, `JobTypes` đã có trong `jobs/contract`.
- [ ] **Bước 3 — Sự kiện (E1):** `EventPublisher` cài `IEventPublisher`, gửi `platform.events` sau commit; `EventTypes` đã có trong `jobs/contract`.
- [ ] **Bước 4 — Migration:** `db/migration/settings/V20260925_0910__create_system_settings.sql`; `db/migration/backup/V20260925_0920__create_backup_tables.sql`; thêm role `backup_reader` vào `01-roles.sh`.
- [ ] **Bước 5 — Settings backend:** `SettingDefinition`, `SettingDefinitionRegistry` (khởi tạo mục khi khởi động, S4), `SettingsService` (cache 30 giây, cập nhật có điều kiện `version`, audit `SETTING_UPDATED`), `ISettings`, `SettingsController` (`GET /api/v1/admin/settings`, `GET /{key}`, `PATCH /{key}`), khai báo nhóm Tệp và Backup; `ISettingsSectionProvider` và `SettingsSectionRegistry` (timeout 1 giây).
- [ ] **Bước 6 — Giới hạn tệp từ Settings:** `PurposePolicy` và `UploadService` đọc qua `ISettings`; thêm `FilePolicyService` và `GET /api/v1/files/policies`.
- [ ] **Bước 7 — Backup:** `BackupScheduleScanner` (B1), `BackupJobHandler` với timeout 2 giờ (B2, B4), `DumpRunner` (`pg_dump` bằng `backup_reader`), `BackupEncryptor` (AES-256-GCM, khóa bọc RSA-OAEP-256), `BackupStorage` (upload, kiểm checksum, xóa trên thư mục backup), `BackupRetentionService` (B3, giữ 7 ngày), `IPendingSweeper` cho `BACKUP_RUN` kẹt ở `CREATED`, audit `BACKUP_COMPLETED`/`BACKUP_FAILED`, phát `backup.failed`.
- [ ] **Bước 8 — Trạng thái backup:** `BackupStatusService` cài `IBackupStatus` (`latest`, `list`).
- [ ] **Bước 9 — Khôi phục:** runbook `docs/runbooks/restore-isolated.md` và script `infra/scripts/restore-isolated.sh` (giải mã ngoài VPS, `pg_restore`, danh sách kiểm sau khôi phục; không khôi phục tệp Drive).
- [ ] **Bước 10 — Frontend:** `SettingListPage`, `SettingGroupTable`, `DelegatedSectionCard`, `SettingDetailPage`, `SettingValueForm`, `SettingsSectionHost` (vẽ `settingsSectionRegistry` đã khai báo trong `src/features/settings/slots.ts`); `FileUploader`, `useFileUpload`, `FileLink`, `purposes` (lấy giới hạn từ `GET /api/v1/files/policies`).
- [ ] **Bước 11 — Hạ tầng:** image worker cài `postgresql16-client`; biến `GOOGLE_BACKUP_FOLDER_ID`, `U03_BACKUP_PUBLIC_KEY`, `BACKUP_DB_PASSWORD`, `U03_SETTINGS_CACHE_TTL`; cập nhật hướng dẫn chạy local.
- [ ] **Bước 12 — Unit test (người code viết):** kiểm giới hạn từng kiểu cài đặt, version lệch trả `409`, khởi tạo không ghi đè, người không phải Admin bị từ chối, phần ủy quyền lỗi không làm hỏng danh sách; giới hạn tệp đọc từ Settings; scanner không tạo trùng lần chạy trong ngày; trạng thái backup; chỉ giữ bản trong 7 ngày và không xóa bản thành công mới nhất; mã hóa và giải mã bằng khóa test; form Settings ở frontend. Integration test do tester viết.
- [ ] **Bước 13 — Review:** đối chiếu UC 14, 15, US-SET-001, US-OPS-001, BR-U03 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Sửa cài đặt với version cũ, giá trị ngoài giới hạn, bật backup khi chưa đặt giờ; người không phải Admin gọi API Settings.
- Upload tệp vượt trần mới sau khi Admin giảm trần; tải tệp bằng token của người khác.
- Việc nền: gửi sau commit, rollback không gửi, thử lại theo backoff, `onFailed` sau 5 lượt, sweeper gửi lại.
- Backup thành công đầy đủ; Drive lỗi giữa chừng thì `FAILED`, giữ bản tốt trước và có thông báo cho Admin; chỉ giữ bản trong 7 ngày; khôi phục vào môi trường cô lập và kiểm dữ liệu.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
