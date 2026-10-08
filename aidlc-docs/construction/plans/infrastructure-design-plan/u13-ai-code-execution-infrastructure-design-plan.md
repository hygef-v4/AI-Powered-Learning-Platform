# U13 AI & Code Execution - Infrastructure Design Plan

**Lịch sử plan baseline trước 2026-10-08**: giữ nguyên các bước đã hoàn thành và mã UC tại thời điểm đó. Thiết kế hiện hành và revision code plan theo [current SRS contract](../../current-srs-contract.md); task đồng bộ ở [construction-sync](../construction-sync-2026-10-08.md), không dùng checklist cũ như phạm vi mới.

- [x] Đọc NFR Design U13, `shared-infrastructure.md`, `docker-compose.yml` và `judge0.conf` của `demo_do_an`.
- [x] Đánh giá 7 nhóm câu hỏi: Judge0 tự chạy, Gemini, VPS đã chốt; **không có câu hỏi mới**.
- [x] Tạo `infrastructure-design.md` và `deployment-architecture.md`.
- [x] Sửa `shared-infrastructure.md`: 4 container Judge0, mạng `sandbox`, secret `JUDGE0_AUTH_TOKEN`, cấu hình VPS gợi ý 4 vCPU / 8 GB.
- [x] Trình duyệt Infrastructure Design U13.
