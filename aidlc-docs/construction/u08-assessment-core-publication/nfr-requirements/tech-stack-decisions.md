# U08 Assessment Core & Publication - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 41 và vòng đời (tạo, xóa, duyệt, phát hành, lịch, ngưng giao, nhân bản, version) của UC 35, 42, 43, 44, 45 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; câu riêng của bài lưu ở `questions` của U06 (`scope_type = ASSIGNMENT`) qua `InlineQuestionPort`, `config` là `jsonb` | Một định dạng câu hỏi |
| Lịch | `AssignmentScheduleScanner` (U03 `ScheduledScanner`, mỗi phút) đọc `opens_at`/`closes_at` của `assignments` | Không thêm scheduler, không có bảng job |
| Giờ | `java.time.Instant`/`OffsetDateTime`; frontend `date-fns-tz` | Chuẩn |
| Frontend | Next.js + Tailwind; dùng lại `QuestionEditor`, `QuestionPicker`, `QuestionView` của U06 trong Quiz Detail và Assignment Form (`RubricEditor` dùng trong popup Rubric Detail của U09) | Tránh viết lại |
