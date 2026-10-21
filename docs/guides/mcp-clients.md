# Kết nối agent MCP

Hướng dẫn cho Student, Teacher, Subject Manager (UC 32). Agent MCP là ứng dụng AI của riêng bạn (chạy trên máy bạn, dùng mô hình và tài khoản của bạn) gọi công cụ của nền tảng theo đúng quyền tài khoản của bạn. Nền tảng không gọi AI thay agent.

> **Chưa kiểm chứng.** Nhóm chưa thử với ứng dụng agent cụ thể nào. Các đoạn cấu hình dưới đây theo đặc tả MCP (transport Streamable HTTP, bản 2025-06-18); tên trường cấu hình có thể khác giữa các ứng dụng. Khi đã thử được với một ứng dụng, cập nhật mục "Ứng dụng đã kiểm chứng".

## Thông số máy chủ

| Mục | Giá trị |
|---|---|
| Địa chỉ | `https://<tên miền của nền tảng>/mcp` (dev: `http://localhost:3000/mcp`) |
| Transport | Streamable HTTP, mỗi yêu cầu một `POST` JSON-RPC 2.0, phản hồi JSON; không mở luồng SSE (`GET` trả 405) |
| Xác thực | Header `Authorization: Bearer <token>`; không dùng cookie đăng nhập |
| Phiên bản giao thức | `2025-06-18` (nhận cả `2025-03-26`) |
| Phương thức | `initialize`, `ping`, `tools/list`, `tools/call` |
| Giới hạn | 60 lời gọi tool mỗi phút mỗi token; tham số tối đa 64 KB; kết quả quá 256 KB bị cắt |

Yêu cầu có header `Origin` (gửi từ trang web trong trình duyệt) bị từ chối để chặn tấn công DNS rebinding; agent chạy trên máy không gửi header này.

## Tạo token

1. Bấm **Kết nối AI** trên thanh trên cùng, phần **Agent MCP**, bấm **Kết nối agent mới**.
2. Đặt nhãn (ví dụ tên ứng dụng), chọn quyền:
   - **Chỉ đọc**: xem lớp, bài, học liệu, kết quả trong quyền của bạn.
   - **Đọc và ghi**: thêm các thao tác được làm như tạo bản nháp, đề xuất chấm. Không có công cụ nộp bài chính thức hay công bố điểm.
3. Chọn hạn dùng 7, 30 hoặc 90 ngày, bấm **Tạo token**.
4. Sao chép token ngay: token chỉ hiện một lần. Mỗi tài khoản có tối đa 5 token còn hiệu lực.

Ai có token có thể dùng công cụ của nền tảng bằng tài khoản của bạn. Không dán token vào tệp đưa lên Git. Lộ token thì bấm **Thu hồi**: lời gọi tiếp theo bị từ chối ngay. Đổi mật khẩu không thu hồi token; tài khoản bị khóa thì mọi token ngừng dùng được.

## Cấu hình mẫu

Đa số ứng dụng hỗ trợ MCP qua HTTP nhận một khối `mcpServers` dạng:

```json
{
  "mcpServers": {
    "nen-tang-hoc-tap": {
      "type": "http",
      "url": "https://<tên miền của nền tảng>/mcp",
      "headers": { "Authorization": "Bearer <token của bạn>" }
    }
  }
}
```

Popup token trên nền tảng có nút **Sao chép cấu hình** điền sẵn địa chỉ và token. Sau khi dán, khởi động lại agent và kiểm tra danh sách công cụ hiện ra.

Kiểm tra nhanh bằng dòng lệnh (thay token):

```bash
curl -s -X POST "https://<tên miền của nền tảng>/mcp" -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

## Token và số liệu sử dụng

Nền tảng không biết agent dùng bao nhiêu token của mô hình. Lịch sử sử dụng AI ghi mỗi lượt gọi tool với số token "Không có số liệu", trừ khi agent tự gửi `_meta.inputTokens` và `_meta.outputTokens` trong `params` của `tools/call` (ghi là "Agent tự báo").

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân |
|---|---|
| HTTP 401 | Token sai, đã thu hồi, hết hạn, hoặc tài khoản không còn hoạt động |
| HTTP 403 | Yêu cầu có header `Origin` (gọi từ trình duyệt) |
| Lỗi JSON-RPC `-32029` | Vượt 60 lời gọi mỗi phút; chờ rồi thử lại |
| Lỗi `-32602` "Không có tool này" | Tool không có, không dành cho role của bạn, hoặc là tool ghi trong khi token chỉ đọc |
| Kết quả `isError: true` "Không tìm thấy hoặc không có quyền" | Dữ liệu ngoài phạm vi lớp hoặc môn của bạn |

## Ứng dụng đã kiểm chứng

Chưa có.
