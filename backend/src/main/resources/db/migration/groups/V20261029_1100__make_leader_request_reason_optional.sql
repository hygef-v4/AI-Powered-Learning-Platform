-- U12: lý do yêu cầu đổi trưởng nhóm không còn bắt buộc (yêu cầu 2026-10-29): bỏ ràng buộc 10–1.000 ký tự, giữ tối đa
-- 1.000 ký tự của cột; không nhập lý do thì lưu chuỗi rỗng.
alter table leader_change_requests drop constraint if exists leader_change_requests_reason_check;
