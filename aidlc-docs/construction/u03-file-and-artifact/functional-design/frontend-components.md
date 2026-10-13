# U03 File, Job, Event & Settings — frontend-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 70. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013/018/033/038; REL-001/002/003/004.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u03-file-and-artifact` không quyết định chức năng hiện hành.

## UI entry và API đề xuất

- GET /api/v1/admin/settings; PATCH /api/v1/admin/settings/{key} expectedVersion; quota group delegates U07.
- BackupStatusPort.list/latest dùng bởi U16; không REST endpoint tải raw archive cho Admin Dashboard. File endpoints giữ owner-scoped /api/v1/files.

## Component/state contract

- List/Detail/Editor phân theo quyền và entry UC hiện hành; query state loading/ready/empty/unavailable/error tách biệt.
- Form state draft/dirty/saving/saved/conflict/invalid; save gửi expectedVersion và requestRef khi có idempotent command; lỗi không xóa draft.
- State-changing requests không chỉ dựa ẩn nút; server authorize, version/policy errors cần refresh lại context trước retry.
- UI không phơi bày raw keys, hidden Quiz answers, unpublished grades hoặc dữ liệu ngoài scope.

## Hành vi riêng unit

- UC 70 primary U03: Admin xem/sửa non-secret Settings; quota qua U07 TeacherQuotaPolicyPort có version/audit riêng. Không transaction nguyên tử Settings+quota; unavailable không báo saved.
- File private, MIME/size/purpose/scope kiểm server trước upload/download; short-lived token hoặc server stream không cấp thêm quyền. File provider ID và secrets không đưa vào log.
- Backup schedule/retention là cấu hình bắt buộc xác nhận, không tự gán số. Consistent PostgreSQL dump + version/schema + manifest và bản sao độc lập các tệp tại snapshot; bản live Drive không là backup.
- BackupRun CREATED → RUNNING → SUCCESS/FAILED; SUCCESS chỉ sau toàn bộ dump/files/encrypted upload/checksum. Retention chỉ xóa bản thành công cũ sau khi giữ tối thiểu bản tốt gần nhất; bản lỗi không thay bản tốt.
- Archive mã hóa bằng khóa giữ ngoài VPS/archive; protected AI credentials trong database vẫn là ciphertext, secret store/key recovery riêng. Lỗi upload/disk/permission retry hữu hạn, phát backup.failed sau ghi trạng thái.
- Restore runbook vào môi trường cô lập; không web production-restore hoặc MCP restore tool. Kiểm records/scope/links/XML/submissions/grades/quota; đo RPO/RTO, không cam kết zero loss.
- Jobs enqueue-after-commit qua outbox; claim/lease/dedup theo requestRef, retry bounded, terminal lỗi ghi rõ. DB job/outbox là nguồn bền vững; Redis mất không được reset quota bền vững.

## UI acceptance

- Settings invalid/version conflict/quota port down; no personal secret response.
- Backup happy path, partial file/upload failure, key unavailable, disk full, retention giữ last-good, restore isolated XML/grades/usage/permissions.
- Job duplicate/crash-after-commit/retry bounded; download expired/out-of-scope; file deletion không xóa backup.
