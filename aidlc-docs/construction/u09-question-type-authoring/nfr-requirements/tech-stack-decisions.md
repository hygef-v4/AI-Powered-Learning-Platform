# U09 Question Type Authoring - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Đọc/ghi DOCX | Apache POI XWPF (đã có từ U06) | Như demo_do_an (`DocxOutlineImporter`, `DocxExporter`) |
| Đọc chunk PNG | Tự đọc chunk PNG bằng Java (`DataInputStream`, `Inflater` cho `zTXt`) | Định dạng đơn giản, không cần thư viện |
| SVG → PNG | JSVG (`com.github.weisj:jsvg`) | demo_do_an đã dùng, nhẹ, không cần trình duyệt |
| Làm sạch SVG | Parser XML an toàn + allowlist thẻ/thuộc tính tự viết | Kiểm soát được |
| Lưu cấu hình, khung | PostgreSQL `jsonb` | Như U06 |
| Trình soạn tài liệu | React tự viết theo block (không dùng editor thương mại) | Cần luật khóa block riêng |
| Draw.io | Iframe `embed.diagrams.net?embed=1&proto=json&spin=1` | Miễn phí, không tốn VPS |
