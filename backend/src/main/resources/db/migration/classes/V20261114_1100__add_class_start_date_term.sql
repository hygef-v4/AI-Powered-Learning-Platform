-- D-U04-05: lớp có ngày bắt đầu; học kỳ suy ra từ ngày bắt đầu và dùng để lọc lớp.
-- Quy tắc: tháng 1–4 SP, 5–8 SU, 9–12 FA + 2 số cuối của năm; tháng 4, 8, 12 thêm "-BL5".
-- Cùng quy tắc với ClassTermPolicy ở backend, hai nơi phải đổi cùng nhau.
-- Lớp có sẵn lấy ngày tạo theo giờ Việt Nam làm ngày bắt đầu; không đổi version/updated_at.
ALTER TABLE course_classes ADD COLUMN starts_on DATE;

UPDATE course_classes
SET starts_on = (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;

ALTER TABLE course_classes ALTER COLUMN starts_on SET NOT NULL;
ALTER TABLE course_classes ADD CONSTRAINT ck_course_classes_starts_on
    CHECK (starts_on BETWEEN DATE '2000-01-01' AND DATE '2099-12-31');

ALTER TABLE course_classes ADD COLUMN term VARCHAR(8) GENERATED ALWAYS AS (
    CASE
        WHEN extract(month FROM starts_on) <= 4 THEN 'SP'
        WHEN extract(month FROM starts_on) <= 8 THEN 'SU'
        ELSE 'FA'
    END
    || lpad((extract(year FROM starts_on)::integer % 100)::text, 2, '0')
    || CASE WHEN extract(month FROM starts_on)::integer % 4 = 0 THEN '-BL5' ELSE '' END
) STORED;
ALTER TABLE course_classes ADD CONSTRAINT ck_course_classes_term
    CHECK (term ~ '^(SP|SU|FA)[0-9]{2}(-BL5)?$');

-- Mã lớp được dùng lại giữa các học kỳ của cùng môn.
ALTER TABLE course_classes DROP CONSTRAINT uq_course_classes_subject_code;
ALTER TABLE course_classes ADD CONSTRAINT uq_course_classes_subject_term_code
    UNIQUE (subject_id, term, code);
CREATE INDEX ix_course_classes_subject_term ON course_classes(subject_id, term);
