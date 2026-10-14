# U14 Group Workspace & Submission — domain-entities

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 56. **Primary stories:** US-GRP-004, US-GRP-005. **Requirements:** FR-018/025/026/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u14-group-document-submission` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- GroupDocument: id/groupId/assessmentVersionId/classId unique, status IN_PROGRESS/CLOSED, version, sharedBlocks, sections JSON, revisions JSON, submittedSnapshots JSON, policySnapshot.
- Section JSON: id/partId/order/title/status/claimedBy/assignedBy, draftBlocks/private-to-editor, publishedBlocks, contentVersion/lastAuthor.
- SectionRevision JSON: sectionId/partId/authorId/blocks/hash/timestamp immutable; GroupSubmission snapshot: document/version/sectionAuthors/submittedBy/mode/time/receiptHash immutable.

## Boundary

claim/save group section; group context policy; leader submit/version; record own member activity through U11 port

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- Group Assignment is ASSIGNMENT submissionMode GROUP, class-only, GRADED default D02. Draw.io optional identical document model U09.
- Preserve aggregate one GroupDocument per group/assessment version; sections/revisions/submitted snapshot JSON in group_documents, no unsolicited new sections tables.
- Section OPEN/CLAIMED/DONE; one editor/section; leader assigns or member claims; finish writes immutable revision and publishedBlocks. Scaffold readonly; diagram disabled enforce U09.
- GroupDocument IN_PROGRESS/CLOSED; leader MANUAL submit/resubmit before deadline snapshots immutable receipt/version; AUTO_DEADLINE/AUTO_RETIRED locks closest persisted drafts and records SYSTEM. Submission history retained; editing current document never mutates submitted snapshots.
- Removing/replacing member/leader changes permissions next request, release claims safely per baseline warning/discard policy; audit authorship retained.
- Student AI context default own editable section D03, no whole-group export automatically. Graded feedback teacher allowed; personal connection only; result tied section/document version.
- Record per-member visibility through U11 ActivityWritePort under group snapshot policy. U14 does not own duplicate ActivityEvent store; teacher timeline through U15.
- SSE per-account/document channels recheck membership, no private draft of others; clock/version from backend, no owner bypass through collaboration.
