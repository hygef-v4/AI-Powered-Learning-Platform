# U08 Assignment/Quiz Lifecycle & Copy — domain-entities

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 34. **Primary stories:** US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. **Requirements:** FR-007/017/027/028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u08-assessment-core-publication` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- Assessment: id/type/title/subjectId/classId nullable/sourceScope/ownerId/status/gradingMode/version, schedule/latePolicy Assignment, referenceIds optional.
- AssignmentSettings: submissionMode, diagramEnabled; QuizSettings: Practice mode, timeLimit/attemptLimit/reveal/shuffle; không enum bài riêng theo content.
- AssessmentVersion: immutable config/rubric/quiz snapshot, policyVersion, reviewedBy/publishedBy/timestamps, lineage(sourceId/version).

## Boundary

createAssignment(actor, submissionMode, diagramEnabled); createQuiz; review/publish/retire/version/copy

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- Assessment type chỉ ASSIGNMENT/QUIZ. Assignment submissionMode INDIVIDUAL/GROUP độc lập diagramEnabled; đủ bốn tổ hợp, không đổi gradingMode vì diagram.
- Baseline Assignment cá nhân GRADED/PRACTICE; Group chỉ GRADED D02; Quiz tạm PRACTICE D01, không Gradebook/lịch Graded mới.
- Owner R3/R4 bài lớp, R2 bài môn; bài môn chỉ INDIVIDUAL; Group bắt buộc class. Quiz không required lessonId, optional referenceIds.
- Lifecycle DRAFT → REVIEWED → PUBLISHED → RETIRED; sửa nháp reset review; publication locks config/rubric snapshot. Assignment open/close/deadline và nhận trễ theo policy preserved.
- Group publication kiểm groups/leader/enrollment readiness qua U12; không cần AI/indexing hoặc Lesson. Quiz publication cần valid keys/settings snapshot.
- Version/copy tạo document/rubric/questions mới độc lập, không sao attempts/submissions/grades/AI connections/class groups/lịch mới. Published snapshot không mutate; retire giữ history và auto-close work theo rules.
