# U14 Group Workspace & Submission — frontend-components

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 27. **Primary stories:** US-GRP-004, US-GRP-005. **Requirements:** FR-018/025/026/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u14-group-document-submission` không quyết định chức năng hiện hành.

## UI entry và API đề xuất

- /api/v1/group-docs/{id}; sections/{id}/claim|draft|finish|release; :submit; /events SSE; group activity delegates U11.

## Component/state contract

- List/Detail/Editor phân theo quyền và entry UC hiện hành; query state loading/ready/empty/unavailable/error tách biệt.
- Form state draft/dirty/saving/saved/conflict/invalid; save gửi expectedVersion và requestRef khi có idempotent command; lỗi không xóa draft.
- State-changing requests không chỉ dựa ẩn nút; server authorize, version/policy errors cần refresh lại context trước retry.
- UI không phơi bày raw keys, hidden Quiz answers, unpublished grades hoặc dữ liệu ngoài scope.

## Hành vi riêng unit

- Group Assignment is ASSIGNMENT submissionMode GROUP, class-only, GRADED default D02. Draw.io optional identical document model U09.
- Preserve aggregate one GroupDocument per group/assessment version; sections/revisions/submitted snapshot JSON in group_documents, no unsolicited new sections tables.
- Section OPEN/CLAIMED/DONE; one editor/section; leader assigns or member claims; finish writes immutable revision and publishedBlocks. Scaffold readonly; diagram disabled enforce U09.
- GroupDocument IN_PROGRESS/CLOSED; leader MANUAL submit/resubmit before deadline snapshots immutable receipt/version; AUTO_DEADLINE/AUTO_RETIRED locks closest persisted drafts and records SYSTEM. Submission history retained; editing current document never mutates submitted snapshots.
- Removing/replacing member/leader changes permissions next request, release claims safely per baseline warning/discard policy; audit authorship retained.
- Student AI context default own editable section D03, no whole-group export automatically. Graded feedback teacher allowed; personal connection only; result tied section/document version.
- Record per-member visibility through U11 ActivityWritePort under group snapshot policy. U14 does not own duplicate ActivityEvent store; teacher timeline through U15.
- SSE per-account/document channels recheck membership, no private draft of others; clock/version from backend, no owner bypass through collaboration.

## UI acceptance

- Concurrent claims one wins; private draft not leaked; changing leader authority immediate; duplicate manual/auto close no duplicate receipt.
- Draw.io enabled/disabled, pure doc group works; AI only own section and teacher policy; per-member activity no group guilt score.
