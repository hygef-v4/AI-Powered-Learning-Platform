-- API chung của trường: nhiều khóa cho mỗi provider (thay BR-U06-02 phần API chung). Khi gọi, khóa đang bật tạo sớm nhất
-- của provider được dùng.
drop index if exists uq_ai_connections_institutional;
create index ix_ai_connections_institutional on ai_connections (provider, created_at)
    where scope = 'INSTITUTIONAL' and status <> 'REVOKED';
