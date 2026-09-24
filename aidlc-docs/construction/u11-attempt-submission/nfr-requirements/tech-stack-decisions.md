# U11 Attempt & Submission - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu nội dung | Bảng riêng `submission_contents` (`jsonb`, TOAST nén) tách khỏi `submissions` | Danh sách lượt không phải đọc nội dung lớn |
| Tự nộp | Job U02 `U11_AUTO_SUBMIT` đặt tại `deadlineAt`; listener event `ASSIGNMENT_RETIRED` | Dùng lại U02 |
| Nén request | Client gzip (`CompressionStream`), backend giải nén theo `Content-Encoding` | Giảm băng thông tự lưu |
| Rate limit | Bucket4j + Redis | Đã có |
| Tải thử | k6 (chạy trên máy dev) | Miễn phí |
