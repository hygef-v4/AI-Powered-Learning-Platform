# U15 Grading & Gradebook — tech-stack-decisions

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 42, 43, 44, 45. **Primary stories:** US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001. **Requirements:** FR-008/009/020/039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u15-grading` không quyết định chức năng hiện hành.

## Stack giữ theo dự án

Java 17/Spring Boot 3.5 modular monolith; Next.js 16/TypeScript, PostgreSQL/Redis/RabbitMQ, Nginx/Docker Compose, U03 Drive private và U01/U16 MailPort. Dependency lock/pin và actual APIs xác minh khi code; không chọn version SDK mới trong revision tài liệu.

- UC42 submissions/work detail plus activity review assigned teacher R3/R4. Admin or manager only R2 denied. Quiz Practice activity read does not create Gradebook.
- Only human teacher confirms Assignment GRADED official result, finalizes/publishes. AI proposal separate PENDING/READY/ACCEPTED/REJECTED, never automatically final/publish.
- Rubric scores deterministic by checklist/max, linked submission/rubric version; proposal stale if submission changed. Manual override requires reason/audit; batch finalize rejects/informs invalid rows, no silent grading.
- Practice Quiz scorer and Assignment feedback not official; answer reveal follows snapshot policy; Student own published official grades only.
- Group common document score + manual contribution adjustment, bounded and explained; activity/number edits cannot determine contribution or cheating score automatically.
- ActivityQueryPort U11 projection per member/work, after current class/policy check; disabled/no data/unavailable distinct. Does not mutate grading entities from telemetry.

## Cost và extension

AI API tổ chức/cá nhân do bên tương ứng trả, không giả định free/đã có key; infrastructure dùng resources nhóm có. Full Lesson/RAG không prerequisite; mọi embedding/provider config là extension riêng. Numeric policies D04/retention chưa giả định. Không thêm unit/container cho từng module.
