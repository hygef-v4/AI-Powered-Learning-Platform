# U08 Assessment Core & Publication - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; câu riêng của bài lưu ở `questions` của U06 (`scope_type = ASSIGNMENT`) qua `InlineQuestionPort`, `config` là `jsonb` | Một định dạng câu hỏi |
| Lịch | `AssignmentScheduleScanner` (U03 `ScheduledScanner`, mỗi phút) đọc `opens_at`/`closes_at` của `assignments` | Không thêm scheduler, không có bảng job |
| Giờ | `java.time.Instant`/`OffsetDateTime`; frontend `date-fns-tz` | Chuẩn |
| Frontend | Next.js + Tailwind; dùng lại `QuestionEditor`, `QuestionView` của U06 (`RubricEditor` dùng trong panel rubric của U09) | Tránh viết lại |
