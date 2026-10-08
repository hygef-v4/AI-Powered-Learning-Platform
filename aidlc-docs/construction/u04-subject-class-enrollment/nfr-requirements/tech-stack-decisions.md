# U04 Subject, Class, Enrollment & Learning Access - Tech Stack Decisions

**Bản tài liệu 2026-10-08**: UC 12, 13, 27, 28, 45, 46, 47, 48, 49, 50, 63, 64, 65, 66; primary stories: US-CAT-001, US-CAT-002, US-CAT-003, US-CAT-005, US-LRN-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu trữ | PostgreSQL + Spring Data JPA, Flyway | Như các unit trước |
| Khóa đồng thời | `SELECT ... FOR UPDATE` trên hàng ghi danh theo người học trong môn; `@Version` cho lớp | Đủ cho một backend |
| Rate limit mã mời | Bucket4j + Redis | Đã dùng ở U01 |
| Đọc CSV | Tự tách dòng (1 cột email) | Không cần thư viện |
| Event | `EventPublisherPort` của U03 | Đã có |
| Frontend | Next.js + Tailwind, component tự viết | Như các unit trước |
