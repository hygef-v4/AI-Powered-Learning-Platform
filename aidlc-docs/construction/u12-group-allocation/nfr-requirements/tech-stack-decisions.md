# U12 Group & Allocation - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 16, 17 và luồng phụ nhóm (quản lý nhóm, duyệt yêu cầu đổi trưởng nhóm trong tab Students của UC 32; sẵn sàng phát hành bài nhóm của UC 45) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-001, US-GRP-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu | PostgreSQL + JPA; lưu nguyên khối bằng so khác biệt trạng thái gửi lên với DB | Một transaction |
| Ngẫu nhiên | `SecureRandom` + Fisher-Yates | Chuẩn |
| Frontend | Next.js + Tailwind; kéo thả thành viên giữa nhóm bằng `@dnd-kit`; component gắn vào màn của U04 (tab Students, Student Class Detail) và U08 (hộp phát hành) | Nhẹ, miễn phí; theo screen flow không có màn nhóm riêng |
