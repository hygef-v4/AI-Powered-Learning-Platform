# U11 Attempt & Submission - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu nội dung | Cột `content jsonb` trong `attempts` (TOAST nén, lưu ngoài dòng khi lớn) | Danh sách lượt chọn cột metadata nên không phải đọc nội dung lớn; bớt một bảng |
| Tự nộp | Scanner U03 đọc `attempts.deadline_at`; ngưng giao đặt hạn ngay qua `AssignmentLifecyclePort` | Không cần bảng job; không mất phản ứng như event |
| Nén request | Client gzip (`CompressionStream`), backend giải nén theo `Content-Encoding` | Giảm băng thông tự lưu |
| Rate limit | Bucket4j + Redis | Đã có |
| Tải thử | k6 (chạy trên máy dev) | Miễn phí |
