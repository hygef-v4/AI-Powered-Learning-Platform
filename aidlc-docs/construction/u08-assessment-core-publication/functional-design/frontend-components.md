# U08 Assignment/Quiz Lifecycle & Copy — frontend-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 34. **Primary stories:** US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. **Requirements:** FR-007/017/027/028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u08-assessment-core-publication` không quyết định chức năng hiện hành.

## UI entry và API đề xuất

- /api/v1/assignments and /api/v1/quizzes; commands review/publish/retire/copy with expectedVersion, scope checks; AssessmentPolicyPort.

## Component/state contract

- List/Detail/Editor phân theo quyền và entry UC hiện hành; query state loading/ready/empty/unavailable/error tách biệt.
- Form state draft/dirty/saving/saved/conflict/invalid; save gửi expectedVersion và requestRef khi có idempotent command; lỗi không xóa draft.
- State-changing requests không chỉ dựa ẩn nút; server authorize, version/policy errors cần refresh lại context trước retry.
- UI không phơi bày raw keys, hidden Quiz answers, unpublished grades hoặc dữ liệu ngoài scope.

## Hành vi riêng unit

- Assessment type chỉ ASSIGNMENT/QUIZ. Assignment submissionMode INDIVIDUAL/GROUP độc lập diagramEnabled; đủ bốn tổ hợp, không đổi gradingMode vì diagram.
- Baseline Assignment cá nhân GRADED/PRACTICE; Group chỉ GRADED D02; Quiz tạm PRACTICE D01, không Gradebook/lịch Graded mới.
- Owner R3/R4 bài lớp, R2 bài môn; bài môn chỉ INDIVIDUAL; Group bắt buộc class. Quiz không required lessonId, optional referenceIds.
- Lifecycle DRAFT → REVIEWED → PUBLISHED → RETIRED; sửa nháp reset review; publication locks config/rubric snapshot. Assignment open/close/deadline và nhận trễ theo policy preserved.
- Group publication kiểm groups/leader/enrollment readiness qua U12; không cần AI/indexing hoặc Lesson. Quiz publication cần valid keys/settings snapshot.
- Version/copy tạo document/rubric/questions mới độc lập, không sao attempts/submissions/grades/AI connections/class groups/lịch mới. Published snapshot không mutate; retire giữ history và auto-close work theo rules.

## UI acceptance

- All four Assignment combinations valid; Quiz no Lesson; Group at subject scope rejected.
- Stale review/publication rejects; copy no reused submission/rubric IDs; late/retire snapshots stable; AI outage manual publication succeeds.
