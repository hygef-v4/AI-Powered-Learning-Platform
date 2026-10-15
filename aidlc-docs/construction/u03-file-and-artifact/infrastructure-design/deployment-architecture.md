# U03 File, Job, Event & Settings — deployment-architecture

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013/018/033/038; REL-001/002/003/004.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u03-file-and-artifact` không quyết định chức năng hiện hành.

## Mapping triển khai

Logical unit chạy trong backend/worker chung; data PostgreSQL theo owner, cache/rate Redis không nguồn durable business/quota. Package legacy mục tiêu review `files`; không tạo deployable riêng.

Nguồn hạ tầng: [shared-infrastructure](../../shared-infrastructure.md).

- Admin Settings → validate version → U03 setting transaction hoặc U07 quota command → audit → result đúng owner.
- Scheduler khóa một run cho lịch → dump nhất quán và chốt danh sách file version → copy immutable files/verify → encrypt archive → upload private backup folder/verify → SUCCESS → retention.
- Backup failure → FAILED giữ last-good → outbox backup.failed → U16. SMTP failure không đổi BackupRun.
- Restore isolated → decrypt bằng operator key → schema/data/files remap → verify domain invariants → restore evidence/audit; production cutover cần kế hoạch riêng.

## Deploy/rollback/secret

- Docker image theo commit SHA, không latest; chỉ reverse proxy public; migration additive có data mapping/backup trước thay đổi và rollback compatibility.
- Credentials protected field với encryption key ngoài DB; không plaintext Settings/environment dump/logs. Backup archive encryption key và recovery secrets lưu riêng.
- Ready/health của dependencies rõ; ports down không trả empty/0/SUCCESS giả. AI unavailable không chặn manual work; event retry/lease tối đa hữu hạn.
- Trước demo chạy isolated restore, negative authorization, migration validation và checklist unit; runtime chưa được chứng minh bởi revision docs.

## Unit integration risks

- Settings invalid/version conflict/quota port down; no personal secret response.
- Backup happy path, partial file/upload failure, key unavailable, disk full, retention giữ last-good, restore isolated XML/grades/usage/permissions.
- Job duplicate/crash-after-commit/retry bounded; download expired/out-of-scope; file deletion không xóa backup.
