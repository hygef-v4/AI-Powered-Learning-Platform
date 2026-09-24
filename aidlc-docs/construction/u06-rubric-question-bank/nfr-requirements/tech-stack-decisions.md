# U06 Rubric & Question Bank - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu `definition` | PostgreSQL `jsonb`, ánh xạ sang Java record theo `questionType` (Jackson polymorphic) | Một bảng, kiểu an toàn trong code |
| Kiểm hợp lệ | Bean Validation trên record + validator riêng từng loại | Thông báo lỗi theo trường |
| Đọc xlsx | Apache POI (`XSSF` event API), cấu hình `ZipSecureFile` | Chuẩn, có chống zip bomb |
| Đọc CSV | Apache Commons CSV | Xử lý dấu phẩy/xuống dòng trong ô |
| Tìm kiếm | Index B-tree `(subject_id, scope_type, class_id, status)`, GIN trên `tags`, `ILIKE` tiêu đề | Đủ cho ≤ 20 000 bản |
| Frontend | Next.js + Tailwind, component tự viết; component hiển thị câu hỏi dùng chung với U11 | Như các unit trước |
