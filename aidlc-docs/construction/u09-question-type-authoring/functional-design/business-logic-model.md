# U09 Question Type Authoring - Business Logic Model

## F1 - Cấu hình bài
1. U08 tạo bài → U09 tạo `QuestionTypeConfig` mặc định theo loại.
2. Giảng viên sửa cấu hình khi bài `DRAFT` (BR-U09-01).

## F2 - Soạn khung tài liệu
1. Mở `DocumentEditor` chế độ khung; mọi block tạo ra là `TEACHER`.
2. Thêm/sửa/xóa/sắp xếp block; sơ đồ mở Draw.io nhúng, lưu XML + SVG (BR-U09-35, 38).
3. Lưu khung: kiểm `validateSkeleton`, tính `contentHash` từng block.

## F3 - Nhập khung từ DOCX
1. Upload DOCX (tạm, không lưu lâu) (BR-U09-40).
2. Duyệt tài liệu theo thứ tự, dựng block; mỗi ảnh chạy `DiagramDetector` (BR-U09-41, 42).
3. Trả bản xem trước + báo cáo (số sơ đồ nhận được, ảnh giữ nguyên, nội dung bị bỏ) (BR-U09-43).
4. Giảng viên chỉnh rồi lưu như F2 bước 3 (BR-U09-44).

## F4 - Kiểm duyệt (`TypeConfigCheckPort`)
- `QUIZ`: BR-U09-14. `ESSAY`: luôn đạt. `DOCUMENT`: khung (nếu có) hợp lệ, `requiredDiagrams` hợp lệ.

## F5 - Kiểm tài liệu của người học (U11 gọi)
- Lưu nháp: `validateForSave` (BR-U09-36).
- Nộp: `validateForSubmit` (BR-U09-37); trả danh sách lỗi theo `blockId`.

## F6 - Xuất DOCX
1. Duyệt block, dựng DOCX bằng POI.
2. Sơ đồ: SVG → PNG (rasterizer server), ghi chunk `tEXt` `mxfile` = XML đầy đủ đã URL-encode (BR-U09-51).

## F7 - Rút gọn XML (U13 gọi)
- `DiagramCompactor.compact(xml)` theo BR-U09-60.
