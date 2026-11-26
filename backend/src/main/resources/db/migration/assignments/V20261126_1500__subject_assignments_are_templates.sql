-- U07: đề mẫu của môn không có loại (2026-11-26). Đề mẫu lưu grading_mode = 'TEMPLATE' và luôn cá nhân; bài của lớp chỉ
-- GRADED hoặc PRACTICE. Bài của môn cũ đã từng giao cho sinh viên (trước 2026-11-09) giữ nguyên loại để không làm hỏng
-- lượt làm và điểm đã có; chỉ đề mẫu chưa giao (nháp) được đổi sang TEMPLATE.
alter table assignments drop constraint ck_assignments_grading_mode;

update assignments set grading_mode = 'TEMPLATE', submission_mode = 'INDIVIDUAL'
where scope = 'SUBJECT' and status in ('DRAFT', 'REVIEWED');

alter table assignments add constraint ck_assignments_grading_mode
    check (grading_mode in ('GRADED', 'PRACTICE', 'TEMPLATE'));
alter table assignments add constraint ck_assignments_template_scope
    check (grading_mode <> 'TEMPLATE' or scope = 'SUBJECT');
