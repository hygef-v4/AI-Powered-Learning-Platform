# U15 Grading - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; `rubric_checks`, `history` là `jsonb`; thời điểm công bố theo lớp suy ra từ `evaluations.published_at` | Như các unit trước; bài của môn công bố riêng từng lớp; chỉ bảng của ERD |
| Chấm quiz luyện tập | Hàm thuần `QuizScorer` dùng `BankQueryPort` (version câu đã ghim, gồm câu riêng của quiz) | Xác định, dễ test |
| Nhận bài nộp | Port do U15 cài, gọi trong transaction nộp của U11/U14 và khi U13 chấm xong; ghi thẳng `evaluations` | Không mất như event; không cần job riêng |
| Sổ điểm, Submission Detail | Query theo `(assignment_id, class_id)` trên `evaluations`; ghi danh × bài `GRADED` của lớp và của môn; index `evaluations (assignment_id, class_id, status)`, `(account_id, kind)` | Đủ cho 200 × 30; không join qua `attempts`/`group_documents` để lọc lớp |
