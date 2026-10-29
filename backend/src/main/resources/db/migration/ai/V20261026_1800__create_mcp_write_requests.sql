-- Retry results and their domain mutation commit atomically. No credential or generated content is stored here.
create table mcp_write_requests (
    account_id uuid not null references accounts(id),
    operation varchar(100) not null,
    request_ref varchar(200) not null,
    fingerprint text not null,
    response jsonb not null,
    created_at timestamptz not null default now(),
    primary key(account_id, operation, request_ref)
);
do $$ begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        grant select, insert on mcp_write_requests to app;
    end if;
end $$;
