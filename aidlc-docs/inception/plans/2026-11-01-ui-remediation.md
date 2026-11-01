# Kế hoạch sửa UI theo audit 2026-11-01

Phạm vi: các lỗi đã quan sát trong `.tmp/ai-ui-review.md` và bốn ảnh người dùng gửi. Giữ giao diện, dữ liệu và quy tắc nghiệp vụ hiện có. Không tạo dữ liệu hoặc gửi tác vụ AI để kiểm tra.

- [x] Sửa dropdown chọn giảng viên nổi trước dialog, giữ khả năng chọn bằng chuột và bàn phím.
- [x] Sửa bảng audit: dịch các mã hành động hiện có và chặn nội dung dài đè sang cột khác.
- [x] Chuẩn hóa cursor cho control chung, nhãn Usage, trạng thái công tắc phân bố điểm và loading theo ngữ cảnh.
- [x] Sửa tràn ngang ở khung nhỏ và các popup AI có nút đóng trùng chức năng.
- [x] Làm thông báo YouTube có playlist rõ cách lấy link video riêng.
- [x] Sửa chữ mờ trên nền sáng của menu chọn mô hình API khi dùng dark mode bằng màu option theo theme.
- [x] Chạy kiểm tra frontend và xem lại UI ở desktop/mobile; ghi rõ giới hạn chưa kiểm tra.

Kiểm chứng: typecheck, lint, test liên quan, production build; quan sát lại các màn đã có bằng Orca nếu phiên local còn chạy.

Kết quả: 599/599 test; typecheck đạt; lint 0 lỗi, 2 warning import cũ; production build đạt. Orca xác nhận dropdown nằm trên dialog (z-index 110 > 100) và nhận hit test; ở 390 px, trang AI/API/MCP có document scrollWidth 390 px, bảng vẫn cuộn nội bộ. Popup Thêm khóa nằm trọn viewport 390 × 844 px, nên ảnh capture cũ bị cắt không phản ánh lỗi bố cục hiện tại. Chưa chạy lại toàn bộ hành trình của mọi vai trò hoặc các trạng thái AI cần gửi tác vụ.

Bổ sung dark mode: option và optgroup của select nhận màu nền `--c-surface`, chữ `--c-ink` trong cả hai theme. Orca đo màu option dark mode là nền `rgb(19, 35, 47)` và chữ `rgb(234, 244, 250)`. Popup native không nằm trong ảnh chụp Orca nên chưa có ảnh xác nhận trực tiếp. Test đầy đủ 599/599 đạt với `--maxWorkers=4`; cấu hình worker mặc định có một test môn học chờ tải bị timeout trong hai lần chạy, test đó qua khi chạy riêng.
