-- Widen quiz_attempts score columns to numeric(10, 2) so 200-question quizzes with 9999.99 points/question never overflow.
alter table quiz_attempts
    alter column score type numeric(10, 2),
    alter column max_score type numeric(10, 2),
    alter column raw_score type numeric(10, 2),
    alter column suggested_penalty type numeric(10, 2),
    alter column penalty_points type numeric(10, 2);
