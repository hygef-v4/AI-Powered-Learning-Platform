# U14 Group Workspace & Submission — business-rules

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 27. **Primary stories:** US-GRP-004, US-GRP-005. **Requirements:** FR-018/025/026/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u14-group-document-submission` không quyết định chức năng hiện hành.

## Quy tắc nghiệp vụ

- **BR-U14-01**: Group Assignment is ASSIGNMENT submissionMode GROUP, class-only, GRADED default D02. Draw.io optional identical document model U09.
- **BR-U14-02**: Preserve aggregate one GroupDocument per group/assessment version; sections/revisions/submitted snapshot JSON in group_documents, no unsolicited new sections tables.
- **BR-U14-03**: Section OPEN/CLAIMED/DONE; one editor/section; leader assigns or member claims; finish writes immutable revision and publishedBlocks. Scaffold readonly; diagram disabled enforce U09.
- **BR-U14-04**: GroupDocument IN_PROGRESS/CLOSED; leader MANUAL submit/resubmit before deadline snapshots immutable receipt/version; AUTO_DEADLINE/AUTO_RETIRED locks closest persisted drafts and records SYSTEM. Submission history retained; editing current document never mutates submitted snapshots.
- **BR-U14-05**: Removing/replacing member/leader changes permissions next request, release claims safely per baseline warning/discard policy; audit authorship retained.
- **BR-U14-06**: Student AI context default own editable section D03, no whole-group export automatically. Graded feedback teacher allowed; personal connection only; result tied section/document version.
- **BR-U14-07**: Record per-member visibility through U11 ActivityWritePort under group snapshot policy. U14 does not own duplicate ActivityEvent store; teacher timeline through U15.
- **BR-U14-08**: SSE per-account/document channels recheck membership, no private draft of others; clock/version from backend, no owner bypass through collaboration.
- **BR-U14-09**: Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- **BR-U14-10**: expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- **BR-U14-11**: Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- **BR-U14-12**: No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Negative scenarios / acceptance

- Concurrent claims one wins; private draft not leaked; changing leader authority immediate; duplicate manual/auto close no duplicate receipt.
- Draw.io enabled/disabled, pure doc group works; AI only own section and teacher policy; per-member activity no group guilt score.
