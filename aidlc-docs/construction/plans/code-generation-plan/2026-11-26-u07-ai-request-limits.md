# U07 — Giới hạn yêu cầu và lỗi AI

1. [x] Đối chiếu ô nhập, giới hạn backend và ngân sách theo tác vụ.
2. [x] Đồng bộ giới hạn 10.000 ký tự trên các màn tạo và sửa đề và ở lớp kiểm tra prompt. Giữ văn bản khi vượt và chặn gửi kèm cảnh báo.
3. [x] Dành tối đa 12.000 token đầu ra cho yêu cầu tạo đề dài hơn 4.000 ký tự. Giữ trần provider và công thức chấm rubric hiện tại.
4. [x] Hiển thị mã ổn định cho lỗi độ dài, kết nối, timeout, xác thực, quota và kết quả AI không dùng được.
5. [x] Kiểm tra 238 ca backend và 409 ca frontend gồm các ca chấm điểm. Chạy lại 61 ca giao diện soạn đề sau chỉnh câu cảnh báo. TypeScript, lint phạm vi và diff đạt. Chrome với API giả lập giữ nguyên 10.001 ký tự và chặn gửi, rồi gửi đủ 10.000 sau khi rút ngắn.

Yêu cầu tạo đề dài giữ nhiều token hơn trong API trường. Phần giữ chỗ dùng quota chung với chấm AI. Giới hạn tạo câu hỏi trắc nghiệm và điều chỉnh rubric tiếp tục dùng chính sách hiện tại.
