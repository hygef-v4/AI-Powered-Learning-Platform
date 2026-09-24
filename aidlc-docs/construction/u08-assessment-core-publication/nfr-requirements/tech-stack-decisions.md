# U08 Assessment Core & Publication - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; `inlineDefinition` `jsonb` dùng lại record của U06 | Một định dạng câu hỏi |
| Lịch | Job U02 với `next_attempt_at = opensAt/closesAt` (`U08_PUBLICATION_OPEN`, `U08_PUBLICATION_CLOSE`) | Không thêm scheduler |
| Giờ | `java.time.Instant`/`OffsetDateTime`; frontend `date-fns-tz` | Chuẩn |
| Frontend | Next.js + Tailwind; dùng lại `QuestionEditor`, `QuestionView` của U06 | Tránh viết lại |
