# U16 Reporting & Notification — tech-stack-decisions

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 65, 66. **Primary stories:** US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001. **Requirements:** FR-011/019/023/024/038.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u16-reporting-notification` không quyết định chức năng hiện hành.

## Stack giữ theo dự án

Java 17/Spring Boot 3.5 modular monolith; Next.js 16/TypeScript, PostgreSQL/Redis/RabbitMQ, Nginx/Docker Compose, U03 Drive private và U01/U16 MailPort. Dependency lock/pin và actual APIs xác minh khi code; không chọn version SDK mới trong revision tài liệu.

- UC66 Admin summaries + BackupStatusPort U03; metadata only latest-good/history/time/size/safeError, no raw archive/keys/student content. Port down unavailable/lastKnown timestamp, not SUCCESS.
- Notifications from outbox after commit, dedup eventId/recipientId/channel/type; delivery statuses PENDING/SENT/FAILED/SKIPPED/DEFERRED with bounded retries, no rollback domain.
- Emails enrolled/assignment opened/grade published/deadline reminder, important announcement only new event selected by assigned teacher; learning preferences respected. Backup failure Admin policy separate; OTP remains U01 shared MailPort.
- Check current recipient enrollment/role at handling; links require login/current permission, no grades/content/backups in email. SENT means SMTP accepted not inbox.
- One reminder 24h before deadline for unsubmitted, cancelled/retired/sent cases excluded; retry past deadline SKIPPED. Email throttling/provider quota reserves room for OTP, values configurable not invented provider guarantee.
- Admin notification popup can show backup alerts now; preferences learning types Student, operational type for Admin handled separately. Important edits no resend.
- Gradebook export Teacher R3/R4 via U15, official Assignment-only, no Quiz/Practice, no overall weighted total; anonymous distribution opt-in manager via U04, privacy sample threshold baseline.

## Cost và extension

AI API tổ chức/cá nhân do bên tương ứng trả, không giả định free/đã có key; infrastructure dùng resources nhóm có. Full Lesson/RAG không prerequisite; mọi embedding/provider config là extension riêng. Numeric policies D04/retention chưa giả định. Không thêm unit/container cho từng module.
