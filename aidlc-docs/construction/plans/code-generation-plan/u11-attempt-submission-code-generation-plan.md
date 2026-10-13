# U11 Individual Work & Quiz Submission — Code Generation Plan — revision chưa triển khai

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 18, 19, 20, 21, 22, 23, 24, 28, 29. **Primary stories:** US-ASM-003, US-ASM-012. **Requirements:** FR-018/019/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u11-attempt-submission` không quyết định chức năng hiện hành.

## Authorization và trạng thái

User chỉ yêu cầu đồng bộ tài liệu Construction theo Inception. Đây là kế hoạch revision, không chạy Code Generation Part 2 hoặc đánh completed. Checklist/approval/code baseline giữ tại [plan cũ](../../historical/baseline-before-66-uc-2026-10-13/plans/code-generation-plan/u11-attempt-submission-code-generation-plan.md); approval 05/10 không tự áp dụng scope mới. Không reset lịch sử code đã sinh thành chưa sinh; checklist dưới đây chỉ delta hiện hành.

## Paths cần review trước sửa code

- Existing backend owner/package: `backend/src/main/java/edu/aiplatform/attempts`; frontend `frontend/src`, public contracts `backend/src/main/java/edu/aiplatform/contracts` nếu tồn tại; migration `backend/src/main/resources/db/migration`; tests `backend/src/test` và frontend test files.
- Exact classes/routes/migration numbers xác định qua repository inventory trước implementation; không invent files đã tồn tại. Legacy package/folder tên cũ không cho phép tiếp tục payment/code.
- Source-of-truth [current shared contract](../../current-srs-contract.md), unit design theo current folder. [Inception dependency](../../../inception/application-design/unit-of-work-dependency.md) là thứ tự hiện hành.

## Checklist delta

- [ ] **Bước 1:** Inventory code/contracts/migrations baseline, xác định reuse/removal, mapping dữ liệu và exact paths; không drop records/snapshots trước backup/restore.
- [ ] **Bước 2:** Review DTO/status/public ports theo U11 owner; authorizations hiện hành, expectedVersion/idempotency và error projection; replace adapters fail-closed trước tích hợp thật.
- [ ] **Bước 3:** Migration owned entities/constraints/index/retention; preserve IDs/historical snapshots, validate old-to-new mapping trên copy database.
- [ ] **Bước 4:** Domain rules/services/transactions và recovery theo functional-design; dependent services gọi port, không repositories xuyên owner.
- [ ] **Bước 5:** API layer guards/validation/rate/CSRF, minimal projections, sanitized audit/outbox cùng owner transaction; external timeout/retry bounded.
- [ ] **Bước 6:** Frontend state/forms/query permissions theo entry UC hiện hành; conflicts/unavailable rõ; không client-only policy/security.
- [ ] **Bước 7:** Coder viết meaningful unit/service/API/UI tests của delta, negative permissions/state/duplicate/concurrency; không chỉ mirror implementation.
- [ ] **Bước 8:** Tester khác viết integration cases từ requirements/design và chạy adapter thật/sandbox/local mock phù hợp, phân biệt cases viết xong và runtime pass.
- [ ] **Bước 9:** Update deployment env/health/secret rotation và runbook; test migration/rollback/restore nếu unit tác động dữ liệu hoặc credentials.
- [ ] **Bước 10:** Review evidence/FR-story-UC traceability, fix/retest; user review trước next code stage, không tự thay milestones/nhân sự.

## Unit-specific implementation scope

- Individual Assignment and Quiz Attempt owner; Group work U14, nhưng activity cả hai U11. Enrollment/current state/version always server checked.
- Attempt IN_PROGRESS → SUBMITTED, snapshot immutable after submit; server determines time limits/deadline/late and MANUAL/AUTO_TIME_LIMIT/AUTO_DEADLINE/AUTO_RETIRED; receipt hash stable across replay. Autosave uses expectedContentVersion.
- Assignment content Document with optional diagram, Quiz answers map optionIds; no runResult/files/language. Quiz result deterministic through U15 PracticeResultPort; not official Gradebook.
- UC29 feedback is allowed only for the owner's SUBMITTED PRACTICE Assignment snapshot via personal API/MCP. Reject draft/IN_PROGRESS, Graded and unauthorized feedback requests or writes server-side. Feedback is versioned/rate limited and never changes submitted content or official grades. UC79 is retired; no draft-feedback opt-in remains.
- ActivityEvent UNIQUE(actorId/workRef/clientEventId); authenticated own attempt or active group member, policy enabled in snapshot, state IN_PROGRESS. U14 delegates member events via ActivityWritePort; fields kind HIDDEN/VISIBLE, clientTime/sequence and serverReceivedAt.
- Visibility intervals pair by work session, dedup blur/hidden, clamp invalid timestamps and flag missing/out-of-order/clock unreliable; lost connection yields UNKNOWN interval, never assumed continuous away or zero cheating. Draw.io iframe focus alone not page hidden.
- ActivityQueryPort for U15 rechecks teacher/class and per-member; own Student projection allowed, Admin denied. Retention setting with no numeric default must be resolved/configured before capture.
- Telemetry never auto-submit/penalize; actual deadline auto-submit remains separate. Personal agent draft writes expectedVersion; no MCP official submit initially.

## Independent integration acceptance

- Concurrent autosave stale conflict; double submit stable receipt; deadline controls server; enrollment revoke denies save/feedback/activity.
- Monitoring off no events, dedup/out-of-order/offline gaps/iframe legitimate; wrong member/work reject; no grade side effects.
- UC29 SUBMITTED/PRACTICE guards; deny draft/IN_PROGRESS and Graded feedback even through API/MCP, deny Student shared quota and reject stale feedback without changing submitted content or grades.

## DoD

Requirements owner, test-case owner, coder khác nhau theo nhóm; implementation readiness/compatibility và unresolved decisions xử lý trước enable feature tương ứng. No runtime pass/checklist tick chỉ vì docs viết xong; codes under workspace root, không aidlc-docs.
