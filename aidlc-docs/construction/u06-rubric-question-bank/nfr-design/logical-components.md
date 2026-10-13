# U06 Rubrics & Subject Quiz Bank — logical-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 46, 56, 57. **Primary stories:** US-QBK-001, US-QBK-002. **Requirements:** FR-016/008.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u06-rubric-question-bank` không quyết định chức năng hiện hành.

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

rubric CRUD/validate/lock/snapshot; MCQ bank version/select

## Owned data

- QuizBankQuestion: id/subjectId/version/status/stem/options[{id,text}]/correctOptionIds/maxScore; MCQ validation single/multiple selection theo cấu hình.
- Rubric: id/assignmentPartId/version/checklist[{criterionId,text,points}]/maxScore/lockedAt; RubricSnapshot immutable.
- ScoreResult: rubricVersion/itemOutcomes/total/evidence; không do AI cộng điểm ngoài rule.

## Critical paths

- Manager draft MCQ → validate options/key → publish/retire; author copies version snapshot into Quiz.
- Document part add → empty rubric → educator fills checklist → validation/lock → scorer computes sum from validated item decisions.
