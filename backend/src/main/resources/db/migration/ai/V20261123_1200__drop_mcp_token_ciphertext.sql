-- Bỏ tính năng xem lại token MCP: server không giữ bản có thể giải mã của token nữa. Token đang dùng vẫn hợp lệ vì
-- xác thực chỉ dựa vào token_hash; người dùng mất token thì tạo agent mới. Xóa luôn các bản mã đã lưu từ trước.
alter table mcp_credentials
    drop column token_ciphertext,
    drop column token_iv,
    drop column token_key_version;
