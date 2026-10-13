# U11 Individual Work & Quiz Submission — logical-components

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 18, 19, 20, 21, 22, 23, 24, 28, 29. **Primary stories:** US-ASM-003, US-ASM-012. **Requirements:** FR-018/019/030/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u11-attempt-submission` không quyết định chức năng hiện hành.

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

start/save/submit individual/Quiz; requestFeedback(actor, contentVersion, connectionRef); feedbackHistory; recordActivityEvent/queryScopedActivity (individual/Quiz/group)

## Owned data

- Attempt: id/assessmentVersionId/classId/accountId/attemptNo unique, status, content/documentOrAnswers, snapshot, contentVersion, startedAt/deadline/submittedAt, submitMode/late/receiptHash.
- DraftFeedback: id/owner/workRef/contentVersion/sourceConnection/taskRef/status/privateResult/timestamp; authoritative result tied to input snapshot, stale retained history not applied.
- ActivityEvent: id/actorId/classId/workKind(ATTEMPT/GROUP_DOCUMENT)/workId/sessionId/clientEventId/sequence/kind/clientTime/serverTime/policyVersion; unique actor/work/clientEventId.
- ActivitySummary projection: intervals/count/knownAwayDuration/unknownGaps/provenance; derived from events, never fraud verdict.

## Critical paths

- Start snapshot/policy notice → autosave expectedVersion → submit transaction freezes snapshot/receipt → outbox submission → U15.
- Draft feedback → verify ownership/teacher policy + capture contentVersion → U13 personal task → result linked version, never mutate official record.
- Browser visibility event → authenticated minimal payload → dedup/rate/state/policy → persist; query → current authorization → trusted/missing-data flags and per-member timeline.
