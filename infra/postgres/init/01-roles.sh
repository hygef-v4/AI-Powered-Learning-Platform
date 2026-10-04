#!/bin/sh
# Chạy một lần khi khởi tạo volume. `migrator` sở hữu schema (Flyway), `app` chỉ đọc/ghi dữ liệu.
# Quyền riêng của bảng `audit_logs` (app không UPDATE/DELETE) do migration U02 đặt.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE ROLE migrator LOGIN PASSWORD '${POSTGRES_MIGRATOR_PASSWORD}';
CREATE ROLE app LOGIN PASSWORD '${POSTGRES_APP_PASSWORD}';

REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO migrator;
GRANT USAGE ON SCHEMA public TO app;
GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO migrator, app;

ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app;
ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO app;
SQL
