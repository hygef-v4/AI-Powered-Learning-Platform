# U09 Assignment Documents & Quiz Authoring — logical-components

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 35, 42, 45. **Primary stories:** US-GRP-003, US-ASM-004, US-ASM-006. **Requirements:** FR-006/016/017/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u09-question-type-authoring` không quyết định chức năng hiện hành.

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

document skeleton/editor/import/export; validate diagramEnabled/XML; Quiz questions/config; versioned activityMonitoringPolicy

## Owned data

- Document/SkeletonPart: blockId/type/content/partId, readonly scaffold/editable slots, optional diagram XML/hash/sanitized preview, immutable publicationVersion.
- AssignmentConfig: assessmentId/version/diagramEnabled/submissionMode, documentSkeleton/parts/rubricRefs, studentAiPolicy, activityPolicy(enabled/notice/retentionVersion).
- QuizConfig: questions/options/keys, time/attempt/reveal/shuffle policies and activityPolicy; student view projection strips keys.

## Critical paths

- Educator edits skeleton/settings → validates blocks/parts/rubric/policy → optimistic save → U08 review/publication.
- Draw.io launch from enabled block → validate postMessage origin/source/request correlation → preserve full XML/version → regenerate safe preview.
- AI proposal/import content → untrusted schema validation → educator diff/accept into draft only → update version; no auto-publication.

Authoring coverage: UC42 Individual and UC45 Group are separate entries into the common Assignment document/rubric model. Validate INDIVIDUAL/GROUP independently of diagramEnabled; subject-level individual allowed within R2, group only assigned class. UC35 Quiz creation can open from View Learning Material with optional current-material reference; it remains independent of Lesson. No Student draft-feedback opt-in.
