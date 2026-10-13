# U08 Assignment/Quiz Lifecycle & Copy — nfr-design-patterns

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 41. **Primary stories:** US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. **Requirements:** FR-007/017/027/028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u08-assessment-core-publication` không quyết định chức năng hiện hành.

## Patterns áp dụng

- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

- Optimistic concurrency cho UI edits, transaction locks/unique constraints cho contested claims/quota/receipt và outbox. Request fingerprint/version để replay không ghi sai.
- Failure retries chỉ khi adapter chứng minh safe; provider dispatch unknown không gọi lại ngầm; DB/outbox recovery dùng lease.
- Snapshot model/value objects immutable; secrets không trả từ repository qua generic serializer. Data retention tác động qua policy/job riêng không phá lịch sử official.

## Unit-specific patterns

- Assessment type chỉ ASSIGNMENT/QUIZ. Assignment submissionMode INDIVIDUAL/GROUP độc lập diagramEnabled; đủ bốn tổ hợp, không đổi gradingMode vì diagram.
- Baseline Assignment cá nhân GRADED/PRACTICE; Group chỉ GRADED D02; Quiz tạm PRACTICE D01, không Gradebook/lịch Graded mới.
- Owner R3/R4 bài lớp, R2 bài môn; bài môn chỉ INDIVIDUAL; Group bắt buộc class. Quiz không required lessonId, optional referenceIds.
- Lifecycle DRAFT → REVIEWED → PUBLISHED → RETIRED; sửa nháp reset review; publication locks config/rubric snapshot. Assignment open/close/deadline và nhận trễ theo policy preserved.
- Group publication kiểm groups/leader/enrollment readiness qua U12; không cần AI/indexing hoặc Lesson. Quiz publication cần valid keys/settings snapshot.
- Version/copy tạo document/rubric/questions mới độc lập, không sao attempts/submissions/grades/AI connections/class groups/lịch mới. Published snapshot không mutate; retire giữ history và auto-close work theo rules.

## Validation matrix

- All four Assignment combinations valid; Quiz no Lesson; Group at subject scope rejected.
- Stale review/publication rejects; copy no reused submission/rubric IDs; late/retire snapshots stable; AI outage manual publication succeeds.
