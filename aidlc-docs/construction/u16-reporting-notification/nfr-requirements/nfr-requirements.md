# U16 Reporting & Notification — nfr-requirements

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 12, 58. **Primary stories:** US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. **Requirements:** FR-011/019/023/024/038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u16-reporting-notification` không quyết định chức năng hiện hành.

## Required controls

- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

- Validation MIME/XML/schema/IDs/filter allowlists phù hợp unit, bounded payloads và rate limit phía server. Không log secret, raw prompt hoặc dữ liệu học vụ nhạy cảm.
- Truy vấn phân trang/index theo owner/resource/time, timeout hữu hạn mọi port/provider; không promise external API p95 như query local.
- Target baseline local APIs p95 500ms tại 100 concurrent/1000 accounts là mục tiêu cần benchmark, chưa có evidence; heavy files/AI/backup chạy worker và status queries.
- Session/password/header controls giữ SEC-001–006; quota/backup/credential protected riêng, không yêu cầu mã hóa toàn DB.

## Unit-specific NFR risks

- UC58 Admin summaries + BackupStatusPort U03; metadata only latest-good/history/time/size/safeError, no raw archive/keys/student content. Port down unavailable/lastKnown timestamp, not SUCCESS.
- Notifications from outbox after commit, dedup eventId/recipientId/channel/type; delivery statuses PENDING/SENT/FAILED/SKIPPED/DEFERRED with bounded retries, no rollback domain.
- Emails enrolled/assignment opened/grade published/deadline reminder, important announcement only new event selected by assigned teacher; learning preferences respected. Backup failure Admin policy separate; OTP remains U01 shared MailPort.
- Check current recipient enrollment/role at handling; links require login/current permission, no grades/content/backups in email. SENT means SMTP accepted not inbox.
- One reminder 24h before deadline for unsubmitted, cancelled/retired/sent cases excluded; retry past deadline SKIPPED. Email throttling/provider quota reserves room for OTP, values configurable not invented provider guarantee.
- Admin notification popup can show backup alerts now; preferences learning types Student, operational type for Admin handled separately. Important edits no resend.
- Gradebook export Teacher R3/R4 via U15, official Assignment-only, no Quiz/Practice, no overall weighted total; anonymous distribution opt-in manager via U04, privacy sample threshold baseline.

## Test evidence cần có

- Important create vs edit; Admin backup receives; no raw grade/archive/key; current enrollment filters recipients.
- Duplicate outbox/provider timeout/backoff/quota no endless retry; SENT only accepted; backup status unavailable distinct.
- Export official-only/authorized teacher; reminder once and not after deadline; Admin count-only + backup metadata.

Security enabled subset 03/04/05/08/09/12/15: constraints auth/input/audit/secret/failure được thể hiện, chưa runtime verified. Resiliency enabled subset 04/06/10: deployment/health/timeouts giữ; rule khác N/A theo state, PBT disabled.
