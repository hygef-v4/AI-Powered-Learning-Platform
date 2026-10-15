# U11 Individual Work & Quiz Submission — business-rules

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 49, 50, 51, 52, 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003, US-ASM-012. **Requirements:** FR-018/019/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u11-attempt-submission` không quyết định chức năng hiện hành.

## Quy tắc nghiệp vụ

- **BR-U11-01**: Individual Assignment and Quiz Attempt owner; Group work U14, nhưng activity cả hai U11. Enrollment/current state/version always server checked.
- **BR-U11-02**: Attempt IN_PROGRESS → SUBMITTED, snapshot immutable after submit; server determines time limits/deadline/late and MANUAL/AUTO_TIME_LIMIT/AUTO_DEADLINE/AUTO_RETIRED; receipt hash stable across replay. Autosave uses expectedContentVersion.
- **BR-U11-03**: Assignment content Document with optional diagram, Quiz answers map optionIds; no runResult/files/language. Quiz result deterministic through U15 PracticeResultPort; not official Gradebook.
- **BR-U11-04**: UC41 feedback is allowed only for the owner's SUBMITTED PRACTICE Assignment snapshot via personal API/MCP. Reject draft/IN_PROGRESS, Graded and unauthorized feedback requests or writes server-side. Feedback is versioned/rate limited and never changes submitted content or official grades. UC79 (ID cũ) is retired; no draft-feedback opt-in remains.
- **BR-U11-05**: ActivityEvent UNIQUE(actorId/workRef/clientEventId); authenticated own attempt or active group member, policy enabled in snapshot, state IN_PROGRESS. U14 delegates member events via ActivityWritePort; fields kind HIDDEN/VISIBLE, clientTime/sequence and serverReceivedAt.
- **BR-U11-06**: Visibility intervals pair by work session, dedup blur/hidden, clamp invalid timestamps and flag missing/out-of-order/clock unreliable; lost connection yields UNKNOWN interval, never assumed continuous away or zero cheating. Draw.io iframe focus alone not page hidden.
- **BR-U11-07**: ActivityQueryPort for U15 rechecks teacher/class and per-member; own Student projection allowed, Admin denied. Retention setting with no numeric default must be resolved/configured before capture.
- **BR-U11-08**: Telemetry never auto-submit/penalize; actual deadline auto-submit remains separate. Personal agent draft writes expectedVersion; no MCP official submit initially.
- **BR-U11-09**: Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- **BR-U11-10**: expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- **BR-U11-11**: Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- **BR-U11-12**: No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Negative scenarios / acceptance

- Concurrent autosave stale conflict; double submit stable receipt; deadline controls server; enrollment revoke denies save/feedback/activity.
- Monitoring off no events, dedup/out-of-order/offline gaps/iframe legitimate; wrong member/work reject; no grade side effects.
- UC41 SUBMITTED/PRACTICE guards; deny draft/IN_PROGRESS and Graded feedback even through API/MCP, deny Student shared quota and reject stale feedback without changing submitted content or grades.
