# U03 Job and Settings — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U03 Job and Settings thuộc Iter 1, người code: Người 3; UC 14–15. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038.

Thiết kế: [business-logic-model](../../u03-job-and-settings/functional-design/business-logic-model.md), [business-rules](../../u03-job-and-settings/functional-design/business-rules.md), [domain-entities](../../u03-job-and-settings/functional-design/domain-entities.md), [frontend-components](../../u03-job-and-settings/functional-design/frontend-components.md), [NFR](../../u03-job-and-settings/nfr-requirements/nfr-requirements.md), [NFR design](../../u03-job-and-settings/nfr-design/nfr-design-patterns.md), [infrastructure](../../u03-job-and-settings/infrastructure-design/infrastructure-design.md). Phụ thuộc và thứ tự: [unit-of-work](../../../inception/application-design/unit-of-work.md).

## Trạng thái

Phần tệp và việc nền đã được code ngày 2026-10-05 theo plan cũ (69 test backend và 18 test frontend chạy qua, xem [test-results](../../u03-job-and-settings/code/test-results.md)); code đó giữ nguyên làm nền. Plan này chỉ gồm phần thay đổi theo unit mới, chưa được duyệt và chưa code.

## Code hiện có

| Path | Hiện trạng | Việc trong plan này |
|---|---|---|
| `backend/.../files/` (upload, tải về, `ArtifactPort`, adapter Drive/local, `DriveJobHandler`) | Đã code, có test | Bỏ avatar; đọc giới hạn từ Settings; thêm đổi mã tệp sau khôi phục |
| `backend/.../files/port/AvatarPort`, `ArtifactPurpose.AVATAR` | Có | Xóa (hồ sơ không còn ảnh đại diện) |
| `backend/.../files/domain/PurposePolicy` | Giới hạn cố định trong code | Lấy trần và loại cho phép qua `SettingsPort`, giữ trần cứng 50 MB và 5 MB |
| `backend/.../jobs/` (`JobPort`, `EventPublisherPort`, listener, retry, sweeper, scanner) | Đã code, có test | Đổi danh sách queue, loại việc, loại sự kiện |
| `frontend/src/shared/files`, `shared/status` | Đã code, có test | Bỏ `AVATAR` khỏi `purposes.ts`; `FileUploader` lấy giới hạn từ API policies |
| Settings, backup | Chưa có | Làm mới toàn bộ |

## Checklist

- [ ] **Bước 1 — Bỏ avatar:** xóa `AvatarPort`, `ArtifactPurpose.AVATAR` và nhánh avatar trong `ArtifactService`, `PurposePolicy`, `purposes.ts`; sửa test liên quan.
- [ ] **Bước 2 — Queue và loại việc:** `JobQueue` còn `TRIGGERED`, `EMAIL`, `AI` (`jobs.ai`, thay `GEMINI`), `DRIVE`, `BACKUP` (`jobs.backup`, mới); bỏ `YOUTUBE`, `CODE`, `PAYOS`. `JobTypes` bỏ `LESSON_SCAN`, `YOUTUBE_CAPTION`, `PAYOS_CHECK`, `CODE_RUN`, thêm `BACKUP_RUN`, `MATERIAL_TEXT_EXTRACT` (U13), sửa chú thích unit sở hữu (U06, U12, U14). Cập nhật `JobRouting`, `AmqpTopology` (5 queue, số luồng theo P11) và test.
- [ ] **Bước 3 — Loại sự kiện:** `EventTypes` bỏ `PAYMENT_PAID`, thêm `QUIZ_PUBLISHED` (U10), `BACKUP_FAILED` (U03), sửa chú thích unit phát theo domain-entities mục 6.
- [ ] **Bước 4 — Migration:** `db/migration/settings/V20260925_0910__create_system_settings.sql`; `db/migration/backup/V20260925_0920__create_backup_tables.sql`; thêm role `backup_reader` vào `01-roles.sh`.
- [ ] **Bước 5 — Settings backend:** `SettingDefinition`, `SettingDefinitionRegistry` (khởi tạo mục khi khởi động, S4), `SettingsService` (cache 30 giây, cập nhật có điều kiện `version`, audit `SETTING_UPDATED`), `SettingsPort`, `SettingsController` (`GET /api/v1/admin/settings`, `GET /{key}`, `PATCH /{key}`), khai báo nhóm Tệp và Backup; `SettingsSectionProvider` và `SettingsSectionRegistry` (timeout 1 giây).
- [ ] **Bước 6 — Giới hạn tệp từ Settings:** `PurposePolicy` và `UploadService` đọc qua `SettingsPort`; thêm `FilePolicyService` và `GET /api/v1/files/policies`.
- [ ] **Bước 7 — Backup:** `BackupScheduleScanner` (B1), `BackupJobHandler` với timeout 2 giờ (B2, B4), `DumpRunner` (`pg_dump` bằng `backup_reader`), `BackupEncryptor` (AES-256-GCM, khóa bọc RSA-OAEP-256), `BackupStorage` (upload, kiểm checksum, xóa trên thư mục backup), `BackupRetentionService` (B3, giữ 7 ngày), `PendingSweeper` cho `BACKUP_RUN` kẹt ở `CREATED`, audit `BACKUP_COMPLETED`/`BACKUP_FAILED`, phát `backup.failed`.
- [ ] **Bước 8 — Trạng thái backup:** `BackupStatusService` cài `BackupStatusPort` (`latest`, `list`).
- [ ] **Bước 9 — Khôi phục:** runbook `docs/runbooks/restore-isolated.md` và script `infra/scripts/restore-isolated.sh` (giải mã ngoài VPS, `pg_restore`, danh sách kiểm sau khôi phục; không khôi phục tệp Drive).
- [ ] **Bước 10 — Frontend:** `SettingListPage`, `SettingGroupTable`, `DelegatedSectionCard`, `SettingDetailPage`, `SettingValueForm`, `SettingsSectionHost`, `settingsSectionRegistry`; `FileUploader` lấy giới hạn từ `GET /api/v1/files/policies`.
- [ ] **Bước 11 — Hạ tầng:** image worker cài `postgresql16-client`; volume `backup-tmp`; biến `GOOGLE_BACKUP_FOLDER_ID`, `U03_BACKUP_PUBLIC_KEY`, `BACKUP_DB_PASSWORD`, `U03_SETTINGS_CACHE_TTL`; cập nhật hướng dẫn chạy local.
- [ ] **Bước 12 — Unit test (người code viết):** kiểm giới hạn từng kiểu cài đặt, version lệch trả `409`, khởi tạo không ghi đè, người không phải Admin bị từ chối, phần ủy quyền lỗi không làm hỏng danh sách; giới hạn tệp đọc từ Settings; scanner không tạo trùng lần chạy trong ngày; trạng thái backup; chỉ giữ bản trong 7 ngày và không xóa bản thành công mới nhất; mã hóa và giải mã bằng khóa test; form Settings ở frontend. Integration test do tester viết.
- [ ] **Bước 13 — Review:** đối chiếu UC 14, 15, US-SET-001, US-OPS-001, BR-U03 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Sửa cài đặt với version cũ, giá trị ngoài giới hạn, bật backup khi chưa đặt giờ; người không phải Admin gọi API Settings.
- Upload tệp vượt trần mới sau khi Admin giảm trần; tải tệp bằng token của người khác.
- Việc nền: gửi sau commit, rollback không gửi, thử lại theo backoff, `onFailed` sau 5 lượt, sweeper gửi lại.
- Backup thành công đầy đủ; Drive lỗi giữa chừng thì `FAILED`, giữ bản tốt trước và có thông báo cho Admin; chỉ giữ bản trong 7 ngày; khôi phục vào môi trường cô lập và kiểm dữ liệu.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
