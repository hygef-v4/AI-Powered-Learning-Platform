# U12 Group & Allocation - Tech Stack Decisions

**Bản tài liệu 2026-10-08**: UC 15, 16; primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; lưu nguyên khối bằng so khác biệt trạng thái gửi lên với DB | Một transaction |
| Ngẫu nhiên | `SecureRandom` + Fisher-Yates | Chuẩn |
| Frontend | Next.js + Tailwind; kéo thả thành viên giữa nhóm bằng `@dnd-kit` | Nhẹ, miễn phí |
