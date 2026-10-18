-- U01: khóa ngoại người thực hiện của audit (domain-entities U02 mục audit_logs); actor_id NULL là hệ thống hoặc worker.
alter table audit_logs
    add constraint audit_logs_actor_id_fkey foreign key (actor_id) references accounts (id);
