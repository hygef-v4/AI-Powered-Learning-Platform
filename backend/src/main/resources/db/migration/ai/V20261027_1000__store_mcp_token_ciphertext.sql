-- Token MCP được xem lại trong popup Sửa agent: lưu thêm bản mã hóa (AES-GCM, cùng khóa với khóa API), vẫn xác thực
-- bằng token_hash. Token tạo trước bản này không có bản mã nên không xem lại được.
alter table mcp_credentials
    add column token_ciphertext bytea,
    add column token_iv bytea,
    add column token_key_version integer;
