# U03 Job and Settings — code summary

Code viết ngày 2026-10-16 theo [plan](../../plans/code-generation-plan/u03-job-and-settings-code-generation-plan.md). Chưa chạy với PostgreSQL, Redis, RabbitMQ, Google Drive thật; integration test do tester viết.

## Business logic

| Luồng | Class chính |
|---|---|
| F1 Upload | `UploadService` (5 upload đồng thời, ghi tệp tạm, kiểm trần và loại theo Settings, SHA-256, lưu, ký `FileRef`, dọn khi lỗi), `ContentInspector` (Tika), `PurposePolicy`, `FileNameSanitizer` |
| F2–F4 Gắn, tải, đọc | `ArtifactService` (cài `IArtifact`), `FileRefSigner` (HMAC, hạn 1 giờ), `DownloadTokenService` (Redis 5 phút, lưu băm), `DownloadService` (header an toàn, CSP cho SVG, abuse trả 410) |
| F5 Giới hạn | `FilePolicyService` |
| Lưu tệp | `IStorage`: `GoogleDriveStorageAdapter` (thử lại 429, 5xx), `LocalFolderStorageAdapter`, `UnconfiguredStorageAdapter`; `DriveCleanupJobHandler` (`DRIVE_CLEANUP`); `DriveHealthIndicator` |
| J1–J4 Việc nền | `JobPublisher` (cài `IJob`, gửi sau commit), `AfterCommitExecutor`, `JobRouting`, `JobDispatcher` (timeout, thử lại 30 giây–8 phút, `onFailed` sau lượt 5), `JobHandlerRegistry`, `JobListener` (worker, ack thủ công), `PendingSweepRunner`, `ScheduledScanRunner` |
| E1 Sự kiện | `EventPublisher` (cài `IEventPublisher`) |
| S1–S5 Settings | `SettingsService` (cài `ISettings`, cache 30 giây, chỉ `ADMIN`, sửa có `version`, audit), `SettingValidator`, `SettingDefinitionRegistry` (khởi tạo mục còn thiếu), `SettingsSectionRegistry` (timeout 1 giây); nhóm Tệp `FileSettingDefinitions`, nhóm Backup `BackupSettingDefinitions` |
| B1–B5 Backup | `BackupScheduleScanner`, `BackupJobHandler` (dump, mã hóa, lưu, đối chiếu MD5, giữ 7 ngày; lỗi thì `FAILED`, dọn, audit, `backup.failed`), `BackupEncryptor` (AES-256-GCM theo khối 1 MiB, khóa bọc RSA-OAEP-256), `BackupRetentionService`, `BackupPendingSweeper`, `BackupStatusService` (cài `IBackupStatus`), `PgDumpRunner`, `DriveBackupStore`, `LocalBackupStore`, `BackupDecryptTool` |

## API

| Endpoint | Ghi chú |
|---|---|
| `POST /api/v1/files` | Multipart `purpose`, `file`; trả `fileRef`, loại, dung lượng, tên, hạn |
| `GET /api/v1/files/policies` | Trần và loại cho phép từng `purpose` |
| `GET /api/v1/files/download/{token}` | Stream tệp; token đúng người, 5 phút |
| `GET /api/v1/admin/settings`, `GET /{key}`, `PATCH /{key}` | UC 14, 15; lỗi trả Problem Detail kèm `code` |

## Repository và migration

- `SystemSettingRepository`, `BackupRunRepository` dùng `JdbcClient`; chuyển trạng thái backup là cập nhật có điều kiện.
- `db/migration/settings/V20260925_0910__create_system_settings.sql` (cột nhóm tên `setting_group`), `db/migration/backup/V20260925_0920__create_backup_tables.sql` (thêm `created_at` cho sweeper).

## Frontend

- `app/admin/settings` (Setting List), `app/admin/settings/[key]` (Setting Detail), `app/admin/settings/sections/[sectionKey]` (panel ủy quyền).
- `features/settings`: `SettingGroupTable`, `DelegatedSectionCard`, `SettingValueForm` (kiểm sơ bộ, 400, 409), `SettingsSectionHost`.
- `features/files`: `FileUploader` (kéo thả, tiến trình, Hủy), `useFileUpload`, `FileLink`, `purposes`.
- `shared/status`: `StatusBadge`, `usePollStatus` (3 giây, dừng ở trạng thái cuối).
- Giao diện theo design G21 (17/10): `shared/ui` (màu, nút, thẻ, bảng, ô nhập, nhãn, popup, icon Material Symbols dạng SVG tự host) và `shared/layout/AppShell` (thanh trên cùng, menu bên theo vai trò, popup Đăng xuất), gắn cho mọi màn `/admin/*` qua `app/admin/layout.tsx`. Menu bên lấy từ `NavEntry` (slot của U01); mỗi unit đăng ký mục của mình trong `features/<package>/register.ts` (đã đăng ký sẵn đủ mục theo design). Unit khác dùng lại, không tự vẽ khung.

## Hạ tầng

- Image backend cài `postgresql16-client`; compose truyền thư mục tạm upload, backup; role `backup_reader` chỉ `SELECT`.
- Chạy local không có Drive: tệp ở volume `files-local`, backup ở `backups-local`.
- Runbook `docs/runbooks/restore-isolated.md`, script `infra/scripts/restore-isolated.sh`.

## Unit test

- Backend 84 test: tên tệp, giới hạn theo Settings, nhận dạng theo nội dung (đổi đuôi, SVG có script), `FileRef` sai hoặc hết hạn, upload (role, vượt trần, sai loại, Drive lỗi, dọn tệp), token sai người, header tải, việc nền (gửi sau commit, rollback không gửi, khóa cấm, thử lại, hết lượt, timeout), Settings (giới hạn từng kiểu, ràng buộc backup, version lệch, chỉ Admin, cache 30 giây, khởi tạo không ghi đè, phần ủy quyền lỗi), backup (lịch theo giờ Việt Nam, không trùng ngày, giữ 7 ngày và bản mới nhất, mã hóa và giải mã bằng khóa test, archive bị sửa hoặc cắt, lỗi thì báo).
- Frontend 30 test: kiểm giá trị cài đặt, hiện giờ Việt Nam, form (lỗi giới hạn ngay khi nhập, 400, 409, Hủy, chọn nhiều, lưu), bảng nhóm cài đặt, giới hạn tệp, `FileUploader`, `usePollStatus`, khung trang (menu theo vai trò, mục đang mở, Đăng xuất), mục menu đúng thứ tự design, popup, công tắc, ô chọn nhiều.

## Khác với thiết kế

- Mỗi mức chờ thử lại có exchange fanout riêng `jobs.retry.<mức>` thay cho một exchange `jobs.retry`, để giữ routing key loại việc khi dead-letter về `jobs` (đã sửa infrastructure-design).
- `FileRef` mang thêm thuộc tính tệp nên `attach` không gọi Drive (đã sửa domain-entities).
- Drive lỗi thì health chi tiết `drive: DEGRADED` nhưng backend vẫn `UP`.

## U15 read capability addendum — 2026-10-22

User approved additive IBackupDashboardStatus plus metadata/summary records. BackupStatusService implements old and new ports; old IBackupStatus/BackupRunView remain unchanged. New reads map real startedAt, sanitize public error and use read-only transaction timeout1s. Summary uses one parameterized SQL statement with168h [from,to), historical SUCCESS completedAt including purged runs, final FAILED count/latest safe error and terminal timestamp integrity. No migration/job/backup action changes.

[Execution evidence](../../u15-admin-dashboard/code/backup-completion-verification-2026-10-22.md): PostgreSQL16 integration8 passed, including all three read timeouts and connection release; full backend verify passed with35 unrelated configured-environment skips. Full app/browser/load acceptance remains open.

## Thay đổi 2026-10-24

- Bỏ cài đặt `files.material.allowedTypes` và `files.documentImage.allowedTypes`: loại tệp cố định trong `PurposePolicy`, học liệu PDF, DOCX, PPTX; ảnh trong tài liệu PNG, JPEG (bỏ GIF, SVG). Vẫn giữ cài đặt dung lượng tối đa.
- Thêm Drive cá nhân (Gmail) bên cạnh Shared Drive: `GOOGLE_DRIVE_OAUTH_CLIENT_ID`, `GOOGLE_DRIVE_OAUTH_CLIENT_SECRET`, `GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN` (scope `drive.file`). `DriveFolders` tạo thư mục `AI Learning Platform` trong My Drive, mỗi purpose một thư mục con, backup trong `backups`; có cả hai cấu hình thì dùng Shared Drive. Khác thiết kế: thiết kế chỉ có Shared Drive với service account.
