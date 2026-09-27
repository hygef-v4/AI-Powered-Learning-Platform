# U04 Subject, Class, Enrollment & Learning Access - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA, Flyway | Như các unit trước |
| Khóa đồng thời | `SELECT ... FOR UPDATE` trên hàng ghi danh theo người học trong môn; `@Version` cho lớp | Đủ cho một backend |
| Rate limit mã mời | Bucket4j + Redis | Đã dùng ở U01 |
| Đọc CSV | Tự tách dòng (1 cột email) | Không cần thư viện |
| Event | `EventPublisherPort` của U02 | Đã có |
| Frontend | Next.js + Tailwind, component tự viết | Như các unit trước |
