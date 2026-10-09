# U06 Rubric & Question Bank - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu `definition`, `criteria` | PostgreSQL `jsonb`, ánh xạ sang Java record theo `questionType` (Jackson polymorphic) và `RubricDefinition` | Một bảng, kiểu an toàn trong code |
| Kiểm hợp lệ | Bean Validation trên record + validator riêng từng loại câu và rubric | Thông báo lỗi theo trường |
| Đọc xlsx | Apache POI (`XSSF` event API), cấu hình `ZipSecureFile` | Chuẩn, có chống zip bomb |
| Đọc CSV | Apache Commons CSV | Xử lý dấu phẩy/xuống dòng trong ô |
| Tìm kiếm | Index B-tree `(scope_type, scope_id, status)`, GIN trên `tags`, `ILIKE` tiêu đề | Đủ cho ≤ 20 000 bản |
| Frontend | Next.js + Tailwind, component tự viết; `QuestionView`, `QuestionEditor`, `RubricEditor` dùng chung với U09, U11, U15 | Như các unit trước |
