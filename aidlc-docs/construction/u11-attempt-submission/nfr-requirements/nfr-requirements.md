# U11 Individual Work & Quiz Submission — nfr-requirements

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 18, 19, 20, 21, 22, 23, 24, 28, 29. **Primary stories:** US-ASM-003, US-ASM-012. **Requirements:** FR-018/019/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u11-attempt-submission` không quyết định chức năng hiện hành.

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

- Individual Assignment and Quiz Attempt owner; Group work U14, nhưng activity cả hai U11. Enrollment/current state/version always server checked.
- Attempt IN_PROGRESS → SUBMITTED, snapshot immutable after submit; server determines time limits/deadline/late and MANUAL/AUTO_TIME_LIMIT/AUTO_DEADLINE/AUTO_RETIRED; receipt hash stable across replay. Autosave uses expectedContentVersion.
- Assignment content Document with optional diagram, Quiz answers map optionIds; no runResult/files/language. Quiz result deterministic through U15 PracticeResultPort; not official Gradebook.
- UC29 feedback is allowed only for the owner's SUBMITTED PRACTICE Assignment snapshot via personal API/MCP. Reject draft/IN_PROGRESS, Graded and unauthorized feedback requests or writes server-side. Feedback is versioned/rate limited and never changes submitted content or official grades. UC79 is retired; no draft-feedback opt-in remains.
- ActivityEvent UNIQUE(actorId/workRef/clientEventId); authenticated own attempt or active group member, policy enabled in snapshot, state IN_PROGRESS. U14 delegates member events via ActivityWritePort; fields kind HIDDEN/VISIBLE, clientTime/sequence and serverReceivedAt.
- Visibility intervals pair by work session, dedup blur/hidden, clamp invalid timestamps and flag missing/out-of-order/clock unreliable; lost connection yields UNKNOWN interval, never assumed continuous away or zero cheating. Draw.io iframe focus alone not page hidden.
- ActivityQueryPort for U15 rechecks teacher/class and per-member; own Student projection allowed, Admin denied. Retention setting with no numeric default must be resolved/configured before capture.
- Telemetry never auto-submit/penalize; actual deadline auto-submit remains separate. Personal agent draft writes expectedVersion; no MCP official submit initially.

## Test evidence cần có

- Concurrent autosave stale conflict; double submit stable receipt; deadline controls server; enrollment revoke denies save/feedback/activity.
- Monitoring off no events, dedup/out-of-order/offline gaps/iframe legitimate; wrong member/work reject; no grade side effects.
- UC29 SUBMITTED/PRACTICE guards; deny draft/IN_PROGRESS and Graded feedback even through API/MCP, deny Student shared quota and reject stale feedback without changing submitted content or grades.

Security enabled subset 03/04/05/08/09/12/15: constraints auth/input/audit/secret/failure được thể hiện, chưa runtime verified. Resiliency enabled subset 04/06/10: deployment/health/timeouts giữ; rule khác N/A theo state, PBT disabled.
