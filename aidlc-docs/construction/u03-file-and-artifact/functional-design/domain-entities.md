# U03 File, Job, Event & Settings — domain-entities

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013/018/033/038; REL-001/002/003/004.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u03-file-and-artifact` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- FileArtifact: id, purpose, ownerId, subject/class/resource refs, providerFileId(private), MIME, size, hash, version, status.
- Setting: key/type/nonSecretValue/version; BackupPolicy: schedule/timezone/retention/backupFolderRef/version, encrypted-key reference ngoài app.
- BackupRun: id, snapshotTime, schema/appVersion, state, encryptedArchiveRef, manifestRef/hash, bytes, startedAt/completedAt, safeError, lastGoodRef.
- BackupFileManifest: logicalFileId, immutableSourceVersion/hash, copiedBackupRef/hash, restoreMapping; copy tất cả nguồn trước SUCCESS.
- Job/Outbox: id/type/payloadRefs/requestRef unique, state, leaseUntil, retries, nextRetry, outcome; không raw secrets.

## Boundary

file store/download scoped; jobs enqueue-after-commit/retry; typed Settings version/audit; viewSettings (UC14) / editSetting (UC15) delegate quota via U07 and institutional AI configuration via U13 public ports; runScheduledBackup/listBackupStatus; publishBackupFailure

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- UC14 View Settings / UC15 Edit Setting primary U03. View returns masked institutional API configuration and quota/operational policies; edits delegate quota to U07 TeacherQuotaPolicyPort and protected credentials/provider/model/test/revoke to U13 AiConnectionAdministrationPort. Each owner validates version and audit; no cross-owner atomic transaction or raw key in ordinary Settings, and unavailable never reports saved. UC76/78/82 (ID cũ) retired.
- File private, MIME/size/purpose/scope kiểm server trước upload/download; short-lived token hoặc server stream không cấp thêm quyền. File provider ID và secrets không đưa vào log.
- Backup schedule/retention là cấu hình bắt buộc xác nhận, không tự gán số. Consistent PostgreSQL dump + version/schema + manifest và bản sao độc lập các tệp tại snapshot; bản live Drive không là backup.
- BackupRun CREATED → RUNNING → SUCCESS/FAILED; SUCCESS chỉ sau toàn bộ dump/files/encrypted upload/checksum. Retention chỉ xóa bản thành công cũ sau khi giữ tối thiểu bản tốt gần nhất; bản lỗi không thay bản tốt.
- Archive mã hóa bằng khóa giữ ngoài VPS/archive; protected AI credentials trong database vẫn là ciphertext, secret store/key recovery riêng. Lỗi upload/disk/permission retry hữu hạn, phát backup.failed sau ghi trạng thái.
- Restore runbook vào môi trường cô lập; không web production-restore hoặc MCP restore tool. Kiểm records/scope/links/XML/submissions/grades/quota; đo RPO/RTO, không cam kết zero loss.
- Jobs enqueue-after-commit qua outbox; claim/lease/dedup theo requestRef, retry bounded, terminal lỗi ghi rõ. DB job/outbox là nguồn bền vững; Redis mất không được reset quota bền vững.
