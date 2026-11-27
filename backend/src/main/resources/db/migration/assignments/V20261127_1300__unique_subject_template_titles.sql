-- BR-U07-102 (2026-11-27): đề mẫu của môn chưa ngưng giao không trùng tên trong môn. So tên sau khi gộp khoảng trắng,
-- bỏ khoảng trắng đầu cuối, không phân biệt hoa thường; cùng cách so với AssignmentSupport.comparableTitle.
-- Bản NEW_VERSION không vào index vì giữ chung tên với version trước; service vẫn kiểm chúng với đề mẫu khác.

-- Dữ liệu cũ đã trùng: giữ tên của bài tạo sớm nhất, các bài sau thêm " (2)", " (3)"… (cắt tên gốc để không quá 200
-- ký tự), tăng version để màn hình đang mở bài đó phải tải lại trước khi lưu.
do $$
declare
    duplicate record;
    candidate varchar(200);
    suffix text;
    n integer;
begin
    for duplicate in
        select id, title
        from (select id, title,
                     row_number() over (partition by subject_id, lower(btrim(regexp_replace(title, '\s+', ' ', 'g')))
                                        order by created_at, id) as position
              from assignments
              where scope = 'SUBJECT' and status <> 'RETIRED' and origin <> 'NEW_VERSION') ranked
        where position > 1
        order by id
    loop
        n := 2;
        loop
            suffix := ' (' || n || ')';
            candidate := btrim(left(btrim(duplicate.title), 200 - length(suffix))) || suffix;
            exit when not exists (
                select 1 from assignments other
                where other.subject_id = (select subject_id from assignments where id = duplicate.id)
                  and other.scope = 'SUBJECT' and other.status <> 'RETIRED'
                  and lower(btrim(regexp_replace(other.title, '\s+', ' ', 'g')))
                      = lower(btrim(regexp_replace(candidate, '\s+', ' ', 'g'))));
            n := n + 1;
        end loop;
        update assignments set title = candidate, version = version + 1, updated_at = now() where id = duplicate.id;
    end loop;
end $$;

create unique index uq_assignments_subject_title
    on assignments (subject_id, lower(btrim(regexp_replace(title, '\s+', ' ', 'g'))))
    where scope = 'SUBJECT' and status <> 'RETIRED' and origin <> 'NEW_VERSION';
