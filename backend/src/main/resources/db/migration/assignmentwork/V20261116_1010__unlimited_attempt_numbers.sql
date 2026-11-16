-- Bài có chấm nộp lại không giới hạn từ 2026-11-10 (BR-U08-70) nên số thứ tự lượt không còn trần 10: trước đây
-- lượt thứ 11 của một người học bị ràng buộc này từ chối. Bài Luyện tập vẫn bị chặn ở maxAttempts 1–10 trong mã.
alter table assignment_attempts drop constraint ck_assignment_attempts_no;
alter table assignment_attempts add constraint ck_assignment_attempts_no check (attempt_no >= 1);
