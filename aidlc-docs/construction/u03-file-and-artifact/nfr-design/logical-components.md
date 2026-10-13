# U03 File, Job, Event & Settings — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 70. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013/018/033/038; REL-001/002/003/004.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u03-file-and-artifact` không quyết định chức năng hiện hành.

## Component responsibility

| Layer | Responsibility |
|---|---|
| Controller / MCP adapter | Deserialize/validate, authenticated actor, minimal response; gọi domain service |
| Application service | Scope/state/version, orchestration qua ports, transaction owner |
| Domain policy/model | Rules, transitions, snapshot invariants |
| Repository | Owned entities only; optimistic/locking/unique constraints |
| Outbound adapters | Timeout/retry/provenance; không quyết định học vụ |
| UI | Entry UC, form/query status, không client-only security |

## Owner contract

file store/download scoped; jobs enqueue-after-commit/retry; typed Settings version/audit; manageSystemSettings (UC 70) delegates quota reads/writes via U07 port; runScheduledBackup/listBackupStatus; publishBackupFailure

## Owned data

- FileArtifact: id, purpose, ownerId, subject/class/resource refs, providerFileId(private), MIME, size, hash, version, status.
- Setting: key/type/nonSecretValue/version; BackupPolicy: schedule/timezone/retention/backupFolderRef/version, encrypted-key reference ngoài app.
- BackupRun: id, snapshotTime, schema/appVersion, state, encryptedArchiveRef, manifestRef/hash, bytes, startedAt/completedAt, safeError, lastGoodRef.
- BackupFileManifest: logicalFileId, immutableSourceVersion/hash, copiedBackupRef/hash, restoreMapping; copy tất cả nguồn trước SUCCESS.
- Job/Outbox: id/type/payloadRefs/requestRef unique, state, leaseUntil, retries, nextRetry, outcome; không raw secrets.

## Critical paths

- Admin Settings → validate version → U03 setting transaction hoặc U07 quota command → audit → result đúng owner.
- Scheduler khóa một run cho lịch → dump nhất quán và chốt danh sách file version → copy immutable files/verify → encrypt archive → upload private backup folder/verify → SUCCESS → retention.
- Backup failure → FAILED giữ last-good → outbox backup.failed → U16. SMTP failure không đổi BackupRun.
- Restore isolated → decrypt bằng operator key → schema/data/files remap → verify domain invariants → restore evidence/audit; production cutover cần kế hoạch riêng.
