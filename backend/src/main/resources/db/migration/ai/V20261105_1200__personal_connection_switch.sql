-- Khóa API cá nhân cũng có công tắc bật / tắt như khóa API của trường (yêu cầu 2026-11-05).
alter table ai_connections drop constraint if exists ck_ai_connections_disabled;
