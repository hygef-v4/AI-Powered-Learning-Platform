-- U12: khóa ngoại tài liệu nhóm -> bài tập, để sau khi bảng assignments (U07) đã có trên staging.
-- NOT VALID: chỉ kiểm dòng mới, không chặn migrate nếu môi trường cũ còn tài liệu thử trỏ tới bài đã xóa.
alter table group_documents
    add constraint fk_group_documents_assignment foreign key (assignment_id) references assignments (id) not valid;
