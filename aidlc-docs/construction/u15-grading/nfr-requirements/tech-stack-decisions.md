# U15 Grading - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; `items` `jsonb` | Như các unit trước |
| Chấm trắc nghiệm | Hàm thuần `QuizScorer` dùng `BankQueryPort` + câu riêng của bài | Xác định, dễ test |
| Tiêu thụ event | Listener RabbitMQ trong `worker` (`u11.submission.submitted`, `u13.code.graded`, `u14.group.submitted`) | Không làm chậm request nộp |
| Sổ điểm | Một query tổng hợp + index `(publication_id, learner_id)` | Đủ cho 200 × 30 |
