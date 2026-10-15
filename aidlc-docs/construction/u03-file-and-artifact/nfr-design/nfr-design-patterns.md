# U03 File, Job, Event & Settings — nfr-design-patterns

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 14, 15. **Primary stories:** US-SET-001, US-OPS-001. **Requirements:** FR-013/018/033/038; REL-001/002/003/004.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u03-file-and-artifact` không quyết định chức năng hiện hành.

## Patterns áp dụng

- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

- Optimistic concurrency cho UI edits, transaction locks/unique constraints cho contested claims/quota/receipt và outbox. Request fingerprint/version để replay không ghi sai.
- Failure retries chỉ khi adapter chứng minh safe; provider dispatch unknown không gọi lại ngầm; DB/outbox recovery dùng lease.
- Snapshot model/value objects immutable; secrets không trả từ repository qua generic serializer. Data retention tác động qua policy/job riêng không phá lịch sử official.

## Unit-specific patterns

- UC14 View Settings / UC15 Edit Setting primary U03. View returns masked institutional API configuration and quota/operational policies; edits delegate quota to U07 TeacherQuotaPolicyPort and protected credentials/provider/model/test/revoke to U13 AiConnectionAdministrationPort. Each owner validates version and audit; no cross-owner atomic transaction or raw key in ordinary Settings, and unavailable never reports saved. UC76/78/82 (ID cũ) retired.
- File private, MIME/size/purpose/scope kiểm server trước upload/download; short-lived token hoặc server stream không cấp thêm quyền. File provider ID và secrets không đưa vào log.
- Backup schedule/retention là cấu hình bắt buộc xác nhận, không tự gán số. Consistent PostgreSQL dump + version/schema + manifest và bản sao độc lập các tệp tại snapshot; bản live Drive không là backup.
- BackupRun CREATED → RUNNING → SUCCESS/FAILED; SUCCESS chỉ sau toàn bộ dump/files/encrypted upload/checksum. Retention chỉ xóa bản thành công cũ sau khi giữ tối thiểu bản tốt gần nhất; bản lỗi không thay bản tốt.
- Archive mã hóa bằng khóa giữ ngoài VPS/archive; protected AI credentials trong database vẫn là ciphertext, secret store/key recovery riêng. Lỗi upload/disk/permission retry hữu hạn, phát backup.failed sau ghi trạng thái.
- Restore runbook vào môi trường cô lập; không web production-restore hoặc MCP restore tool. Kiểm records/scope/links/XML/submissions/grades/quota; đo RPO/RTO, không cam kết zero loss.
- Jobs enqueue-after-commit qua outbox; claim/lease/dedup theo requestRef, retry bounded, terminal lỗi ghi rõ. DB job/outbox là nguồn bền vững; Redis mất không được reset quota bền vững.

## Validation matrix

- Settings invalid/version conflict/quota port down; no personal secret response.
- Backup happy path, partial file/upload failure, key unavailable, disk full, retention giữ last-good, restore isolated XML/grades/usage/permissions.
- Job duplicate/crash-after-commit/retry bounded; download expired/out-of-scope; file deletion không xóa backup.
