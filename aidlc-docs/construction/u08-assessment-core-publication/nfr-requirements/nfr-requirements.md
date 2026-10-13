# U08 Assignment/Quiz Lifecycle & Copy — nfr-requirements

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 41. **Primary stories:** US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. **Requirements:** FR-007/017/027/028.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u08-assessment-core-publication` không quyết định chức năng hiện hành.

## Required controls

- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

- Validation MIME/XML/schema/IDs/filter allowlists phù hợp unit, bounded payloads và rate limit phía server. Không log secret, raw prompt hoặc dữ liệu học vụ nhạy cảm.
- Truy vấn phân trang/index theo owner/resource/time, timeout hữu hạn mọi port/provider; không promise external API p95 như query local.
- Target baseline local APIs p95 500ms tại 100 concurrent/1000 accounts là mục tiêu cần benchmark, chưa có evidence; heavy files/AI/backup chạy worker và status queries.
- Session/password/header controls giữ SEC-001–006; quota/backup/credential protected riêng, không yêu cầu mã hóa toàn DB.

## Unit-specific NFR risks

- Assessment type chỉ ASSIGNMENT/QUIZ. Assignment submissionMode INDIVIDUAL/GROUP độc lập diagramEnabled; đủ bốn tổ hợp, không đổi gradingMode vì diagram.
- Baseline Assignment cá nhân GRADED/PRACTICE; Group chỉ GRADED D02; Quiz tạm PRACTICE D01, không Gradebook/lịch Graded mới.
- Owner R3/R4 bài lớp, R2 bài môn; bài môn chỉ INDIVIDUAL; Group bắt buộc class. Quiz không required lessonId, optional referenceIds.
- Lifecycle DRAFT → REVIEWED → PUBLISHED → RETIRED; sửa nháp reset review; publication locks config/rubric snapshot. Assignment open/close/deadline và nhận trễ theo policy preserved.
- Group publication kiểm groups/leader/enrollment readiness qua U12; không cần AI/indexing hoặc Lesson. Quiz publication cần valid keys/settings snapshot.
- Version/copy tạo document/rubric/questions mới độc lập, không sao attempts/submissions/grades/AI connections/class groups/lịch mới. Published snapshot không mutate; retire giữ history và auto-close work theo rules.

## Test evidence cần có

- All four Assignment combinations valid; Quiz no Lesson; Group at subject scope rejected.
- Stale review/publication rejects; copy no reused submission/rubric IDs; late/retire snapshots stable; AI outage manual publication succeeds.

Security enabled subset 03/04/05/08/09/12/15: constraints auth/input/audit/secret/failure được thể hiện, chưa runtime verified. Resiliency enabled subset 04/06/10: deployment/health/timeouts giữ; rule khác N/A theo state, PBT disabled.
