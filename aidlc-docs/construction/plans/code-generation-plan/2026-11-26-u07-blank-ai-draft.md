# U07 — Sửa đề bằng AI khi bản nháp còn trống

Phạm vi đã được duyệt trong trao đổi về lỗi `AI_OUTPUT_INVALID` trên đề Cinema Ticket Booking System.

1. [x] Nhận biết bản nháp trống ở backend sau khi kiểm quyền. Đề đã có hướng dẫn, chữ, rubric hoặc khối bố cục và đa phương tiện tiếp tục dùng các thao tác sửa đề.
2. [x] Dùng prompt và schema tạo đề cho bản nháp trống. Theo ngôn ngữ yêu cầu khi giảng viên chưa cấu hình ngôn ngữ. Dùng giới hạn rubric của trình soạn đề cho trường hợp này.
3. [x] Chuyển điểm dạng chuỗi số thập phân thành số trước khi kiểm tra. Dùng phần tạo khung và rubric sẵn có để lưu đề xuất và kiểm tra phiên bản trước khi áp dụng.
4. [x] Kiểm tra đề với 4 câu, 26 tiêu chí và 10 điểm từ phản hồi giả lập. Kiểm tra trường hợp đề có nội dung và đề bị sửa trong khi chờ kết quả.

Yêu cầu tự do trong chat được giữ nguyên trong prompt. Việc kiểm tra số câu và tổng điểm theo giá trị kỳ vọng dùng các tùy chọn đã được xác nhận khi có. Một lượt gửi vẫn tạo một tác vụ AI.

Đạt 738 kiểm thử backend trong các package soạn đề, AI và chấm điểm, cùng 28 kiểm thử frontend. Một ca đánh giá AI chấm điểm được bỏ qua theo cấu hình. Phản hồi AI trong ca tạo đề được giả lập.
