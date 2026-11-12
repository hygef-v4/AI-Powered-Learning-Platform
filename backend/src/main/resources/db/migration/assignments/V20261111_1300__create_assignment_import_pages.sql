-- Nhập đề từ ảnh/PDF (BR-U07-91): ảnh trang đã chuẩn hóa giữ tạm tới khi AI đọc xong, xóa khi bản nháp rời PENDING;
-- job dọn xóa trang quá 24 giờ. Không lưu tệp gốc.
create table assignment_import_pages (
    draft_id   uuid        not null references assignment_ai_drafts (id) on delete cascade,
    page_no    smallint    not null,
    media_type varchar(20) not null,
    width      integer     not null,
    height     integer     not null,
    bytes      bytea       not null,
    text_hint  text,
    created_at timestamptz not null,
    constraint pk_assignment_import_pages primary key (draft_id, page_no),
    constraint ck_assignment_import_pages_page check (page_no between 1 and 10),
    constraint ck_assignment_import_pages_media check (media_type in ('image/jpeg', 'image/png'))
);

create index ix_assignment_import_pages_created_at on assignment_import_pages (created_at);

do $$
begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        revoke all on assignment_import_pages from app;
        grant select, insert, delete on assignment_import_pages to app;
    end if;
end
$$;
