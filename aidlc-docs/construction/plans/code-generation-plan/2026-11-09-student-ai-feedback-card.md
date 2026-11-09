# Thẻ Góp ý AI của sinh viên

Phạm vi U08, UC 40–41, FR-030 và US-ASM-012. Sinh viên đã chọn thẻ luôn hiện trên Lịch sử nộp bài Practice, dùng cột nhận xét của `DocumentEditor`. Màn hình hẹp dùng vị trí nội tuyến có sẵn. Thẻ theo từng lượt đã nộp, hiển thị lịch sử và cho phép yêu cầu góp ý bằng thao tác trong thẻ. Hợp đồng GET/POST, điều kiện truy cập và nghiệp vụ U06/U08 tiếp tục được dùng như hiện tại.

- [x] Bước 1 — Bổ sung kiểm thử `frontend/tests/features/assignmentwork/pages/SubmissionHistoryPage.test.tsx`. Xác nhận bố cục rộng/hẹp, cách ly lượt nộp, chỉ gửi khi bấm và phạm vi Practice. Hai ca mới thất bại với popup cũ vì góp ý chưa hiện khi mở trang.
- [x] Bước 2 — Đưa `PracticeFeedbackPanel.tsx` vào cột nhận xét của `SubmissionHistoryPage.tsx`, gắn vòng đời theo mã lượt. Thẻ hiển thị trạng thái tải, chờ, lỗi và thao tác thử lại. Sáu ca trong bộ kiểm thử trang đã qua.
- [x] Bước 3 — Đồng bộ UC 41, FR-030, US-ASM-012 và tài liệu nghiệp vụ, giao diện và mã U08 có mô tả luồng góp ý. Giữ tài liệu lịch sử làm tham chiếu.
- [x] Bước 4 — Chạy kiểm thử frontend trong phạm vi liên quan, TypeScript trong production build, lint và build. Kiểm tra diff và ghi kết quả trong trạng thái AI-DLC.

## Kết quả kiểm tra

- `npm test -- tests/features/assignmentwork tests/shared/document --maxWorkers=2` đạt 71/71 ca trong 6 bộ, gồm thẻ trên màn hình rộng và hẹp, cách ly lượt nộp, gửi theo thao tác, trạng thái chờ và tải lại lịch sử khi lỗi.
- `npm run lint`, `npm run build` và `git diff --check` đều hoàn tất thành công. Build đã kiểm tra TypeScript và báo cảnh báo DNS khi tải Google Fonts.
- Lượt chạy toàn bộ Vitest gặp lỗi `ENOENT` ở thư mục tạm của sandbox và được dừng. Lượt chạy 71 ca đặt `TEMP` và `TMP` trong thư mục cache của workspace. Kiểm thử dùng API giả lập, chưa kiểm tra trên trình duyệt hoặc gọi nhà cung cấp AI thật.
