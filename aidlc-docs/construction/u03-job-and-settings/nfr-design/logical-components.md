# U03 Job and Settings — logical-components

> Current source baseline 2026-10-30, staging@794a4053; ownership theo unit-of-work hiện hành. Có implementation source; chưa chạy lại runtime acceptance trong đợt sync. Ước lượng và kết quả có ngày cũ giữ theo phạm vi lịch sử.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013, FR-033, FR-038; REL-001/002/003/004.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [requirements](../../../inception/requirements/requirements.md), [council features](../../../inception/requirements/council-features-2026-10-13.md).

## 1. Thành phần

| Thành phần | Chạy ở | Trách nhiệm |
|---|---|---|
| `FileUploadController`, `UploadService`, `ContentInspector` | backend | F1 upload (P1, P3) |
| `ArtifactService` | backend, worker | `IArtifact`: `store`, `attach`, `issueDownloadToken`, `open` |
| `DownloadController`, `DownloadTokenService` | backend | F3 tải về (P2, P4) |
| `FilePolicyService` | backend | F5 `GET /api/v1/files/policies`, đọc giới hạn qua `ISettings` |
| `IStorage` + adapter Drive, local, chưa cấu hình | backend, worker | P5, P6 |
| `DriveJobHandler` | worker | `DRIVE_CLEANUP` |
| `JobPublisher`, `EventPublisher`, `AmqpPublisher`, `AmqpTopology` | backend, worker | J1, E1, P8 |
| `JobListener`, `JobRetryPublisher`, `JobHandlerRegistry`, `PendingSweepRunner`, `ScheduledScanRunner` | worker | J2–J4, P9–P12 |
| `SettingsController`, `SettingsService`, `SettingDefinitionRegistry`, `SettingsSectionRegistry` | backend (worker đọc qua `ISettings`) | S1–S5, P14, P15 |
| `BackupScheduleScanner` | worker | B1 |
| `BackupJobHandler`, `DumpRunner`, `BackupEncryptor`, `BackupStorage` | worker | B2, B4, P16, P17 |
| `BackupRetentionService` | worker | B3, P18 |
| `BackupStatusService` | backend | `IBackupStatus` cho U15 |

## 2. Cấu hình

| Biến | Ý nghĩa |
|---|---|
| `GOOGLE_DRIVE_SERVICE_ACCOUNT_KEY`, `GOOGLE_SHARED_DRIVE_ID` | Drive cho tệp đang dùng |
| `GOOGLE_BACKUP_FOLDER_ID` | Thư mục backup riêng |
| `U03_FILEREF_SECRET` | Khóa ký `FileRef` |
| `U03_REQUIRE_DRIVE` | Production bắt buộc có Drive |
| `U03_JOB_HANDLER_TIMEOUT` | Timeout mặc định của handler |
| `U03_SETTINGS_CACHE_TTL` | 30 giây |
| `U03_BACKUP_PUBLIC_KEY` | Public key RSA bọc khóa archive |
| `BACKUP_DB_PASSWORD` | Mật khẩu role `backup_reader` |

## Design assessment scope — 2026-10-30

Các bảng Compliant/Pending ở trên là đánh giá thiết kế hoặc evidence có ngày trước baseline, không chứng nhận runtime tại HEAD. Đợt này chỉ đồng bộ tài liệu; hiệu lực security/resiliency, latency/capacity và provider acceptance phải có kiểm chứng riêng. PBT vẫn disabled.
