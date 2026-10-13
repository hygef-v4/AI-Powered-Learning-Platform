# U16 Reporting & Notification — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 12, 58. **Primary stories:** US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. **Requirements:** FR-011/019/023/024/038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u16-reporting-notification` không quyết định chức năng hiện hành.

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

notifications/email/reminders; aggregates/gradebook export via ports; dashboardBackupStatus (UC 58) via U03 BackupStatusPort; idempotent importantAnnouncement/backupFailure email

## Owned data

- Notification: id/sourceEventId/recipientId/type/link/minimalContent/readAt/createdAt; unique event/recipient/type.
- Delivery: eventId/recipientId/channel unique, state/retry/nextAttempt/providerAcceptedAt/safeError; per-channel preference and dedup.
- DashboardSummary DTO + BackupStatusProjection from U03, no duplicate backup table. ExportJob refs/class/filter validated before file creation.

## Critical paths

- After-commit domain event → recipient resolver checks current access → in-app persistent + eligible email delivery → bounded retry → terminal.
- Backup failed → Admin notification/email minimal metadata; error not rollback BackupRun.
- UC58 summaries → scope/count ports U01/U04 plus U03 backup read → safe DTO; UC40 teacher export → U15 projection → U03 private file.
