-- BR-U11-32, 33: chụp số liệu rời trang và mức trừ gợi ý vào lượt khi nộp, vì sự kiện gốc chỉ giữ 1 ngày.
-- score là điểm cuối; raw_score là điểm chấm từ câu trả lời. Hệ thống không tự trừ: suggested_penalty chỉ là gợi ý,
-- penalty_points là điểm giảng viên đã quyết định trừ. Lượt cũ để trống các cột mới.

alter table quiz_attempts add column raw_score numeric(8, 2);
alter table quiz_attempts add column suggested_penalty numeric(8, 2);
alter table quiz_attempts add column penalty_points numeric(8, 2);
alter table quiz_attempts add column leave_count integer;
alter table quiz_attempts add column activity_signal varchar(12);
alter table quiz_attempts add column penalty_decision varchar(10);
alter table quiz_attempts add column penalty_decided_by uuid;
alter table quiz_attempts add column penalty_decided_at timestamptz;
alter table quiz_attempts add column penalty_decision_reason varchar(1000);

alter table quiz_attempts add constraint ck_quiz_attempts_activity_signal
    check (activity_signal is null or activity_signal in ('DISABLED', 'OK', 'NO_SIGNAL', 'SIGNAL_LOST', 'UNAVAILABLE'));
alter table quiz_attempts add constraint ck_quiz_attempts_suggested_penalty check (suggested_penalty is null or suggested_penalty >= 0);
alter table quiz_attempts add constraint ck_quiz_attempts_penalty check (penalty_points is null or penalty_points >= 0);
alter table quiz_attempts add constraint ck_quiz_attempts_leave_count check (leave_count is null or leave_count >= 0);
alter table quiz_attempts add constraint ck_quiz_attempts_penalty_decision check (
    (penalty_decision is null and penalty_decided_at is null and penalty_decided_by is null and penalty_decision_reason is null)
    or (penalty_decision in ('APPLIED', 'DISMISSED') and penalty_decided_at is not null and penalty_decided_by is not null
        and length(btrim(penalty_decision_reason)) > 0));
alter table quiz_attempts add constraint ck_quiz_attempts_penalty_applied check (
    penalty_decision is distinct from 'APPLIED' or penalty_points > 0);
alter table quiz_attempts add constraint ck_quiz_attempts_penalty_dismissed check (
    penalty_decision is distinct from 'DISMISSED' or penalty_points = 0);
alter table quiz_attempts add constraint fk_quiz_attempts_penalty_decided_by
    foreign key (penalty_decided_by) references accounts (id);
