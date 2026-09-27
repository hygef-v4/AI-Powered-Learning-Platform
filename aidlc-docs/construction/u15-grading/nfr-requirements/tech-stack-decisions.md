# U15 Grading - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; `items` `jsonb` | Như các unit trước |
| Chấm trắc nghiệm | Hàm thuần `QuizScorer` dùng `BankQueryPort` + câu riêng của bài | Xác định, dễ test |
| Nhận bài nộp | Port do U15 cài, gọi trong transaction nộp của U11/U14 và khi U13 chấm xong; tạo job `GRADE_INIT` trên `jobs.triggered` | Không làm chậm request nộp; không mất như event |
| Sổ điểm | Một query tổng hợp + index `(publication_id, learner_id)` | Đủ cho 200 × 30 |
