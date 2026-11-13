-- Align the existing visibility table with the application role permissions.
grant select, insert, update on grade_distribution_visibility to app;
revoke delete on grade_distribution_visibility from app;
