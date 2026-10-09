# U11 Attempt & Submission - Tech Stack Decisions

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Lưu nội dung | Cột `content jsonb` trong `attempts` (TOAST nén, lưu ngoài dòng khi lớn) | Danh sách lượt chọn cột metadata nên không phải đọc nội dung lớn; bớt một bảng |
| Lớp của lượt | Cột `class_id` trong `attempts` | Bài và quiz của môn dùng chung cho nhiều lớp; lọc danh sách gộp và chấm theo lớp không cần tra ghi danh lại |
| Tự nộp | Scanner U03 đọc `attempts.deadline_at`; ngưng giao đặt hạn ngay qua `AssignmentLifecyclePort` | Không cần bảng job; không mất phản ứng như event |
| Nén request | Client gzip (`CompressionStream`), backend giải nén theo `Content-Encoding` | Giảm băng thông tự lưu |
| Rate limit | Bucket4j + Redis | Đã có |
| Tải thử | k6 (chạy trên máy dev) | Miễn phí |
