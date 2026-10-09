# U01 Account & Access - Infrastructure Design Plan

**Lịch sử plan baseline trước 2026-10-08**: giữ nguyên các bước đã hoàn thành và mã UC tại thời điểm đó. Thiết kế hiện hành và revision code plan theo [current SRS contract](../../current-srs-contract.md); task đồng bộ ở [construction-sync](../construction-sync-2026-10-08.md), không dùng checklist cũ như phạm vi mới.

- [x] Đọc Functional Design, NFR Requirements, NFR Design của U01.
- [x] Hỏi 7 nhóm: nơi triển khai, queue, quan sát, secret, backup, HTTPS, mã hóa at rest.
- [x] Ghi câu hỏi và đáp án vào `../infrastructure-design-questions/u01-account-and-access-infrastructure-design-questions.md`.
- [x] Ghi ngoại lệ vào `SEC-001`, `REL-002`, `REL-006`, `REL-008` (mã REL cũ, đã gộp khi rút gọn phạm vi ngày 2026-09-24).
- [x] Tạo `construction/shared-infrastructure.md` (dùng chung cho 16 unit).
- [x] Tạo `infrastructure-design.md` và `deployment-architecture.md` của U01.
- [x] Ghi compliance.
- [x] Trình duyệt Infrastructure Design trước khi sang Code Generation.
