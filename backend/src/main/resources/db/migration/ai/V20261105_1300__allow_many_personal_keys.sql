-- Khóa API cá nhân như khóa của trường (yêu cầu 2026-11-05): nhiều khóa cho mỗi nhà cung cấp.
drop index if exists uq_ai_connections_personal;
create index if not exists ix_ai_connections_personal on ai_connections (owner_id, provider, created_at)
    where scope = 'PERSONAL' and status <> 'REVOKED';
