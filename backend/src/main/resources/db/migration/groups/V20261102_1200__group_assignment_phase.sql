-- Góp ý 2026-11-02 (FB-U12-01, thay cho chốt từng mục): tài liệu nhóm có giai đoạn phân công. Chưa chốt phân công thì
-- thành viên chỉ nhận hoặc được giao mục (người phụ trách), chưa soạn được; trưởng nhóm chốt phân công khi mọi mục
-- đã có người phụ trách, từ đó mỗi người soạn mục của mình; mở chốt để phân lại.

alter table group_documents
    add column assignment_locked_by uuid references accounts (id),
    add column assignment_locked_at timestamptz,
    add constraint ck_group_documents_assignment_lock
        check ((assignment_locked_by is null) = (assignment_locked_at is null));

alter table group_document_sections
    add column assignee_id uuid references accounts (id);

-- Dữ liệu cũ: người đang giữ hoặc người làm cuối là người phụ trách; tài liệu đã có người làm coi như đã chốt phân
-- công để nhóm làm tiếp. Bỏ chốt từng mục (cột locked_by/locked_at không còn dùng).
update group_document_sections set assignee_id = coalesce(claimed_by, last_author_id);
update group_document_sections set locked_by = null, locked_at = null where locked_by is not null;
update group_documents d set assignment_locked_by = g.leader_id, assignment_locked_at = now()
from class_groups g
where g.id = d.group_id and g.leader_id is not null
  and exists (select 1 from group_document_sections s where s.group_document_id = d.id and s.status <> 'OPEN');
