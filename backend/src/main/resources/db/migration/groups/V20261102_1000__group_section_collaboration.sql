-- Góp ý 2026-11-02 cho bài nhóm: trưởng nhóm chốt mục (BR-U12-39), lịch sử ghi cả lần tiếp quản và nhập file
-- (BR-U12-35, 46), đánh giá chéo giữa thành viên (BR-U12-47).

alter table group_document_sections
    add column locked_by uuid references accounts (id),
    add column locked_at timestamptz,
    add constraint ck_group_document_sections_lock check ((locked_by is null) = (locked_at is null)),
    -- Chỉ chốt mục đã Xong; mục đang có người giữ không chốt được.
    add constraint ck_group_document_sections_lock_done check (locked_by is null or status = 'DONE');

alter table group_document_revisions
    add column kind varchar(20) not null default 'DONE' check (kind in ('DONE', 'TAKEOVER', 'IMPORT'));

create table group_peer_reviews (
    id                uuid          primary key,
    group_document_id uuid          not null references group_documents (id) on delete cascade,
    reviewer_id       uuid          not null references accounts (id),
    reviewee_id       uuid          not null references accounts (id),
    score             smallint      not null check (score between 1 and 5),
    comment           varchar(1000),
    created_at        timestamptz   not null,
    updated_at        timestamptz   not null,
    constraint uq_group_peer_reviews unique (group_document_id, reviewer_id, reviewee_id),
    constraint ck_group_peer_reviews_not_self check (reviewer_id <> reviewee_id)
);
create index ix_group_peer_reviews_document on group_peer_reviews (group_document_id);

do $$
begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        revoke all on group_peer_reviews from app;
        grant select, insert, update on group_peer_reviews to app;
    end if;
end
$$;
