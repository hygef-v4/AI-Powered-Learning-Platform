# U15 Grading - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; `rubric_checks`, `history` là `jsonb` | Như các unit trước |
| Chấm trắc nghiệm | Hàm thuần `QuizScorer` dùng `BankQueryPort` + câu riêng của bài | Xác định, dễ test |
| Nhận bài nộp | Port do U15 cài, gọi trong transaction nộp của U11/U14 và khi U13 chấm xong; ghi thẳng `evaluations` (trắc nghiệm chấm luôn, chỉ vài ms) | Không mất như event; không cần job riêng |
| Sổ điểm | Một query tổng hợp: ghi danh × bài `GRADED`, join `attempts`/`group_documents` lấy `evaluations`; index `attempts (assignment_id, account_id)`, `evaluations (account_id, kind)` | Đủ cho 200 × 30 |
