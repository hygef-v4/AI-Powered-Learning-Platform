# U12 Group & Allocation - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; lưu nguyên khối bằng so khác biệt trạng thái gửi lên với DB | Một transaction |
| Ngẫu nhiên | `SecureRandom` + Fisher-Yates | Chuẩn |
| Frontend | Next.js + Tailwind; kéo thả thành viên giữa nhóm bằng `@dnd-kit` | Nhẹ, miễn phí |
