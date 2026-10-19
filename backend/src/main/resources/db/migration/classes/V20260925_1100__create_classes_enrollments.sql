-- U05: lớp và ghi danh (domain-entities U05). Chạy sau accounts (0930) và subjects (1000) vì có FK sang hai bảng đó.
CREATE TABLE course_classes (
    id UUID PRIMARY KEY,
    subject_id UUID NOT NULL REFERENCES subjects(id),
    code VARCHAR(30) NOT NULL CHECK (length(trim(code)) > 0),
    name VARCHAR(200) NOT NULL CHECK (length(trim(name)) > 0),
    description VARCHAR(2000),
    term VARCHAR(20),
    status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'OPEN', 'ARCHIVED')),
    teacher_id UUID REFERENCES accounts(id),
    show_grade_distribution BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at >= created_at),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_course_classes_subject_code UNIQUE (subject_id, code),
    CONSTRAINT ck_open_class_teacher CHECK (status <> 'OPEN' OR teacher_id IS NOT NULL)
);
CREATE INDEX ix_course_classes_subject_status ON course_classes(subject_id, status);
CREATE INDEX ix_course_classes_teacher ON course_classes(teacher_id);

CREATE TABLE enrollments (
    class_id UUID NOT NULL REFERENCES course_classes(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'REMOVED')),
    source VARCHAR(10) NOT NULL CHECK (source IN ('MANUAL', 'LIST')),
    enrolled_at TIMESTAMPTZ NOT NULL,
    removed_at TIMESTAMPTZ,
    PRIMARY KEY (class_id, account_id),
    CONSTRAINT ck_enrollment_removed_at CHECK (
        (status = 'ACTIVE' AND removed_at IS NULL) OR
        (status = 'REMOVED' AND removed_at IS NOT NULL AND removed_at >= enrolled_at)
    )
);
CREATE INDEX ix_enrollments_account_status ON enrollments(account_id, status);

REVOKE ALL ON course_classes, enrollments FROM app;
GRANT SELECT, INSERT, UPDATE ON course_classes, enrollments TO app;
