# U03 File & Artifact - Business Rules

## 1. Upload

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-01 | Upload đi qua backend trong một request multipart. | Câu 2 |
| BR-U03-02 | Trần dung lượng theo `purpose`: `AVATAR`, `MATERIAL` ≤ 50 MB; `DOCUMENT_IMAGE` ≤ 5 MB. Vượt thì từ chối trước khi đọc hết. | Câu 4, BR-U09-34 |
| BR-U03-03 | Loại file xác định bằng nội dung (magic bytes), phải khớp allowlist của `purpose`. Đuôi file và `Content-Type` của trình duyệt chỉ để tham khảo. | Câu 5 |
| BR-U03-04 | Quyền upload theo mục đích: `AVATAR` mọi người đã đăng nhập; `MATERIAL` giảng viên/chủ nhiệm môn/admin; `DOCUMENT_IMAGE` (ảnh trong khung đề, bài làm, tài liệu nhóm) giảng viên, chủ nhiệm môn và người học. U03 không có file do hệ thống tạo. | SEC-002 |
| BR-U03-05 | Tính SHA-256 khi nhận; lưu cùng artifact. | services.md |
| BR-U03-06 | Chỉ khi file đã lên Drive **và** dòng `artifacts` đã commit mới trả về `artifactId`. Lỗi ở bất kỳ bước nào → xóa file trên Drive nếu đã tạo, không để lại dòng nào. | Câu 7 |
| BR-U03-07 | Upload thành công là artifact hợp lệ, được giữ kể cả khi chưa gắn vào đối tượng nào; không có job dọn file chưa gắn. | Câu 7 |
| BR-U03-08 | Tên file gốc được làm sạch: bỏ đường dẫn, ký tự điều khiển, cắt còn 255 ký tự. | SEC-003 |

## 2. Ảnh SVG và Draw.io

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-10 | Ảnh SVG (`DOCUMENT_IMAGE`) khi tải luôn kèm header `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; sandbox` để script trong SVG không chạy. | SEC-004 |
| BR-U03-11 | U03 không lưu và không kiểm XML Draw.io: XML đầy đủ nằm trong block `DIAGRAM` của tài liệu, do U09 kiểm (BR-U09-35); bản rút gọn cho AI do U09 tạo trong bộ nhớ, không lưu. | U09, Invariant #5 |

## 3. Tải về

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-20 | U03 **không tự quyết** ai được xem file. Unit sở hữu kiểm quyền nghiệp vụ rồi gọi `issueDownloadToken`. Riêng `AVATAR`: mọi người đã đăng nhập được xem. | components.md |
| BR-U03-21 | Token hạn 5 phút, gắn với đúng `accountId`; người khác dùng token → từ chối. | Câu 3 |
| BR-U03-22 | Backend stream file từ Drive; không bao giờ trả `providerFileId` hay link Drive. | SEC-002 |
| BR-U03-23 | Header tải về: `Content-Type` đã xác định, `X-Content-Type-Options: nosniff`; file không phải ảnh/PDF luôn `attachment`. | SEC-004 |
| BR-U03-24 | Drive từ chối trả file vì abuse → chuyển `BLOCKED`, trả "file không khả dụng"; không dùng `acknowledgeAbuse`. | components.md §5 |
| BR-U03-25 | File `BLOCKED` không cấp token được. | Thiết kế |

## 4. Gắn và lưu giữ

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-30 | `attach` chỉ gắn một lần; artifact đã gắn không chuyển sang đối tượng khác. Chỉ người tải lên hoặc hệ thống được gắn. | Thiết kế |
| BR-U03-31 | Không có file dẫn xuất; mọi artifact do người dùng tải lên. | Rút gọn U03 (2026-09-25) |
| BR-U03-32 | File đã upload thành công không bị xóa; U03 không có chức năng xóa file. | Invariant #4 |

## 5. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-40 | Audit: upload bị từ chối, file bị `BLOCKED`, dùng token sai người. Không audit từng lần tải thành công. | SEC-005 |
| BR-U03-41 | Drive không khả dụng: upload và tải về trả "tạm thời không khả dụng"; không để lại dữ liệu dở dang. | REL-003 |
