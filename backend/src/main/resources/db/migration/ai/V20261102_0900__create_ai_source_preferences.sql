-- Nguồn AI mặc định của từng tài khoản (góp ý 2026-11-02): có dòng thì popup Chọn nguồn AI tự dùng nguồn này khi
-- nguồn còn dùng được; không có dòng là hỏi mỗi lần. API chung không gắn provider (Quản trị viên chọn khóa).
create table ai_source_preferences (
    account_id uuid        primary key references accounts (id),
    source     varchar(20) not null check (source in ('SHARED', 'PERSONAL')),
    provider   varchar(20) check (provider in ('GEMINI', 'OPENAI', 'CLAUDE')),
    updated_at timestamptz not null,
    constraint ck_ai_source_preferences_provider check ((source = 'PERSONAL') = (provider is not null))
);

-- Bỏ mặc định (về hỏi mỗi lần) là xóa dòng của chính mình.
do $$
begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        revoke all on ai_source_preferences from app;
        grant select, insert, update, delete on ai_source_preferences to app;
    end if;
end
$$;
