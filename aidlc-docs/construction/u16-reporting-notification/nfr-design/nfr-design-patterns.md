# U16 Reporting & Notification — nfr-design-patterns

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 12, 58. **Primary stories:** US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. **Requirements:** FR-011/019/023/024/038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u16-reporting-notification` không quyết định chức năng hiện hành.

## Patterns áp dụng

- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

- Optimistic concurrency cho UI edits, transaction locks/unique constraints cho contested claims/quota/receipt và outbox. Request fingerprint/version để replay không ghi sai.
- Failure retries chỉ khi adapter chứng minh safe; provider dispatch unknown không gọi lại ngầm; DB/outbox recovery dùng lease.
- Snapshot model/value objects immutable; secrets không trả từ repository qua generic serializer. Data retention tác động qua policy/job riêng không phá lịch sử official.

## Unit-specific patterns

- UC58 Admin summaries + BackupStatusPort U03; metadata only latest-good/history/time/size/safeError, no raw archive/keys/student content. Port down unavailable/lastKnown timestamp, not SUCCESS.
- Notifications from outbox after commit, dedup eventId/recipientId/channel/type; delivery statuses PENDING/SENT/FAILED/SKIPPED/DEFERRED with bounded retries, no rollback domain.
- Emails enrolled/assignment opened/grade published/deadline reminder, important announcement only new event selected by assigned teacher; learning preferences respected. Backup failure Admin policy separate; OTP remains U01 shared MailPort.
- Check current recipient enrollment/role at handling; links require login/current permission, no grades/content/backups in email. SENT means SMTP accepted not inbox.
- One reminder 24h before deadline for unsubmitted, cancelled/retired/sent cases excluded; retry past deadline SKIPPED. Email throttling/provider quota reserves room for OTP, values configurable not invented provider guarantee.
- Admin notification popup can show backup alerts now; preferences learning types Student, operational type for Admin handled separately. Important edits no resend.
- Gradebook export Teacher R3/R4 via U15, official Assignment-only, no Quiz/Practice, no overall weighted total; anonymous distribution opt-in manager via U04, privacy sample threshold baseline.

## Validation matrix

- Important create vs edit; Admin backup receives; no raw grade/archive/key; current enrollment filters recipients.
- Duplicate outbox/provider timeout/backoff/quota no endless retry; SENT only accepted; backup status unavailable distinct.
- Export official-only/authorized teacher; reminder once and not after deadline; Admin count-only + backup metadata.
