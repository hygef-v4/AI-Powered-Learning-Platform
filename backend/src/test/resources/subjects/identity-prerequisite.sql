-- Chỉ dùng cho test PostgreSQL U04 trước khi nhận migration accounts thật của U01.
-- Không nằm trong db/migration và không được chạy ở application runtime.
create table accounts (id uuid primary key);
