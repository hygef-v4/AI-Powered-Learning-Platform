-- U03: các tệp WAL đã tải lên Google Drive (2026-11-26). Dọn khi cũ hơn base backup cũ nhất còn giữ.
create table wal_segments (
    file_name   varchar(64)  primary key,
    archive_ref varchar(200) not null,
    byte_size   bigint       not null,
    uploaded_at timestamptz  not null,
    purged_at   timestamptz
);

create index wal_segments_uploaded on wal_segments (uploaded_at) where purged_at is null;
