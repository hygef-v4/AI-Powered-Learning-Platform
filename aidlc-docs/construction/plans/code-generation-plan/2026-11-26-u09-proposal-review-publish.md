# U09 — Xác nhận đề xuất và thông báo khi công bố điểm

1. [x] Tái hiện việc popup mất kết quả khi nhận lỗi từ API và tải lại bản chấm.
2. [x] Ghi xác nhận cho các tiêu chí cần xem lại khi giảng viên giữ hoặc áp dụng đề xuất. Mở link MCP và tự điền từ AI web tiếp tục là thao tác xem trước. Thay đổi điểm sau xác nhận sẽ đưa tiêu chí trở lại trạng thái cần kiểm tra theo quy tắc hiện hành.
3. [x] Hiện hướng dẫn trong màn hình chấm trước khi mở popup nếu có tiêu chí chưa xác nhận. Giữ kết quả từ API trong popup khi tải lại bản chấm.
4. [x] Kiểm tra 233 ca frontend của U09, TypeScript, ESLint phạm vi và diff. Kiểm thử luồng công bố dùng API giả lập.

Thay đổi dùng hợp đồng lưu và công bố hiện có. Backend tiếp tục kiểm quyền, phiên bản và dữ liệu xác nhận. Các quy tắc triển khai, health check và gọi dịch vụ không phát sinh thêm trong phạm vi này.
