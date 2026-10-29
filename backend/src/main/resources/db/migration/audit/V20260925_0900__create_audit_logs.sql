-- U02: nhật ký audit chỉ thêm, giữ vĩnh viễn (BR-U02-01, 06). FK actor_id -> accounts do migration của U01 thêm.
create table audit_logs (
    id          uuid          primary key,
    actor_id    uuid,
    action      varchar(100)  not null,
    object_type varchar(50),
    object_id   varchar(200),
    result      varchar(10)   not null check (result in ('SUCCESS', 'DENIED', 'FAILURE')),
    reason      varchar(2000),
    details     jsonb         not null default '{}'::jsonb,
    occurred_at timestamptz   not null,
    recorded_at timestamptz   not null default now()
);

-- Tra cứu theo thời gian, người thực hiện, đối tượng, hành động (NFR-U02-03).
create index audit_logs_occurred_at_idx on audit_logs (occurred_at desc, id desc);
create index audit_logs_actor_idx on audit_logs (actor_id, occurred_at desc);
create index audit_logs_object_idx on audit_logs (object_type, object_id);
create index audit_logs_action_idx on audit_logs (action);

-- User ứng dụng chỉ SELECT, INSERT (NFR-U02-30). Môi trường không có role app (ví dụ Testcontainers) thì bỏ qua.
do $$
begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        revoke update, delete, truncate on audit_logs from app;
    end if;
end
$$;
