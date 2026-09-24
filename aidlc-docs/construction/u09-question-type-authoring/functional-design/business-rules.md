# U09 Question Type Authoring - Business Rules

## 1. Chung

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-01 | Cấu hình chỉ sửa khi bài `DRAFT`; người sửa là giảng viên của lớp (theo BR-U08-01). | U08 |
| BR-U09-02 | Không có đề chung cấp môn; Chủ nhiệm môn chỉ phát hành khi là giảng viên của lớp. | Câu 4, 8, 12 |
| BR-U09-03 | Loại bài: `QUIZ`, `ESSAY`, `DOCUMENT` (U09 cấu hình), `CODE_LAB` (U13), `GROUP` (U12/U14). `DRAWIO` đổi tên thành `DOCUMENT`. | Câu 5 |

## 2. Trắc nghiệm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-10 | `MCQ_MULTI` chấm đúng hết mới có điểm (chọn đủ đáp án đúng và không chọn sai). | Câu 1 |
| BR-U09-11 | Trộn câu / trộn đáp án: mỗi lượt làm một thứ tự, lưu seed trên lượt làm (U11). | Câu 2 |
| BR-U09-12 | Giới hạn thời gian tính từ lúc bắt đầu lượt, không vượt hạn đóng; hết giờ U11 tự nộp. | Câu 2 |
| BR-U09-13 | `showScoreAfterSubmit` và `showCorrectAnswers` (`NEVER`, `AFTER_SUBMIT`, `AFTER_CLOSE`). | Câu 2 |
| BR-U09-14 | Duyệt `QUIZ` cần mọi câu có đáp án hợp lệ và điểm > 0; báo đúng câu lỗi. | US-ASM-006 S2 |

## 3. Bài viết (`ESSAY`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-20 | Bài viết văn bản thường có định dạng cơ bản (đoạn, heading, danh sách, đậm/nghiêng); không bảng, ảnh, sơ đồ. | Câu 9 |
| BR-U09-21 | Không giới hạn số từ; chỉ có trần kỹ thuật 1 000 000 ký tự để bảo vệ hệ thống. | Câu 9 |
| BR-U09-22 | Ngôn ngữ tự nhiên nào cũng được; không phải cấu hình riêng. | US-ASM-007 S2 |

## 4. Bài tài liệu (`DOCUMENT`)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-30 | Tài liệu tự do: có thể trang trắng hoặc có khung của giảng viên; không giới hạn số từ. | Câu 6, 9 |
| BR-U09-31 | Block của giảng viên theo bảng quyền ở `domain-entities.md` §5: chữ/ảnh khóa; bảng và sơ đồ người học sửa được nhưng không xóa/di chuyển. | Câu 6, 11 |
| BR-U09-32 | Người học chèn block của mình ở bất kỳ vị trí nào. | Câu 11 |
| BR-U09-33 | `requiredDiagrams` (tùy chọn): mỗi loại 1-20 sơ đồ tối thiểu; kiểm lúc nộp. | demo_do_an |
| BR-U09-34 | Trần kỹ thuật: ≤ 2 000 block, ≤ 100 sơ đồ, mỗi XML sơ đồ ≤ 2 MB, mỗi ảnh ≤ 5 MB. | Thiết kế |
| BR-U09-35 | XML sơ đồ phải qua parser an toàn (không DOCTYPE/XXE/XInclude; gốc `mxfile` hoặc `mxGraphModel`). | BR-U03-10, US-ASM-004 S2 |
| BR-U09-36 | Lưu nháp: kiểm cấu trúc, id block duy nhất, block giảng viên khóa không bị đổi (so `contentHash`). | US-ASM-004 S2 |
| BR-U09-37 | Nộp: thêm kiểm mọi block giảng viên còn đủ, mọi sơ đồ không rỗng, đủ `requiredDiagrams`, tài liệu có nội dung của người học. | US-ASM-004 S1 |
| BR-U09-38 | Sơ đồ luôn hiển thị bằng SVG xem trước; không bao giờ hiện XML cho người dùng. Bấm vào mở Draw.io nhúng. | demo_do_an |

## 5. Nhập khung từ DOCX (giảng viên)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-40 | Nhận `.docx` ≤ 20 MB; chuyển heading (style Heading 1-6), đoạn văn, danh sách, bảng, ảnh thành block `TEACHER`. | Câu 6, 7 |
| BR-U09-41 | Ảnh PNG có chunk `tEXt`/`zTXt`/`iTXt` khóa `mxfile` (hoặc `mxGraphModel` cũ), hoặc SVG có thuộc tính `content` chứa `mxfile` → giải mã (URL-decode, base64 + inflate nếu nén) → qua BR-U09-35 → thành block `DIAGRAM` (loại `OTHER`, giảng viên đổi được). **Không dùng AI.** | Câu 6, 10 |
| BR-U09-42 | Ảnh không có dữ liệu Draw.io, hoặc giải mã/kiểm lỗi → giữ nguyên là block `IMAGE`. | Câu 6 |
| BR-U09-43 | Nội dung không hỗ trợ (textbox, SmartArt, công thức, header/footer) bỏ qua và liệt kê trong báo cáo nhập. | Thiết kế |
| BR-U09-44 | Kết quả nhập là bản xem trước; giảng viên sửa/xóa block rồi mới lưu làm khung. | Thiết kế |

## 6. Xuất DOCX

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-50 | Xuất tài liệu (khung + phần người học) ra `.docx` giữ heading, đoạn, danh sách, bảng, ảnh. | Câu 7 |
| BR-U09-51 | Sơ đồ xuất thành ảnh PNG dựng từ SVG, **nhúng lại XML Draw.io** vào chunk `tEXt` `mxfile` để mở lại được trong draw.io. | Câu 10 |
| BR-U09-52 | Người học xuất bài của mình; giảng viên xuất bài trong lớp mình dạy (U11/U15 kiểm quyền rồi gọi `DocxExportPort`). | Câu 7 |

## 7. Rút gọn XML cho AI

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U09-60 | Chỉ tạo khi giảng viên yêu cầu AI chấm (U13 gọi): giữ `mxCell` `id`, `value`, `parent`, `source`, `target`, `vertex`, `edge` và phần `style` quy định loại hình; bỏ tọa độ, màu, font. Bản đầy đủ không đổi. | US-ASM-004 S3 |
