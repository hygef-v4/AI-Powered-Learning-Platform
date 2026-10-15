# U14 Group Workspace & Submission — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 56. **Primary stories:** US-GRP-004, US-GRP-005. **Requirements:** FR-018/025/026/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u14-group-document-submission` không quyết định chức năng hiện hành.

## Component responsibility

| Layer | Responsibility |
|---|---|
| Controller / MCP adapter | Deserialize/validate, authenticated actor, minimal response; gọi domain service |
| Application service | Scope/state/version, orchestration qua ports, transaction owner |
| Domain policy/model | Rules, transitions, snapshot invariants |
| Repository | Owned entities only; optimistic/locking/unique constraints |
| Outbound adapters | Timeout/retry/provenance; không quyết định học vụ |
| UI | Entry UC, form/query status, không client-only security |

## Owner contract

claim/save group section; group context policy; leader submit/version; record own member activity through U11 port

## Owned data

- GroupDocument: id/groupId/assessmentVersionId/classId unique, status IN_PROGRESS/CLOSED, version, sharedBlocks, sections JSON, revisions JSON, submittedSnapshots JSON, policySnapshot.
- Section JSON: id/partId/order/title/status/claimedBy/assignedBy, draftBlocks/private-to-editor, publishedBlocks, contentVersion/lastAuthor.
- SectionRevision JSON: sectionId/partId/authorId/blocks/hash/timestamp immutable; GroupSubmission snapshot: document/version/sectionAuthors/submittedBy/mode/time/receiptHash immutable.

## Critical paths

- Publish/readiness → group doc from locked skeleton → member claim → autosave expectedVersion → Done revision → published content.
- Leader submit with group/doc/version/current schedule → snapshot → outbox → U15; deadline scanner idempotent auto-close incl nearest saved drafts.
- Member telemetry/AI → current membership/section edit scope → U11 events or U13 feedback → no own duplicated quota/activity stores.
