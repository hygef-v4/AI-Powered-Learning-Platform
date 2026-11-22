CREATE TABLE unattached_files (
    file_id TEXT PRIMARY KEY,
    cleanup_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_unattached_files_cleanup ON unattached_files (cleanup_at, file_id);
