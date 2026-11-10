-- Bài có chấm (cá nhân, nhóm) nộp bao nhiêu lần cũng được khi còn hạn (yêu cầu 2026-11-10): chỉ bài Luyện tập cần số lượt
-- khi đã lên lịch; số lượt lưu cũ của bài có chấm giữ nguyên nhưng không còn dùng.
alter table assignments drop constraint ck_assignments_published;
alter table assignments add constraint ck_assignments_published check (status in ('DRAFT', 'REVIEWED')
    or (opens_at is not null and closes_at is not null and (grading_mode = 'GRADED' or max_attempts is not null)));
