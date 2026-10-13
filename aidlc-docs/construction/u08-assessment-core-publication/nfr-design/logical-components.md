# U08 Assignment/Quiz Lifecycle & Copy — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 41. **Primary stories:** US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. **Requirements:** FR-007/017/027/028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u08-assessment-core-publication` không quyết định chức năng hiện hành.

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

createAssignment(actor, submissionMode, diagramEnabled); createQuiz; review/publish/retire/version/copy

## Owned data

- Assessment: id/type/title/subjectId/classId nullable/sourceScope/ownerId/status/gradingMode/version, schedule/latePolicy Assignment, referenceIds optional.
- AssignmentSettings: submissionMode, diagramEnabled; QuizSettings: Practice mode, timeLimit/attemptLimit/reveal/shuffle; không enum bài riêng theo content.
- AssessmentVersion: immutable config/rubric/quiz snapshot, policyVersion, reviewedBy/publishedBy/timestamps, lineage(sourceId/version).

## Critical paths

- Create type/config → U09 validate document/Quiz → U06 rubric/questions → review → publish snapshot/readiness → opened event.
- Student start gets published snapshot → U11/U14; retire schedules deterministic auto-submit/close after commit.
- Copy current authorized version → new draft/config/rubric IDs → re-review; teacher cannot edit bài môn not owner.
