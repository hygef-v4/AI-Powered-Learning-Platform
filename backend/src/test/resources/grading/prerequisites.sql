-- Isolated U09 test fixture only. These are NOT production provider migrations.
-- Role app có sẵn ở cluster dùng chung (SharedPostgres), không tạo lại.
CREATE TABLE accounts (id UUID PRIMARY KEY);
CREATE TABLE course_classes (id UUID PRIMARY KEY);
CREATE TABLE assignments (id UUID PRIMARY KEY);
CREATE TABLE assignment_attempts (id UUID PRIMARY KEY);
