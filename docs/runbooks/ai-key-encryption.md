# Khóa mã hóa API key AI

Runbook của U06 (P4, NFR-U06-30, SECURITY-09). Khóa API của người dùng và của trường được lưu dạng mã hóa AES-256-GCM bằng một khóa chỉ có trong biến môi trường. Thiếu khóa thì backend vẫn chạy, nhưng mọi thao tác lưu, kiểm tra hay dùng khóa API trả lỗi `AI_KEY_ENCRYPTION_NOT_CONFIGURED` (503); không có giá trị mặc định.

## Biến môi trường

| Biến | Bắt buộc | Ý nghĩa |
|---|---|---|
| `U06_AI_KEY_ENCRYPTION_KEY` | Có (môi trường thật) | Khóa phiên bản 1, base64 của đúng 32 byte |
| `U06_AI_KEY_ENCRYPTION_KEY_V2` | Chỉ khi đổi khóa | Khóa phiên bản 2; có thì mọi khóa API lưu từ đây dùng phiên bản này |
| `U06_AI_REQUEST_TIMEOUT` | Không (mặc định `120s`) | Thời gian chờ một yêu cầu tới nhà cung cấp |
| `U06_AI_TEST_TIMEOUT` | Không (mặc định `20s`) | Thời gian chờ nút Kiểm tra kết nối |
| `U06_GEMINI_MODELS`, `U06_OPENAI_MODELS`, `U06_CLAUDE_MODELS` | Không | Danh sách mô hình cho phép, cách nhau bởi dấu phẩy |

## Tạo khóa

Chạy trên máy người vận hành, rồi đưa giá trị vào `.env` của VPS (`infra/.env.example` có sẵn tên biến):

```bash
openssl rand -base64 32
```

Không commit khóa, không gửi qua chat hay email. Mất khóa thì mọi khóa API đã lưu không giải mã được nữa: người dùng và Quản trị viên phải nhập lại khóa.

## Chạy ở máy dev

Đặt biến trước khi chạy backend, ví dụ trong Git Bash:

```bash
export U06_AI_KEY_ENCRYPTION_KEY="$(openssl rand -base64 32)"
```

Khóa tạo mới mỗi lần mở terminal làm các khóa API đã lưu trong database dev không dùng được; muốn giữ thì lưu giá trị vào tệp env riêng của máy (không commit).

## Đổi khóa

Đổi dần, không giải mã lại hàng loạt:

1. Tạo khóa mới, đặt vào `U06_AI_KEY_ENCRYPTION_KEY_V2`, giữ nguyên `U06_AI_KEY_ENCRYPTION_KEY`; khởi động lại backend và worker.
2. Từ đây khóa API lưu mới hoặc thay khóa được mã hóa bằng phiên bản 2; khóa cũ vẫn giải mã bằng phiên bản 1.
3. Nhắc người dùng và Quản trị viên thay khóa (nút Thay khóa). Theo dõi số khóa còn ở phiên bản 1:

   ```sql
   select key_version, count(*) from ai_connections where status <> 'REVOKED' group by key_version;
   ```

4. Khi không còn khóa nào ở phiên bản 1 mới được bỏ khóa cũ. Kết nối phiên bản 1 còn sót lại sẽ báo lỗi khi dùng và phải nhập lại khóa.

Khóa mã hóa đã lộ thì làm ngay bước 1, yêu cầu mọi người thay khóa API ở nhà cung cấp (thu hồi khóa cũ ở trang của nhà cung cấp), rồi bỏ khóa cũ.
