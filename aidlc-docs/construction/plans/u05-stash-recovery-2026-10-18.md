# U05 — khôi phục stash và giữ UI mới

Người dùng yêu cầu "giúp tôi xử lý" sau khi xem stash U05. Tiếp tục phạm vi độc lập đã được phép; không mở rộng sang tích hợp U01/U04, không commit/push hoặc xóa stash.

Nguồn cố định: `86fc91e092915f1b292019f782b99bbcfa55ee63` (stash của Class&Enrollment).

- [x] R1: Sao lưu file hiện tại và nội dung stash vào `.tmp/u05-stash-recovery-2026-10-18/`, kèm hash.
- [x] R2: Khôi phục 20 file backend/SQL/test và ba tài liệu U05; ghép audit theo kiểu append, giữ trạng thái/UI mới.
- [x] R3: Đối chiếu test UI cũ, chuyển tình huống còn thiếu sang component mới; đồng bộ quy tắc parser hai phía nếu có khác biệt.
- [x] R4: Chạy backend/frontend unit test, typecheck/lint; xác nhận UI không bị ghi đè và stash còn nguyên.
- [x] R5: Cập nhật trạng thái và tài liệu bàn giao; giữ integration U01/U04 ở trạng thái chờ.

Không dựng auth/schema giả. SQL vẫn nằm ngoài Flyway, adapter persistence chỉ bật qua cấu hình opt-in. Các test repository dùng mock không thay thế kiểm thử PostgreSQL/transaction thật. Giữ cấu hình extension hiện tại: Security/Resiliency subset, PBT disabled.
