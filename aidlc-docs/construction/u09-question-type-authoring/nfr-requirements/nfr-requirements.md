# U09 Question Type Authoring - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U09-01 | `validateForSave` p95 ≤ 200 ms với tài liệu 500 block, 20 sơ đồ. | U11 lưu nháp thường xuyên |
| NFR-U09-02 | Nhập DOCX 50 trang, 20 ảnh ≤ 15 s (đồng bộ), áp dụng cho giảng viên và preview của người học. | BR-U09-40, 45 |
| NFR-U09-03 | Xuất DOCX tài liệu 20 sơ đồ ≤ 20 s; tối đa 2 lần xuất đồng thời trên backend (semaphore), vượt → `503` thử lại sau. | BR-U09-50 |
| NFR-U09-04 | Trình soạn tài liệu mượt với 500 block trên máy tính thông thường (chỉ render block trong vùng nhìn thấy khi > 200 block). | NFR-002 |

## 2. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U09-10 | DOCX là ZIP: giới hạn 20 MB nén, 200 MB giải nén, ≤ 1 000 mục, tỉ lệ nén tối thiểu (chống zip bomb). | SEC-003 |
| NFR-U09-11 | XML sơ đồ và XML trong DOCX parse với DTD/external entity/XInclude tắt. | BR-U09-35 |
| NFR-U09-12 | Giải nén dữ liệu Draw.io trong PNG/SVG giới hạn 2 MB sau giải nén. | BR-U09-41 |
| NFR-U09-13 | SVG xem trước từ client: server làm sạch (bỏ `script`, `foreignObject` chứa HTML, thuộc tính `on*`, `href` ngoài), frontend hiển thị bằng `<img>` từ data URL; không chèn SVG thô vào DOM. | SEC-003 |
| NFR-U09-14 | Nội dung chữ render qua React (không `dangerouslySetInnerHTML`). | SEC-003 |
| NFR-U09-15 | Iframe Draw.io từ `https://embed.diagrams.net` (CSP `frame-src`); chỉ nhận `postMessage` có `origin` đúng. Dữ liệu vẽ nằm trong trình duyệt, không gửi lên server draw.io. | Câu N1, SEC-004 |

## 3. Khả dụng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U09-20 | `embed.diagrams.net` không tải được → khối sơ đồ vẫn hiện SVG, nút vẽ báo "trình vẽ tạm thời không khả dụng"; các block khác vẫn soạn được. | REL-003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U09-30 | Unit test mọi `BR-U09-xx`; bộ DOCX mẫu: có PNG draw.io nhúng XML, SVG draw.io, ảnh thường, ảnh PNG đã nén lại (mất XML), bảng gộp ô, textbox. | NFR-004 |
| NFR-U09-31 | Test chặn: sửa block khóa, xóa bảng của giảng viên, DOCX zip bomb, SVG có script, XML có DOCTYPE; nhập DOCX của người học không đổi khung và lỗi không mất bản nháp. | NFR-004 |
| NFR-U09-32 | Test vòng tròn: xuất DOCX rồi nhập lại → sơ đồ vẫn nhận được (XML nhúng còn). | BR-U09-51 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung tài liệu |
| SECURITY-04 | Compliant | NFR-U09-15 |
| SECURITY-05 | Compliant | NFR-U09-10…14 |
| SECURITY-08 | Compliant | Quyền theo U08; khóa block phía server |
| SECURITY-15 | Compliant | NFR-U09-20, lỗi nhập ảnh → giữ ảnh |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
