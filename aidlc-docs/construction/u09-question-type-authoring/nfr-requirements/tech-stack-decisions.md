# U09 Question Type Authoring - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: nội dung theo dạng bài của UC 35 (cài đặt quiz), 42, 43, 44, 45 và popup Rubric Detail của UC 46 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-003, US-ASM-004, US-ASM-005, US-ASM-006, US-ASM-007. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Đọc/ghi DOCX | Apache POI XWPF (đã có từ U06) | Như demo_do_an (`DocxOutlineImporter`, `DocxExporter`) |
| Đọc chunk PNG | Tự đọc chunk PNG bằng Java (`DataInputStream`, `Inflater` cho `zTXt`) | Định dạng đơn giản, không cần thư viện |
| SVG → PNG | JSVG (`com.github.weisj:jsvg`) | demo_do_an đã dùng, nhẹ, không cần trình duyệt |
| Làm sạch SVG | Parser XML an toàn + allowlist thẻ/thuộc tính tự viết | Kiểm soát được |
| Lưu cấu hình, khung | PostgreSQL `jsonb` | Như U06 |
| Trình soạn tài liệu | React tự viết theo block (không dùng editor thương mại) | Cần luật khóa block riêng |
| Draw.io | Iframe `embed.diagrams.net?embed=1&proto=json&spin=1` | Miễn phí, không tốn VPS |
