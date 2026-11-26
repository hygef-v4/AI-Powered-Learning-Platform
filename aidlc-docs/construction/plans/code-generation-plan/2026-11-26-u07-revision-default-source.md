# U07 — Nguồn AI của chat sửa đề

Tiếp tục sau yêu cầu đặt nút đổi nguồn cạnh nút đính kèm và xác nhận “làm tiếp”. Phạm vi là chat Sửa đề bằng AI. Chat lưu bản nháp, chọn nguồn, gửi đề xuất rồi áp dụng kết quả qua API hiện có.

1. [x] Đối chiếu luồng chọn nguồn từ commit f8b4523e và bước xác thực đề sửa.
2. [x] Kiểm thử Gửi bằng nguồn mặc định, đổi nguồn cho lần gửi tiếp theo và trường hợp phải chọn thủ công. Kiểm thử hồi quy tái hiện popup mở trước khi sửa.
3. [x] Dùng nguồn mặc định khả dụng khi Gửi. Đặt nút Đổi nguồn AI cạnh nút đính kèm. Khi chưa có nguồn mặc định khả dụng, hiện bộ chọn trước khi gửi. Tô nguồn đã chọn cho lượt sắp gửi, nguồn mặc định hoặc nguồn vừa dùng nếu chưa đặt mặc định. Ẩn hai ghi chú của popup trong chat. Hỗ trợ vi/en.
4. [x] Chạy 104 kiểm thử U07 và bộ chọn nguồn, lint theo phạm vi và TypeScript. Chạy 11 kiểm thử AssignmentAiRevisionTest, gồm từ chối đề xuất trống. Chrome dùng API giả lập xác nhận hai lượt gửi đúng nguồn, tô đúng lựa chọn, ẩn ghi chú và nút đính kèm vẫn bấm được ở 390 px.

Backend tiếp tục kiểm tra quyền truy cập và hạn mức của nguồn. Chat gửi nguồn cụ thể và giữ mã yêu cầu khi lần gửi chưa được xác nhận.
