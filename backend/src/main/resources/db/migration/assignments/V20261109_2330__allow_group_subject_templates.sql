-- Đề mẫu của môn có thể là bài nhóm (yêu cầu 2026-11-09): bài của môn chỉ để giảng viên copy về lớp, không giao trực tiếp.
alter table assignments drop constraint ck_assignments_scope_owner;
alter table assignments add constraint ck_assignments_scope_owner check ((scope = 'CLASS' and class_id is not null)
    or (scope = 'SUBJECT' and class_id is null));
