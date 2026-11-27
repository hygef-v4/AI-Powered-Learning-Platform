#!/bin/sh
# Chạy một lần khi khởi tạo volume. `migrator` sở hữu schema (chỉ Flyway dùng), `app` chỉ đọc và ghi dữ liệu.
# Quyền riêng của `audit_logs` (app không UPDATE, DELETE) do migration U02 đặt.
# `backup_replicator` chỉ có quyền REPLICATION, dùng cho pg_basebackup của worker (U03, 2026-11-26).
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
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

CREATE ROLE backup_replicator LOGIN REPLICATION PASSWORD '${BACKUP_DB_PASSWORD:-}';
SQL
