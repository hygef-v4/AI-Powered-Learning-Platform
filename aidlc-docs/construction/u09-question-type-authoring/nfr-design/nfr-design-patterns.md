# U09 Question Type Authoring - NFR Design Patterns

## P1 - Mô hình tài liệu dùng chung một nơi
- Java: `sealed interface Block` + record từng loại, Jackson polymorphic theo `type`; TypeScript: kiểu sinh từ JSON schema `contracts/schemas/document.json` (một nguồn cho backend, frontend, U11, U15).
- `DocumentValidator` thuần (không I/O) cho `validateSkeleton`, `validateForSave`, `validateForSubmit` (NFR-U09-01).

## P2 - Khóa block bằng hash
- Khi lưu khung: `contentHash = sha256(canonical JSON)` cho block `TEACHER` loại chữ/ảnh; bảng/sơ đồ lưu hash của "vỏ" (id, loại, vị trí tương đối).
- `validateForSave`: mọi block `TEACHER` có mặt đúng thứ tự tương đối; block khóa phải trùng hash; người học không đổi `origin` (NFR-U09-31).

## P3 - Nhập DOCX theo luồng
1. `SafeZipGuard` duyệt mục ZIP trước khi mở: số mục, tổng giải nén, tỉ lệ nén (NFR-U09-10).
2. POI `XWPFDocument` duyệt `IBodyElement` theo thứ tự → `BlockMapper`.
3. Ảnh → `DiagramDetector`:
   - PNG: đọc chunk; `tEXt`/`iTXt` khóa `mxfile` → URL-decode; `zTXt` → inflate rồi URL-decode; khóa `mxGraphModel` → định dạng cũ.
   - SVG: parser an toàn, lấy thuộc tính `content` của `<svg>` → unescape.
   - Nếu `<diagram>` bên trong nén (base64 + raw deflate + URL-encode) → giải nén (≤ 2 MB).
   - Qua `SafeDrawioParser`; lỗi ở bất kỳ bước nào → trả `IMAGE` (BR-U09-42).
4. Ảnh còn lại lưu qua `ArtifactPort.store(DOCUMENT_IMAGE)`.
- Cùng parser phục vụ hai chế độ: nhập khung gắn `origin = TEACHER`; preview cho lượt DOCUMENT gắn `origin = LEARNER`. U11 kiểm chủ lượt/trạng thái trước khi gọi và chỉ lưu sau xác nhận qua `PUT /attempts/{id}/content` với `contentVersion`.

## P4 - Xuất DOCX có giới hạn
- `Semaphore(2)` (NFR-U09-03); `JsvgRasterizer` dựng PNG từ SVG đã làm sạch (scale 2x, tối đa 4000 px cạnh dài); `PngChunkWriter` chèn `tEXt` `mxfile` trước `IEND`.
- Trả stream, không lưu file lâu dài.

## P5 - Làm sạch SVG
- `SvgSanitizer`: parser an toàn, allowlist thẻ (`svg`, `g`, `path`, `rect`, `ellipse`, `circle`, `line`, `polyline`, `polygon`, `text`, `tspan`, `defs`, `marker`, `use`, `image` với `data:` ảnh), bỏ `script`, `foreignObject`, thuộc tính `on*`, `href` không phải `#` hoặc `data:image` (NFR-U09-13).
- Chạy khi lưu (khung và bài của người học); kết quả lưu thay SVG gốc.

## P6 - Draw.io nhúng
- `DrawioPanel` mở iframe `https://embed.diagrams.net/?embed=1&proto=json&spin=1&ui=min&lang=vi`; gửi `{action: "load", xml}`; nhận `save`/`export` với `format: "svg"`; kiểm `event.origin === "https://embed.diagrams.net"` (NFR-U09-15).
- Timeout tải 10 s → trạng thái "trình vẽ không khả dụng" (NFR-U09-20).

## P7 - Soạn thảo lớn
- `DocumentEditor` giữ state dạng mảng block bất biến; render ảo hóa (`react-window`) khi > 200 block (NFR-U09-04).
