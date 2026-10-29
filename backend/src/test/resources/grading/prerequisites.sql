-- Isolated U09 test fixture only. These are NOT production provider migrations.
CREATE ROLE app NOLOGIN;
CREATE TABLE accounts (id UUID PRIMARY KEY);
CREATE TABLE course_classes (id UUID PRIMARY KEY);
CREATE TABLE assignments (id UUID PRIMARY KEY);
CREATE TABLE assignment_attempts (id UUID PRIMARY KEY);
