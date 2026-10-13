# U09 Assignment Documents & Quiz Authoring — nfr-design-patterns

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 35, 42, 45. **Primary stories:** US-GRP-003, US-ASM-004, US-ASM-006. **Requirements:** FR-006/016/017/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u09-question-type-authoring` không quyết định chức năng hiện hành.

## Patterns áp dụng

- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

- Optimistic concurrency cho UI edits, transaction locks/unique constraints cho contested claims/quota/receipt và outbox. Request fingerprint/version để replay không ghi sai.
- Failure retries chỉ khi adapter chứng minh safe; provider dispatch unknown không gọi lại ngầm; DB/outbox recovery dùng lease.
- Snapshot model/value objects immutable; secrets không trả từ repository qua generic serializer. Data retention tác động qua policy/job riêng không phá lịch sử official.

## Unit-specific patterns

- Document common Assignment editor; heading/paragraph/list/table/image and DIAGRAM when enabled, structured Document JSON + full Draw.io XML + sanitized preview; không flattened XML as source.
- Skeleton locked student vs editable content parts separated. Parts derived leaf heading per baseline; each partId links rubric. Author import/export DOCX scope/size/type verified; exact diagram roundtrip required or report unsupported representation, no silent loss.
- Diagram disabled reject diagram blocks/imports; enabled validate XML limits/external refs and sanitize SVG/HTML, Draw.io iframe origin/message checks; no eval/import external entity.
- Quiz authoring MCQ independent Lesson; immutable publication keys/options snapshot with question versions, hidden keys never Student context.
- activityMonitoringEnabled default false, policy/notice/retention version in author config and attempt snapshot; changing published policy requires new version, no silent mid-attempt switch.
- No Student feedback on drafts or Graded work; remove allowStudentAiFeedback. UC29 requires an owned SUBMITTED PRACTICE snapshot. Educator AI-assisted authoring remains separately authorized with explicit source selection and reviewed outputs.

## Validation matrix

- Diagram XML roundtrip; malicious/oversized XML/SVG/import reject; disabled diagram reject; valid pure doc works.
- Student cannot edit scaffold/key/rubric; Quiz keys stripped; policy snapshot unchanged mid-attempt; AI proposal stale rejected.

Authoring coverage: UC42 Individual and UC45 Group are separate entries into the common Assignment document/rubric model. Validate INDIVIDUAL/GROUP independently of diagramEnabled; subject-level individual allowed within R2, group only assigned class. UC35 Quiz creation can open from View Learning Material with optional current-material reference; it remains independent of Lesson. No Student draft-feedback opt-in.
