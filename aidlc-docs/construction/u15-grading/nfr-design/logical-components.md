# U15 Grading & Gradebook — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001. **Requirements:** FR-008/009/020/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u15-grading` không quyết định chức năng hiện hành.

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

proposal validation; manual evaluation; finalize/publish/history/gradebook; reviewWorkActivity(authorizedTeacher, attempt/member) within UC 42 via U11 ActivityQueryPort

## Owned data

- Evaluation: id/submissionRef/classId/studentId/groupRef/kind(PRACTICE/OFFICIAL)/rubricVersion, outcomes/score/feedback/status/version/finalizedBy/publishedBy/timestamps.
- GradingProposal: id/sourceTaskRef/submissionVersion/rubricVersion/providerProvenance/validatedOutcomes/status; acceptance expectedEvaluationVersion.
- GradeHistory immutable: before/after/reason/actor/timestamp; group contribution adjustment explicit bounded per student; no ActivityEvent tables in U15.

## Critical paths

- Submission event → register scope/snapshot → teacher manual or selected-source AI request → validate proposal/rubric → teacher confirms → finalize → publish event.
- Student view grade only published own; teacher export/Gradebook official Assignments only via U16.
- UC42 activity tab → authorize → U11 scoped query → display known intervals/gaps; no grade mutation or verdict automation.
