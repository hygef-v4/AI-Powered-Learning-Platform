# U11 Individual Work & Quiz Submission — frontend-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 18, 19, 20, 21, 22, 23, 24, 26, 28, 29, 79. **Primary stories:** US-ASM-003, US-ASM-012. **Requirements:** FR-018/019/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u11-attempt-submission` không quyết định chức năng hiện hành.

## UI entry và API đề xuất

- /api/v1/attempts/{id}/content, :submit, :feedback; /api/v1/work/{kind}/{id}/activity (POST authenticated own events); ActivityWritePort/ActivityQueryPort for group/review.

## Component/state contract

- List/Detail/Editor phân theo quyền và entry UC hiện hành; query state loading/ready/empty/unavailable/error tách biệt.
- Form state draft/dirty/saving/saved/conflict/invalid; save gửi expectedVersion và requestRef khi có idempotent command; lỗi không xóa draft.
- State-changing requests không chỉ dựa ẩn nút; server authorize, version/policy errors cần refresh lại context trước retry.
- UI không phơi bày raw keys, hidden Quiz answers, unpublished grades hoặc dữ liệu ngoài scope.

## Hành vi riêng unit

- Individual Assignment and Quiz Attempt owner; Group work U14, nhưng activity cả hai U11. Enrollment/current state/version always server checked.
- Attempt IN_PROGRESS → SUBMITTED, snapshot immutable after submit; server determines time limits/deadline/late and MANUAL/AUTO_TIME_LIMIT/AUTO_DEADLINE/AUTO_RETIRED; receipt hash stable across replay. Autosave uses expectedContentVersion.
- Assignment content Document with optional diagram, Quiz answers map optionIds; no runResult/files/language. Quiz result deterministic through U15 PracticeResultPort; not official Gradebook.
- UC29 feedback own submitted Practice vs UC79 editable draft feedback; Graded draft allowStudentAiFeedback, personal API/MCP only for Student. Multiple feedback versioned/rate limited, no overwrite newer draft or grades.
- ActivityEvent UNIQUE(actorId/workRef/clientEventId); authenticated own attempt or active group member, policy enabled in snapshot, state IN_PROGRESS. U14 delegates member events via ActivityWritePort; fields kind HIDDEN/VISIBLE, clientTime/sequence and serverReceivedAt.
- Visibility intervals pair by work session, dedup blur/hidden, clamp invalid timestamps and flag missing/out-of-order/clock unreliable; lost connection yields UNKNOWN interval, never assumed continuous away or zero cheating. Draw.io iframe focus alone not page hidden.
- ActivityQueryPort for U15 rechecks teacher/class and per-member; own Student projection allowed, Admin denied. Retention setting with no numeric default must be resolved/configured before capture.
- Telemetry never auto-submit/penalize; actual deadline auto-submit remains separate. Personal agent draft writes expectedVersion; no MCP official submit initially.

## UI acceptance

- Concurrent autosave stale conflict; double submit stable receipt; deadline controls server; enrollment revoke denies save/feedback/activity.
- Monitoring off no events, dedup/out-of-order/offline gaps/iframe legitimate; wrong member/work reject; no grade side effects.
- UC29 vs79 state distinctions; Student shared denied, Graded feedback disabled deny, stale feedback not overwrite.
