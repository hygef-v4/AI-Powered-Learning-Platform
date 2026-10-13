# U08 Assignment/Quiz Lifecycle & Copy — business-rules

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 41. **Primary stories:** US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. **Requirements:** FR-007/017/027/028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u08-assessment-core-publication` không quyết định chức năng hiện hành.

## Quy tắc nghiệp vụ

- **BR-U08-01**: Assessment type chỉ ASSIGNMENT/QUIZ. Assignment submissionMode INDIVIDUAL/GROUP độc lập diagramEnabled; đủ bốn tổ hợp, không đổi gradingMode vì diagram.
- **BR-U08-02**: Baseline Assignment cá nhân GRADED/PRACTICE; Group chỉ GRADED D02; Quiz tạm PRACTICE D01, không Gradebook/lịch Graded mới.
- **BR-U08-03**: Owner R3/R4 bài lớp, R2 bài môn; bài môn chỉ INDIVIDUAL; Group bắt buộc class. Quiz không required lessonId, optional referenceIds.
- **BR-U08-04**: Lifecycle DRAFT → REVIEWED → PUBLISHED → RETIRED; sửa nháp reset review; publication locks config/rubric snapshot. Assignment open/close/deadline và nhận trễ theo policy preserved.
- **BR-U08-05**: Group publication kiểm groups/leader/enrollment readiness qua U12; không cần AI/indexing hoặc Lesson. Quiz publication cần valid keys/settings snapshot.
- **BR-U08-06**: Version/copy tạo document/rubric/questions mới độc lập, không sao attempts/submissions/grades/AI connections/class groups/lịch mới. Published snapshot không mutate; retire giữ history và auto-close work theo rules.
- **BR-U08-07**: Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- **BR-U08-08**: expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- **BR-U08-09**: Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- **BR-U08-10**: No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Negative scenarios / acceptance

- All four Assignment combinations valid; Quiz no Lesson; Group at subject scope rejected.
- Stale review/publication rejects; copy no reused submission/rubric IDs; late/retire snapshots stable; AI outage manual publication succeeds.
