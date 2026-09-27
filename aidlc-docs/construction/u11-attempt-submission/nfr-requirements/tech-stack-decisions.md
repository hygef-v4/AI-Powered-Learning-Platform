# U11 Attempt & Submission - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu nội dung | Cột `content jsonb` trong `submissions` (TOAST nén, lưu ngoài dòng khi lớn) | Danh sách lượt chọn cột metadata nên không phải đọc nội dung lớn; bớt một bảng |
| Tự nộp | Job U02 `ATTEMPT_AUTO_SUBMIT` đặt tại `deadlineAt`, hoặc tạo khi U08 ngưng giao (`PublicationLifecyclePort`) | Dùng lại U02; không mất phản ứng như event |
| Nén request | Client gzip (`CompressionStream`), backend giải nén theo `Content-Encoding` | Giảm băng thông tự lưu |
| Rate limit | Bucket4j + Redis | Đã có |
| Tải thử | k6 (chạy trên máy dev) | Miễn phí |
