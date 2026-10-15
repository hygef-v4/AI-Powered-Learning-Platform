# U14 Group Workspace & Submission — Code Generation Plan — revision chưa triển khai

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 56. **Primary stories:** US-GRP-004, US-GRP-005. **Requirements:** FR-018/025/026/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u14-group-document-submission` không quyết định chức năng hiện hành.

## Authorization và trạng thái

User chỉ yêu cầu đồng bộ tài liệu Construction theo Inception. Đây là kế hoạch revision, không chạy Code Generation Part 2 hoặc đánh completed. Checklist/approval/code baseline giữ tại [plan cũ](../../historical/baseline-before-66-uc-2026-10-13/plans/code-generation-plan/u14-group-document-submission-code-generation-plan.md); approval 05/10 không tự áp dụng scope mới. Không reset lịch sử code đã sinh thành chưa sinh; checklist dưới đây chỉ delta hiện hành.

## Paths cần review trước sửa code

- Existing backend owner/package: `backend/src/main/java/edu/aiplatform (xác định package existing trước refactor)`; frontend `frontend/src`, public contracts `backend/src/main/java/edu/aiplatform/contracts` nếu tồn tại; migration `backend/src/main/resources/db/migration`; tests `backend/src/test` và frontend test files.
- Exact classes/routes/migration numbers xác định qua repository inventory trước implementation; không invent files đã tồn tại. Legacy package/folder tên cũ không cho phép tiếp tục payment/code.
- Source-of-truth [current shared contract](../../current-srs-contract.md), unit design theo current folder. [Inception dependency](../../../inception/application-design/unit-of-work-dependency.md) là thứ tự hiện hành.

## Checklist delta

- [ ] **Bước 1:** Inventory code/contracts/migrations baseline, xác định reuse/removal, mapping dữ liệu và exact paths; không drop records/snapshots trước backup/restore.
- [ ] **Bước 2:** Review DTO/status/public ports theo U14 owner; authorizations hiện hành, expectedVersion/idempotency và error projection; replace adapters fail-closed trước tích hợp thật.
- [ ] **Bước 3:** Migration owned entities/constraints/index/retention; preserve IDs/historical snapshots, validate old-to-new mapping trên copy database.
- [ ] **Bước 4:** Domain rules/services/transactions và recovery theo functional-design; dependent services gọi port, không repositories xuyên owner.
- [ ] **Bước 5:** API layer guards/validation/rate/CSRF, minimal projections, sanitized audit/outbox cùng owner transaction; external timeout/retry bounded.
- [ ] **Bước 6:** Frontend state/forms/query permissions theo entry UC hiện hành; conflicts/unavailable rõ; không client-only policy/security.
- [ ] **Bước 7:** Coder viết meaningful unit/service/API/UI tests của delta, negative permissions/state/duplicate/concurrency; không chỉ mirror implementation.
- [ ] **Bước 8:** Tester khác viết integration cases từ requirements/design và chạy adapter thật/sandbox/local mock phù hợp, phân biệt cases viết xong và runtime pass.
- [ ] **Bước 9:** Update deployment env/health/secret rotation và runbook; test migration/rollback/restore nếu unit tác động dữ liệu hoặc credentials.
- [ ] **Bước 10:** Review evidence/FR-story-UC traceability, fix/retest; user review trước next code stage, không tự thay milestones/nhân sự.

## Unit-specific implementation scope

- Group Assignment is ASSIGNMENT submissionMode GROUP, class-only, GRADED default D02. Draw.io optional identical document model U09.
- Preserve aggregate one GroupDocument per group/assessment version; sections/revisions/submitted snapshot JSON in group_documents, no unsolicited new sections tables.
- Section OPEN/CLAIMED/DONE; one editor/section; leader assigns or member claims; finish writes immutable revision and publishedBlocks. Scaffold readonly; diagram disabled enforce U09.
- GroupDocument IN_PROGRESS/CLOSED; leader MANUAL submit/resubmit before deadline snapshots immutable receipt/version; AUTO_DEADLINE/AUTO_RETIRED locks closest persisted drafts and records SYSTEM. Submission history retained; editing current document never mutates submitted snapshots.
- Removing/replacing member/leader changes permissions next request, release claims safely per baseline warning/discard policy; audit authorship retained.
- Student AI context default own editable section D03, no whole-group export automatically. Graded feedback teacher allowed; personal connection only; result tied section/document version.
- Record per-member visibility through U11 ActivityWritePort under group snapshot policy. U14 does not own duplicate ActivityEvent store; teacher timeline through U15.
- SSE per-account/document channels recheck membership, no private draft of others; clock/version from backend, no owner bypass through collaboration.

## Independent integration acceptance

- Concurrent claims one wins; private draft not leaked; changing leader authority immediate; duplicate manual/auto close no duplicate receipt.
- Draw.io enabled/disabled, pure doc group works; AI only own section and teacher policy; per-member activity no group guilt score.

## DoD

Requirements owner, test-case owner, coder khác nhau theo nhóm; implementation readiness/compatibility và unresolved decisions xử lý trước enable feature tương ứng. No runtime pass/checklist tick chỉ vì docs viết xong; codes under workspace root, không aidlc-docs.
