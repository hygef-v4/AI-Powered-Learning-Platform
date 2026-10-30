# U03 Job and Settings — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 14–15; FR-013, FR-033, FR-038.

files/jobs/settings/backup có source và Flyway schema. Settings hiện hành lấy từ SettingDefinition trong source, không từ danh sách cũ; activity.retentionDays không còn được khai báo. Drive private upload/download và backup encryption cần runtime secrets; không chứng nhận restore từ việc có script. CI deploy vẫn disabled.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/backup/application/BackupRetentionService.java](../../../../backend/src/main/java/edu/aiplatform/backup/application/BackupRetentionService.java)
- [backend/src/main/java/edu/aiplatform/backup/application/BackupStatusService.java](../../../../backend/src/main/java/edu/aiplatform/backup/application/BackupStatusService.java)
- [backend/src/main/java/edu/aiplatform/files/application/ArtifactService.java](../../../../backend/src/main/java/edu/aiplatform/files/application/ArtifactService.java)
- [backend/src/main/java/edu/aiplatform/files/application/DownloadService.java](../../../../backend/src/main/java/edu/aiplatform/files/application/DownloadService.java)
- [backend/src/main/java/edu/aiplatform/files/application/DownloadTokenService.java](../../../../backend/src/main/java/edu/aiplatform/files/application/DownloadTokenService.java)
- [backend/src/main/java/edu/aiplatform/files/application/FilePolicyService.java](../../../../backend/src/main/java/edu/aiplatform/files/application/FilePolicyService.java)
- [backend/src/main/java/edu/aiplatform/files/application/UploadService.java](../../../../backend/src/main/java/edu/aiplatform/files/application/UploadService.java)
- [backend/src/main/java/edu/aiplatform/files/web/DownloadController.java](../../../../backend/src/main/java/edu/aiplatform/files/web/DownloadController.java)
- [backend/src/main/java/edu/aiplatform/files/web/FileController.java](../../../../backend/src/main/java/edu/aiplatform/files/web/FileController.java)
- [backend/src/main/java/edu/aiplatform/settings/application/SettingsService.java](../../../../backend/src/main/java/edu/aiplatform/settings/application/SettingsService.java)
- [backend/src/main/java/edu/aiplatform/settings/web/SettingsController.java](../../../../backend/src/main/java/edu/aiplatform/settings/web/SettingsController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
