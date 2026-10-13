# U09 Assignment Documents & Quiz Authoring — domain-entities

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 35, 42. **Primary stories:** US-GRP-003, US-ASM-004, US-ASM-006. **Requirements:** FR-006/016/017/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u09-question-type-authoring` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- Document/SkeletonPart: blockId/type/content/partId, readonly scaffold/editable slots, optional diagram XML/hash/sanitized preview, immutable publicationVersion.
- AssignmentConfig: assessmentId/version/diagramEnabled/submissionMode, documentSkeleton/parts/rubricRefs, studentAiPolicy, activityPolicy(enabled/notice/retentionVersion).
- QuizConfig: questions/options/keys, time/attempt/reveal/shuffle policies and activityPolicy; student view projection strips keys.

## Boundary

document skeleton/editor/import/export; validate diagramEnabled/XML; Quiz questions/config; versioned activityMonitoringPolicy

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- Document common Assignment editor; heading/paragraph/list/table/image and DIAGRAM when enabled, structured Document JSON + full Draw.io XML + sanitized preview; không flattened XML as source.
- Skeleton locked student vs editable content parts separated. Parts derived leaf heading per baseline; each partId links rubric. Author import/export DOCX scope/size/type verified; exact diagram roundtrip required or report unsupported representation, no silent loss.
- Diagram disabled reject diagram blocks/imports; enabled validate XML limits/external refs and sanitize SVG/HTML, Draw.io iframe origin/message checks; no eval/import external entity.
- Quiz authoring MCQ independent Lesson; immutable publication keys/options snapshot with question versions, hidden keys never Student context.
- activityMonitoringEnabled default false, policy/notice/retention version in author config and attempt snapshot; changing published policy requires new version, no silent mid-attempt switch.
- allowStudentAiFeedback for Graded default deny unless educator allows; assignment doc draft AI only authorized edit context; personal/shared/MCP chosen explicitly; author reviews outputs.
