# U03 File & Artifact - Business Rules

## 1. Upload

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-01 | Upload đi qua backend trong một request multipart. | Câu 2 |
| BR-U03-02 | Mọi file ≤ 50 MB; vượt thì từ chối trước khi đọc hết. | Câu 4 |
| BR-U03-03 | Loại file xác định bằng nội dung (magic bytes), phải khớp allowlist của `purpose`. Đuôi file và `Content-Type` của trình duyệt chỉ để tham khảo. | Câu 5 |
| BR-U03-04 | Quyền upload theo mục đích: `AVATAR` mọi người đã đăng nhập; `MATERIAL` giảng viên/chủ nhiệm môn/admin; `DRAWIO_FULL` giảng viên (đề) và người học (bài làm); `SUBMISSION_FILE` người học; `DOCUMENT_IMAGE` (PNG, JPEG, GIF, SVG ≤ 5 MB, ảnh trong bài tài liệu) giảng viên và người học. Mục đích dẫn xuất chỉ hệ thống tạo. | SEC-002 |
| BR-U03-05 | Tính SHA-256 khi nhận; lưu cùng artifact. | services.md |
| BR-U03-06 | Chỉ khi file đã lên Drive **và** dòng `artifacts` đã commit mới trả về `artifactId`. Lỗi ở bất kỳ bước nào → xóa file trên Drive nếu đã tạo, không để lại dòng nào. | Câu 7 |
| BR-U03-07 | Upload thành công là artifact hợp lệ, được giữ kể cả khi chưa gắn vào đối tượng nào; không có job dọn file chưa gắn. | Câu 7 |
| BR-U03-08 | Tên file gốc được làm sạch: bỏ đường dẫn, ký tự điều khiển, cắt còn 255 ký tự. | SEC-003 |

## 2. Draw.io XML

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-10 | Parser XML tắt DTD, external entity và XInclude; gặp khai báo DOCTYPE thì từ chối. | services.md, SEC-003 |
| BR-U03-11 | Phần tử gốc phải là `mxfile` hoặc `mxGraphModel`. | Allowlist Draw.io |
| BR-U03-12 | XML không hợp lệ bị từ chối với lỗi dễ hiểu, không lộ chi tiết parser. | SEC-006 |
| BR-U03-13 | `DRAWIO_FULL` là bản gốc, không bao giờ bị sửa hay xóa. | Invariant #5 |

## 3. Tải về

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-20 | U03 **không tự quyết** ai được xem file. Unit sở hữu kiểm quyền nghiệp vụ rồi gọi `issueDownloadToken`. Riêng `AVATAR`: mọi người đã đăng nhập được xem. | components.md |
| BR-U03-21 | Token hạn 5 phút, gắn với đúng `accountId`; người khác dùng token → từ chối. | Câu 3 |
| BR-U03-22 | Backend stream file từ Drive; không bao giờ trả `providerFileId` hay link Drive. | SEC-002 |
| BR-U03-23 | Header tải về: `Content-Type` đã xác định, `X-Content-Type-Options: nosniff`; file không phải ảnh/PDF luôn `attachment`. | SEC-004 |
| BR-U03-24 | Drive từ chối trả file vì abuse → chuyển `BLOCKED`, trả "file không khả dụng"; không dùng `acknowledgeAbuse`. | components.md §5 |
| BR-U03-25 | File `BLOCKED` hoặc `DELETED` không cấp token được. | Thiết kế |

## 4. Gắn và dẫn xuất

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-30 | `attach` chỉ gắn một lần; artifact đã gắn không chuyển sang đối tượng khác. Chỉ người tải lên hoặc hệ thống được gắn. | Thiết kế |
| BR-U03-31 | File dẫn xuất luôn có `sourceArtifactId`; file gốc phải `ACTIVE`. | ERD |
| BR-U03-32 | `DRAWIO_AI_COMPACT` bị xóa khỏi Drive và đánh dấu `DELETED` ngay khi job AI kết thúc (thành công hay lỗi). | Câu 6 |
| BR-U03-33 | Chỉ file dẫn xuất được xóa; file người dùng tải lên không bao giờ bị xóa. | Invariant #4 |

## 5. Audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U03-40 | Audit: upload bị từ chối, file bị `BLOCKED`, dùng token sai người. Không audit từng lần tải thành công. | SEC-005 |
| BR-U03-41 | Drive không khả dụng: upload và tải về trả "tạm thời không khả dụng"; không để lại dữ liệu dở dang. | REL-003 |
