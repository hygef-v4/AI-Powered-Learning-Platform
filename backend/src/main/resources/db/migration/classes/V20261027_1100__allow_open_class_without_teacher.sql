-- Lớp mở không còn bắt buộc có giảng viên: lớp tạo ra được mở ngay, phân công giảng viên sau.
ALTER TABLE course_classes DROP CONSTRAINT IF EXISTS ck_open_class_teacher;
