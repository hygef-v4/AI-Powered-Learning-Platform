# Lấy khóa API AI

Hướng dẫn cho Student, Teacher, Subject Manager (API cá nhân, UC 31) và Quản trị viên (API của trường, UC 14). Các bước theo trang của nhà cung cấp tại thời điểm viết (10/2026) và có thể thay đổi; nền tảng không kiểm soát giá hay hạn mức của nhà cung cấp.

## Nguyên tắc chung

- Khóa API cá nhân chỉ dùng cho yêu cầu của chính bạn; chi phí tính vào tài khoản nhà cung cấp của bạn và không bị giới hạn bởi hạn mức của trường.
- Nền tảng chỉ lưu bản mã hóa, sau khi lưu chỉ hiện 4 ký tự cuối (ví dụ `••••3kQf`), không ai xem lại được khóa đầy đủ.
- Nút **Kiểm tra** gửi một yêu cầu rất nhỏ và có thể phát sinh chi phí nhỏ trên khóa.
- Thu hồi trên nền tảng chỉ xóa khóa khỏi nền tảng. Nếu nghi khóa bị lộ, hãy thu hồi luôn ở trang của nhà cung cấp.
- Danh sách mô hình lấy từ cấu hình backend và hiển thị trong ô **Mô hình** (API `/api/v1/ai/providers`). Chọn mô hình trong danh sách đang chạy; danh sách trong tài liệu cũ có thể không còn khớp môi trường. Không cần đổi khóa hoặc mô hình nếu kết nối hiện có vẫn hoạt động.
- Token **Agent MCP** và khóa API nhà cung cấp là hai loại khác nhau. Token MCP cho phép agent gọi công cụ nền tảng; nó không bật Gemini/OpenAI/Claude trong các nút AI của ứng dụng.
- MCP chỉ dành cho Teacher, Subject Manager và Admin. Student tiếp tục quản lý khóa cá nhân/Practice ở Kết nối AI; trang này không gọi API token MCP. Admin quản lý token ở **Agent MCP** trong menu quản trị, không cần khóa AI cá nhân.
- Tạo nháp bài tập, tạo câu hỏi quiz và đề xuất chấm có lựa chọn nguồn API cá nhân/API của trường. Nguồn của trường cần được quản trị viên cấu hình và bật. Phản hồi Practice dùng API cá nhân của student đang yêu cầu; khóa teacher không được dùng thay.

## Gemini (Google AI Studio)

1. Mở aistudio.google.com và đăng nhập tài khoản Google.
2. Chọn **Get API key** rồi **Create API key**, chọn hoặc tạo một dự án Google Cloud.
3. Sao chép khóa. Tài khoản miễn phí có giới hạn số yêu cầu; muốn dùng nhiều hơn thì bật thanh toán cho dự án.

## OpenAI

1. Mở platform.openai.com và đăng nhập.
2. Vào **API keys**, bấm **Create new secret key**, đặt tên dễ nhớ (ví dụ tên nền tảng).
3. Sao chép khóa ngay (chỉ hiện một lần). Tài khoản cần có tín dụng ở mục Billing thì khóa mới gọi được.

## Claude (Anthropic)

1. Mở console.anthropic.com và đăng nhập.
2. Vào **API keys**, bấm **Create Key**, chọn workspace.
3. Sao chép khóa ngay (chỉ hiện một lần). Nạp tín dụng ở mục Billing nếu tài khoản chưa có.

## Thêm khóa vào nền tảng

1. Bấm **Kết nối AI** trên thanh trên cùng (Quản trị viên: **Cài đặt › API AI của trường**).
2. Ở dòng nhà cung cấp, bấm **Thêm khóa**, dán khóa, chọn mô hình, bấm **Lưu khóa**.
3. Bấm **Kiểm tra** để chắc khóa dùng được. Kết quả "Đạt" kèm thời điểm hiện ở cột Kiểm tra gần nhất.
4. Đổi mô hình: bấm **Thay khóa**, để trống ô khóa và chọn mô hình khác.

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Kiểm tra báo khóa không hợp lệ | Sao chép lại khóa, không thừa khoảng trắng; xem khóa còn hiệu lực ở trang nhà cung cấp |
| Kiểm tra báo hết hạn mức hoặc hết tín dụng | Nạp tín dụng hoặc chờ hạn mức của nhà cung cấp đặt lại |
| "Hệ thống chưa bật mã hóa khóa API" | Báo Quản trị viên: thiếu biến `U06_AI_KEY_ENCRYPTION_KEY` (xem `docs/runbooks/ai-key-encryption.md`) |
| "Dữ liệu vừa được thay đổi ở nơi khác" | Tải lại trang rồi làm lại |
